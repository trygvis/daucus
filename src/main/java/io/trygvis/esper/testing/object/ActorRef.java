package io.trygvis.esper.testing.object;

import java.io.*;

public interface ActorRef<A> extends Closeable {
    A underlying();
}
