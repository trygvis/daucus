package io.trygvis.esper.testing.util.object;

import org.slf4j.*;

import javax.sql.*;
import java.io.*;
import java.sql.*;
import java.util.concurrent.*;

import static java.lang.System.currentTimeMillis;

public class ObjectUtil {

    public static <A extends TransactionalActor> ActorRef<A> threadedActor(String threadName, long delay, DataSource dataSource, String name, A actor) {
        return new ThreadedActor<>(dataSource, threadName, name, actor, delay);
    }

    public static <A extends TransactionalActor> ActorRef<A> scheduledActorWithFixedDelay(ScheduledExecutorService scheduledExecutorService, long initialDelay, long delay, TimeUnit unit, DataSource dataSource, String name, A actor) {
        return new ScheduledActor<>(scheduledExecutorService, initialDelay, delay, unit, dataSource, name, actor);
    }

    private static class TransactionalActorWrapper<A extends TransactionalActor> implements Runnable {
        private static final Logger logger = LoggerFactory.getLogger(TransactionalActorWrapper.class);

        private final DataSource dataSource;
        private final String name;
        private final A actor;

        TransactionalActorWrapper(DataSource dataSource, String name, A actor) {
            this.dataSource = dataSource;
            this.name = name;
            this.actor = actor;
        }

        public void run() {
            try {
                Connection c = dataSource.getConnection();

                try {
                    try (PreparedStatement s = c.prepareStatement("set application_name = 'Actor: " + name + "';")) {
//                        s.setString(1, "Actor: " + name);
                        s.executeUpdate();
                        s.close();
                    }

                    actor.act(c);
                    long start = currentTimeMillis();
                    c.commit();
                    long end = currentTimeMillis();
                    logger.debug("COMMIT performed in in " + (end - start) + "ms.");
                }
                catch(SQLException e) {
                    c.rollback();
                    throw e;
                } finally {
                    c.close();
                }
            } catch (Throwable e) {
                logger.warn("Exception in thread " + Thread.currentThread().getName());
                e.printStackTrace(System.out);
            }
        }
    }

    static class ScheduledActor<A extends TransactionalActor> implements ActorRef<A>, Runnable {
        private final ScheduledFuture<?> future;

        private final TransactionalActorWrapper<A> actor;

        ScheduledActor(ScheduledExecutorService executorService, long initialDelay, long delay, TimeUnit unit, DataSource dataSource, String name, A actor) {
            future = executorService.scheduleWithFixedDelay(this, initialDelay, delay, unit);
            this.actor = new TransactionalActorWrapper<>(dataSource, name, actor);
        }

        public A underlying() {
            return actor.actor;
        }

        public void close() throws IOException {
            future.cancel(true);
        }

        public void run() {
            actor.run();
        }
    }

    static class ThreadedActor<A extends TransactionalActor> implements ActorRef<A>, Runnable, Closeable {

        private final TransactionalActorWrapper<A> actor;
        private final long delay;
        private final Thread thread;
        private boolean shouldRun = true;

        ThreadedActor(DataSource dataSource, String threadName, String name, A actor, long delay) {
            this.actor = new TransactionalActorWrapper<>(dataSource, name, actor);
            this.delay = delay;
            thread = new Thread(this, threadName);
            thread.setDaemon(true);
            thread.start();
        }

        public A underlying() {
            return actor.actor;
        }

        @SuppressWarnings("ConstantConditions")
        public void run() {
            while (shouldRun) {
                actor.run();

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
                } catch (InterruptedException ignore) {
                }
            }
        }
    }
}
