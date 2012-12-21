package io.trygvis.esper.testing.util.object;

import java.io.*;

public interface ActorRef<A> extends Closeable {
    A underlying();
}
