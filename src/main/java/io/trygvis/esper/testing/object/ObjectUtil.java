package io.trygvis.esper.testing.object;

import javax.sql.*;
import java.io.*;
import java.sql.*;

public class ObjectUtil {

    public static <A extends TransactionalActor> ActorRef<A> threadedActor(DataSource dataSource, long delay, A actor) {
        return new ThreadedActor<>(dataSource, actor, delay);
    }

    static class ThreadedActor<A extends TransactionalActor> implements ActorRef<A>, Runnable, Closeable {

        private final DataSource dataSource;
        private final A actor;
        private final long delay;
        private final Thread thread;
        private boolean shouldRun = true;

        ThreadedActor(DataSource dataSource, A actor, long delay) {
            this.dataSource = dataSource;
            this.actor = actor;
            this.delay = delay;
            thread = new Thread(this);
            thread.setDaemon(true);
            thread.start();
        }

        public A underlying() {
            return actor;
        }

        @SuppressWarnings("ConstantConditions")
        public void run() {
            while (shouldRun) {
                try {
                    try (Connection c = dataSource.getConnection()) {
                        try {
                            actor.act(c);
                        } finally {
                            c.rollback();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace(System.out);
                }

                try {
                    Thread.sleep(delay);
                } catch (InterruptedException e) {
                    // ignore
                }
            }
        }

        public void close() throws IOException {
            shouldRun = false;
            thread.interrupt();
            while (thread.isAlive()) {
                try {
                    thread.join();
                } catch (InterruptedException e) {
                    continue;
                }
            }
        }
    }
}
