/*
 * Copyright (c) 2017 Suk Honzeon
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package me.asu.http.util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.*;
import lombok.Getter;

/**
 * TimeToLiveCache.
 * <p>2017 Suk All rights reserved.</p>
 *
 * @author Suk
 * @version 1.0.0
 * @since 2017-09-12 15:45
 */
public class TimeToLiveCache<K, T> implements Serializable {

    private static final long serialVersionUID = -3021761536220090818L;
    ExecutorService executorService = new ThreadPoolExecutor(0,
            Runtime.getRuntime().availableProcessors() * 2, 60L, TimeUnit.SECONDS,
            new SynchronousQueue<Runnable>(),
            new NamedThreadFactory("response-timeout-event-thread", true));
    @Getter
    private long                              timeToLive;
    private ConcurrentHashMap<K, CacheObject> cacheMap;
    private CheckThread                       checkThread;
    private List<TimeoutHandler> handlers = new ArrayList<TimeoutHandler>();
    @Getter
    private boolean shutdown = false;
    public TimeToLiveCache(final long timeToLive, final long timerInterval) {
        this.timeToLive = timeToLive;

        cacheMap = new ConcurrentHashMap<K, CacheObject>();

        if (this.timeToLive > 0 && timerInterval > 0) {
            checkThread = new CheckThread(timerInterval);
        }

        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                shutdownGracefully();
            }
        }, "TimeToLiveCache-Shutdown"));
    }

    public void shutdownGracefully() {
        executorService.shutdownNow();
        try {
            executorService.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) { /*ignore*/ }
        if (checkThread != null) {
            checkThread.cancel();
        }
        shutdown = true;
    }

    public void put(K key, T value) {
        cacheMap.put(key, new CacheObject(value));
    }

    public T get(K key) {
        CacheObject c = cacheMap.get(key);

        if (c == null) {
            return null;
        } else {
            c.lastAccessed = System.currentTimeMillis();
            return c.value;
        }
    }

    public T remove(K key) {
        CacheObject remove = cacheMap.remove(key);
        if (remove != null) {
            return remove.value;
        } else {
            return null;
        }
    }

    public int size() {
        return cacheMap.size();
    }

    public void addTimeoutHandler(TimeoutHandler<K, T> handler) {
        if (handler == null) {
            return;
        }
        handlers.add(handler);
    }

    public void removeTimeoutHandler(TimeoutHandler<K, T> handler) {
        if (handler == null) {
            return;
        }
        handlers.remove(handler);
    }

    private void cleanup() {
        long now = System.currentTimeMillis();
        ArrayList<K> deleteKey = null;

        synchronized (cacheMap) {
            Iterator<Entry<K, CacheObject>> itr = cacheMap.entrySet().iterator();
            deleteKey = new ArrayList<K>((cacheMap.size() / 2) + 1);
            while (itr.hasNext()) {
                Entry<K, CacheObject> next = itr.next();
                K key = next.getKey();
                CacheObject c = next.getValue();
                if (c != null && (now > (timeToLive + c.lastAccessed))) {
                    deleteKey.add(key);
                }
            }
        }

        for (K key : deleteKey) {
            CacheObject remove = cacheMap.remove(key);
            notifyTimeoutObject(key, remove.getValue());
            Thread.yield();
        }
    }

    private void notifyTimeoutObject(final K key, final T value) {
        if (handlers != null) {
            for (final TimeoutHandler<K, T> handler : handlers) {
                executorService.execute(new Runnable() {
                    @Override
                    public void run() {
                        handler.fireTimeout(key, value);
                    }
                });
            }
        }
    }

    public interface TimeoutHandler<K, T> {

        void fireTimeout(K k, T v);
    }

    class CheckThread extends Thread {

        boolean running = false;
        private long timerInterval;

        CheckThread(long timerInterval) {
            super("TimeToLiveCache-Check-Thread");
            setDaemon(true);
            this.timerInterval = timerInterval;
        }

        @Override
        public void run() {
            if (running) {
                return;
            }
            running = true;
            while (running) {
                try {
                    Thread.sleep(timerInterval);
                } catch (InterruptedException ex) {
                }
                cleanup();
            }
        }

        public void cancel() {
            running = false;
        }
    }

    @lombok.Data
    class CacheObject implements Serializable {

        private static final long serialVersionUID = -2040740421609709915L;
        public long lastAccessed = System.currentTimeMillis();
        public T value;

        protected CacheObject(T value) {
            this.value = value;
        }
    }
}
