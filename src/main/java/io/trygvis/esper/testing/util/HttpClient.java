package io.trygvis.esper.testing.util;

import fj.*;
import fj.data.*;
import io.trygvis.esper.testing.*;
import org.apache.commons.io.*;
import org.apache.http.conn.scheme.*;
import org.apache.http.impl.client.*;
import org.apache.http.impl.conn.tsccm.*;
import org.apache.http.params.*;
import org.codehaus.httpcache4j.*;
import org.codehaus.httpcache4j.cache.*;
import org.codehaus.httpcache4j.payload.*;
import org.codehaus.httpcache4j.resolver.*;
import org.slf4j.*;

import java.io.*;
import java.net.*;

import static fj.data.Option.*;
import static java.lang.System.*;

public class HttpClient<A> {

    private static final Logger logger = LoggerFactory.getLogger(HttpClient.class);

    private final HTTPCache http;
    private final F<HTTPResponse, Option<A>> f;

    public HttpClient(HTTPCache http, F<HTTPResponse, Option<A>> f) {
        this.http = http;
        this.f = f;
    }

    public static <A> F<HTTPResponse, Option<A>> inputStreamOnly(final F<InputStream, Option<A>> f) {
        return new F<HTTPResponse, Option<A>>() {
            public Option<A> f(HTTPResponse response) {
                InputStream inputStream = response.getPayload().getInputStream();
                try {
                    return f.f(inputStream);
                } finally {
                    IOUtils.closeQuietly(inputStream);
                }
            }
        };
    }

    public Option<P2<A, byte[]>> fetch(URI uri) {
        HTTPResponse response = null;

        try {
            response = http.execute(new HTTPRequest(uri));
            int code = response.getStatus().getCode();

            if (code != 200) {
                logger.warn("Did not get 200 back, got " + code);
                return none();
            }

            final ByteArrayOutputStream buffer = new ByteArrayOutputStream(1024 * 1024);

            IOUtils.copy(response.getPayload().getInputStream(), buffer);

            final byte[] bytes = buffer.toByteArray();

            response = new HTTPResponse(new ByteArrayPayload(new ByteArrayInputStream(bytes), response.getPayload().getMimeType()), response.getStatusLine(), response.getHeaders());

            return f.f(response).map(new F<A, P2<A, byte[]>>() {
                public P2<A, byte[]> f(A a) {
                    return P.p(a, bytes);
                }
            });
        } catch (HTTPException e) {
            logger.warn("Error while fetching: " + uri, e);
            return none();
        } catch (IOException e) {
            logger.warn("Error while fetching: " + uri, e);
            return none();
        } finally {
            if (response != null) {
                try {
                    response.consume();
                } catch (Exception ignore) {
                }
            }
        }
    }

    public static HTTPCache createHttpCache(Config config) {
        SchemeRegistry schemeRegistry = new SchemeRegistry();
        schemeRegistry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
        schemeRegistry.register(new Scheme("https", new EasySSLSocketFactory(), 443));
        BasicHttpParams params = new BasicHttpParams();
        ThreadSafeClientConnManager cm = new ThreadSafeClientConnManager(params, schemeRegistry);
        ResponseResolver responseResolver = new HTTPClientResponseResolver(new DefaultHttpClient(cm, new BasicHttpParams()));

        if (config.gitorious.sessionValue.isSome()) {
            responseResolver = new GitoriousResponseResolver(config.gitorious.sessionValue.some(), responseResolver);
        }

        responseResolver = new TimingResponseResolver(responseResolver);

        return new HTTPCache(new MemoryCacheStorage(), responseResolver);
    }

    private static class TimingResponseResolver implements ResponseResolver {
        private final ResponseResolver r;

        private TimingResponseResolver(ResponseResolver r) {
            this.r = r;
        }

        public HTTPResponse resolve(HTTPRequest request) throws IOException {
            logger.debug(request.getRequestURI() + ": Executing");
            long start = currentTimeMillis();
            Status status = null;
            try {
                HTTPResponse response = r.resolve(request);
                status = response.getStatus();
                return response;
            } finally {
                long end = currentTimeMillis();

                String s = request.getRequestURI() + ": Executed in " + (end - start) + "ms, ";

                if (status != null) {
                    s += "response: " + status.getCode() + " " + status.getName();
                } else {
                    s += "with exception";
                }

                logger.debug(s);
            }
        }

        public void shutdown() {
            r.shutdown();
        }
    }

    private static class GitoriousResponseResolver implements ResponseResolver {
        private final String gitoriousSessionValue;
        private final ResponseResolver responseResolver;

        public GitoriousResponseResolver(String gitoriousSessionValue, ResponseResolver responseResolver) {
            this.gitoriousSessionValue = gitoriousSessionValue;
            this.responseResolver = responseResolver;
        }

        public HTTPResponse resolve(HTTPRequest request) throws IOException {
            request = request.addHeader("Cookie", "_gitorious_sess=" + gitoriousSessionValue);
            return responseResolver.resolve(request);
        }

        public void shutdown() {
            responseResolver.shutdown();
        }
    }
}
