package io.trygvis.esper.testing.object;

import java.io.*;

public interface ObjectFactory<K, V extends Closeable> {
    V create(K k);
}
