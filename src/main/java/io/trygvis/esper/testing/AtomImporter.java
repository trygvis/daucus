package io.trygvis.esper.testing;

import org.apache.abdera.*;
import org.apache.abdera.protocol.client.*;
import org.apache.abdera.protocol.client.cache.*;

public class AtomImporter {
    public static void main(String[] args) {
        Abdera abdera = new Abdera();
        AbderaClient abderaClient = new AbderaClient(abdera, new LRUCache(abdera, 1000));

        while(true) {
            ClientResponse response = abderaClient.get("http://gitorious.org/qt.atom");

//            response.
        }
    }
}
