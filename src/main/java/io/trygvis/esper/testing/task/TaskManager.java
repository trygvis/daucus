package io.trygvis.esper.testing.task;

import com.jolbox.bonecp.*;
import org.slf4j.*;
import org.slf4j.helpers.*;

import java.sql.*;
import java.util.*;
import java.util.concurrent.*;

public class TaskManager<T> {
    public final String table;
    public final TaskExecutorFactory taskExecutorFactory;
    public final Executor executor;
    public final BoneCP boneCP;

    private final Set<String> inProgress = new HashSet<>();

    public TaskManager(String table, TaskExecutorFactory taskExecutorFactory, Executor executor, BoneCP boneCP) {
        this.table = table;
        this.taskExecutorFactory = taskExecutorFactory;
        this.executor = executor;
        this.boneCP = boneCP;

        Thread thread = new Thread(new Runnable() {
            public void run() {
                TaskManager.this.run();
            }
        });
        thread.start();
    }

    private void run() {
        while (true) {
            try {
                try (Connection c = boneCP.getConnection()) {
                    singleRun(c);
                }
            } catch (SQLException e) {
                e.printStackTrace(System.out);
            }
        }
    }

    private void singleRun(Connection c) throws SQLException {
        TaskDao taskDao = new TaskDao(c, table);

        List<String> ids = taskDao.findTasks();

        System.out.println("Found " + ids.size() + " new tasks.");

        synchronized (inProgress) {
            System.out.println("Have " + inProgress.size() + " tasks in progress already");
            ids.removeAll(inProgress);

            List<Runnable> runnables = new ArrayList<>(ids.size());

            for (final String id : ids) {
                System.out.println("Scheduling " + id);

                final TaskExecutor executor = taskExecutorFactory.create();
                runnables.add(new Runnable() {
                    public void run() {
                        System.out.println("Executing " + id);
                        try {
                            try (Connection c2 = boneCP.getConnection()) {
                                SqlLogger logger = new SqlLogger();
                                executor.execute(id, c2, logger);
                                // TODO: insert log statements
                                System.out.println("Executing " + id);
                                c2.commit();
                            }
                        } catch (SQLException e) {
                            e.printStackTrace();
                        } finally {
                            synchronized (inProgress) {
                                inProgress.remove(id);
                            }
                        }
                    }
                });
                inProgress.add(id);
            }
            for (Runnable runnable : runnables) {
                executor.execute(runnable);
            }
        }
    }

    /**
     * See MessageFormatter
     */
    private static class SqlLogger extends MarkerIgnoringBase {

        public boolean isTraceEnabled() {
            throw new RuntimeException("Not implemented");
        }

        public void trace(String msg) {
            throw new RuntimeException("Not implemented");
        }

        public void trace(String format, Object arg) {
            throw new RuntimeException("Not implemented");
        }

        public void trace(String format, Object arg1, Object arg2) {
            throw new RuntimeException("Not implemented");
        }

        public void trace(String format, Object[] argArray) {
            throw new RuntimeException("Not implemented");
        }

        public void trace(String msg, Throwable t) {
            throw new RuntimeException("Not implemented");
        }

        public boolean isDebugEnabled() {
            throw new RuntimeException("Not implemented");
        }

        public void debug(String msg) {
            throw new RuntimeException("Not implemented");
        }

        public void debug(String format, Object arg) {
            throw new RuntimeException("Not implemented");
        }

        public void debug(String format, Object arg1, Object arg2) {
            throw new RuntimeException("Not implemented");
        }

        public void debug(String format, Object[] argArray) {
            throw new RuntimeException("Not implemented");
        }

        public void debug(String msg, Throwable t) {
            throw new RuntimeException("Not implemented");
        }

        public boolean isInfoEnabled() {
            throw new RuntimeException("Not implemented");
        }

        public void info(String msg) {
            throw new RuntimeException("Not implemented");
        }

        public void info(String format, Object arg) {
            throw new RuntimeException("Not implemented");
        }

        public void info(String format, Object arg1, Object arg2) {
            throw new RuntimeException("Not implemented");
        }

        public void info(String format, Object[] argArray) {
            throw new RuntimeException("Not implemented");
        }

        public void info(String msg, Throwable t) {
            throw new RuntimeException("Not implemented");
        }

        public boolean isWarnEnabled() {
            throw new RuntimeException("Not implemented");
        }

        public void warn(String msg) {
            throw new RuntimeException("Not implemented");
        }

        public void warn(String format, Object arg) {
            throw new RuntimeException("Not implemented");
        }

        public void warn(String format, Object[] argArray) {
            throw new RuntimeException("Not implemented");
        }

        public void warn(String format, Object arg1, Object arg2) {
            throw new RuntimeException("Not implemented");
        }

        public void warn(String msg, Throwable t) {
            throw new RuntimeException("Not implemented");
        }

        public boolean isErrorEnabled() {
            throw new RuntimeException("Not implemented");
        }

        public void error(String msg) {
            throw new RuntimeException("Not implemented");
        }

        public void error(String format, Object arg) {
            throw new RuntimeException("Not implemented");
        }

        public void error(String format, Object arg1, Object arg2) {
            throw new RuntimeException("Not implemented");
        }

        public void error(String format, Object[] argArray) {
            throw new RuntimeException("Not implemented");
        }

        public void error(String msg, Throwable t) {
            throw new RuntimeException("Not implemented");
        }
    }
}

interface TaskExecutorFactory {
    TaskExecutor create();
}

interface TaskExecutor {
    void execute(String id, Connection c, Logger logger)
        throws SQLException;
}
