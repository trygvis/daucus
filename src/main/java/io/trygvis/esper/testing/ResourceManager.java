package io.trygvis.esper.testing;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

public class ResourceManager<K extends Comparable<K>, V> implements Closeable {
    private final ResourceManagerCallbacks<K, V> callbacks;
    private final ScheduledFuture<?> future;

    private Map<K, V> map = Collections.emptyMap();

    public interface ResourceManagerCallbacks<K extends Comparable<K>, V> {
        Set<K> discover() throws Exception;

        V onNew(K key);

        void onGone(K key, V value);
    }

    public ResourceManager(ScheduledExecutorService executorService, int delay,
                           ResourceManagerCallbacks<K, V> callbacks) {
        this.callbacks = callbacks;

        future = executorService.scheduleWithFixedDelay(new Runnable() {
            public void run() {
                work();
            }
        }, delay, delay, TimeUnit.MILLISECONDS);

    }

    private void work() {
        try {
            System.out.println("Discovering...");
            Set<K> keys = callbacks.discover();

            Set<K> found = new HashSet<>();

            for (K key : keys) {
                if (map.containsKey(key)) {
                    continue;
                }

                found.add(key);
            }

            Set<K> lost = new HashSet<>(map.keySet());
            lost.retainAll(found);

            System.out.println("Discovered " + keys.size() + " keys, new: " + found.size() + ", gone=" + lost.size());

            Map<K, V> newMap = new HashMap<>(found.size());

            for (K k : found) {
                newMap.put(k, callbacks.onNew(k));
            }

            for (K k : lost) {
                callbacks.onGone(k, map.get(k));
            }

            map = newMap;
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
    }

    public void close() {
        future.cancel(true);
    }
}
