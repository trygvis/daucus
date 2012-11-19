package io.trygvis.esper.testing;

import org.apache.http.conn.scheme.*;
import org.apache.http.conn.ssl.*;
import org.apache.http.impl.client.*;
import org.apache.http.impl.conn.tsccm.*;
import org.apache.http.params.*;
import org.codehaus.httpcache4j.cache.*;
import org.codehaus.httpcache4j.resolver.*;

public class Http {

    public static final HTTPCache http;

    static {
        SchemeRegistry schemeRegistry = new SchemeRegistry();
        schemeRegistry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
        schemeRegistry.register(new Scheme("https", new EasySSLSocketFactory(), 443));

        BasicHttpParams params = new BasicHttpParams();
        ThreadSafeClientConnManager cm = new ThreadSafeClientConnManager(params, schemeRegistry);
        DefaultHttpClient httpClient = new DefaultHttpClient(cm, params);
        HTTPClientResponseResolver resolver = new HTTPClientResponseResolver(httpClient);
        http = new HTTPCache(new MemoryCacheStorage(), resolver);
    }
}
