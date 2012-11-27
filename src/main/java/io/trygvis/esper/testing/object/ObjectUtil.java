package io.trygvis.esper.testing.object;

import javax.sql.*;
import java.io.*;
import java.sql.*;
import java.util.concurrent.*;

public class ObjectUtil {

    public static <A extends TransactionalActor> ActorRef<A> threadedActor(String threadName, long delay, DataSource dataSource, A actor) {
        return new ThreadedActor<>(dataSource, threadName, actor, delay);
    }

    public static <A extends TransactionalActor> ActorRef<A> scheduledActorWithFixedDelay(ScheduledExecutorService scheduledExecutorService, long initialDelay, long delay, TimeUnit unit, DataSource dataSource, A actor) {
        return new ScheduledActor<>(scheduledExecutorService, initialDelay, delay, unit, dataSource, actor);
    }

    private static class TransactionalActorWrapper<A extends TransactionalActor> implements Runnable {
        private final DataSource dataSource;
        private final A actor;

        TransactionalActorWrapper(DataSource dataSource, A actor) {
            this.dataSource = dataSource;
            this.actor = actor;
        }

        public void run() {
            try {
                Connection c = dataSource.getConnection();
                try {
                    actor.act(c);
                    c.commit();
                }
                catch(SQLException e) {
                    c.rollback();
                } finally {
                    c.close();
                }
            } catch (Throwable e) {
                System.out.println("Exception in thread " + Thread.currentThread().getName());
                e.printStackTrace(System.out);
            }
        }
    }

    static class ScheduledActor<A extends TransactionalActor> implements ActorRef<A>, Runnable {
        private final ScheduledFuture<?> future;

        private final TransactionalActorWrapper<A> actor;

        ScheduledActor(ScheduledExecutorService executorService, long initialDelay, long delay, TimeUnit unit, DataSource dataSource, A actor) {
            future = executorService.scheduleWithFixedDelay(this, initialDelay, delay, unit);
            this.actor = new TransactionalActorWrapper<>(dataSource, actor);
        }

        public A underlying() {
            return actor.actor;
        }

        public void close() throws IOException {
            future.cancel(true);
        }

        @Override
        public void run() {
            actor.run();
        }
    }

    static class ThreadedActor<A extends TransactionalActor> implements ActorRef<A>, Runnable, Closeable {

        private final TransactionalActorWrapper<A> actor;
        private final long delay;
        private final Thread thread;
        private boolean shouldRun = true;

        ThreadedActor(DataSource dataSource, String threadName, A actor, long delay) {
            this.actor = new TransactionalActorWrapper<A>(dataSource, actor);
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
                } catch (InterruptedException e) {
                    continue;
                }
            }
        }
    }
}
