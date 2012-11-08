package io.trygvis.esper.testing;

import fj.*;

import java.util.*;
import java.util.concurrent.*;

public class ResourceManager<K, V> {
    private final Equal<K> equal;
    private final Callable<List<K>> discoverer;
    private Map<K, V> map = Collections.emptyMap();

    public ResourceManager(Equal<K> equal, ScheduledExecutorService executorService, int delay, Callable<List<K>> discoverer) {
        this.equal = equal;
        this.discoverer = discoverer;

        executorService.scheduleWithFixedDelay(new Runnable() {
            public void run() {
                work();
            }
        }, delay, delay, TimeUnit.MILLISECONDS);
    }

    private void work() {
        try {
            List<K> keys = discoverer.call();
        } catch (Exception e) {
            return;
        }
    }
}
