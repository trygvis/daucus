package io.trygvis.esper.testing.util.object;

import java.io.*;

public interface ObjectFactory<K, V extends Closeable> {
    V create(K k);
}
