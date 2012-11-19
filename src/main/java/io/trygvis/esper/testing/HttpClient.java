package io.trygvis.esper.testing;

import static java.lang.System.*;
import org.codehaus.httpcache4j.*;
import org.codehaus.httpcache4j.cache.*;
import org.codehaus.httpcache4j.client.*;
import org.codehaus.httpcache4j.resolver.*;

import java.io.*;

public class HttpClient {

    public static HTTPCache createHttpClient(Config config) {
        return new HTTPCache(new MemoryCacheStorage(), createResponseResolver(config));
    }

    private static ResponseResolver createResponseResolver(final Config config) {
        ResponseResolver responseResolver = HTTPClientResponseResolver.createMultithreadedInstance();

        if (config.gitoriousSessionValue.isSome()) {
            responseResolver = new GitoriousResponseResolver(config.gitoriousSessionValue.some(), responseResolver);
        }

        responseResolver = new TimingResponseResolver(responseResolver);

        return responseResolver;
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
