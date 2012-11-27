package io.trygvis.esper.testing;

import static java.lang.System.*;

import org.apache.http.conn.scheme.*;
import org.apache.http.impl.client.*;
import org.apache.http.impl.conn.tsccm.*;
import org.apache.http.params.*;
import org.codehaus.httpcache4j.*;
import org.codehaus.httpcache4j.cache.*;
import org.codehaus.httpcache4j.resolver.*;

import java.io.*;

public class HttpClient {

    public static HTTPCache createHttpClient(Config config) {
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
            System.out.println(request.getRequestURI() + ": Executing");
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

                System.out.println(s);
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
