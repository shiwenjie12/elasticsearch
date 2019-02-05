/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.common.lucene;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.LeafReader;
import org.elasticsearch.Assertions;
import org.elasticsearch.index.shard.ShardId;
import org.elasticsearch.index.shard.ShardUtils;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A map between segment core cache keys and the shard that these segments
 * belong to. This allows to get the shard that a segment belongs to or to get
 * the entire set of live core cache keys for a given index. In order to work
 * this class needs to be notified about new segments. It modifies the current
 * mappings as segments that were not known before are added and prevents the
 * structure from growing indefinitely by registering close listeners on these
 * segments so that at any time it only tracks live segments.
 * 段核心缓存键与这些段所属的分片之间的映射。 这允许获取段所属的分片或获取给定索引的整个实时核心缓存键集。
 * 为了工作，这个课程需要通知新的细分。 它将当前映射修改为添加之前未知的段，并通过在这些段上注册关闭侦听器来防止结构无限增长，
 * 以便在任何时候它只跟踪实时段。
 *
 * NOTE: 这很重。 除非绝对必要，否则避免使用此类.
 */
public final class ShardCoreKeyMap {

    private final Map<IndexReader.CacheKey, ShardId> coreKeyToShard;
    private final Map<String, Set<IndexReader.CacheKey>> indexToCoreKey;

    public ShardCoreKeyMap() {
        coreKeyToShard = new ConcurrentHashMap<>();
        indexToCoreKey = new HashMap<>();
    }

    /**
     * Register a {@link LeafReader}. This is necessary so that the core cache
     * key of this reader can be found later using {@link #getCoreKeysForIndex(String)}.
     * 注册{@link LeafReader}。 这是必要的，以便稍后可以使用{@link #getCoreKeysForIndex(String)}找到此阅读器的核心缓存密钥。
     */
    public void add(LeafReader reader) {
        final ShardId shardId = ShardUtils.extractShardId(reader);
        if (shardId == null) {
            throw new IllegalArgumentException("Could not extract shard id from " + reader);
        }
        final IndexReader.CacheHelper cacheHelper = reader.getCoreCacheHelper();
        if (cacheHelper == null) {
            throw new IllegalArgumentException("Reader " + reader + " does not support caching");
        }
        final IndexReader.CacheKey coreKey = cacheHelper.getKey();

        if (coreKeyToShard.containsKey(coreKey)) {
            // Do this check before entering the synchronized block in order to
            // avoid taking the mutex if possible (which should happen most of
            // the time).
            return;
        }

        final String index = shardId.getIndexName();
        synchronized (this) {
            if (coreKeyToShard.containsKey(coreKey) == false) {
                Set<IndexReader.CacheKey> objects = indexToCoreKey.get(index);
                if (objects == null) {
                    objects = new HashSet<>();
                    indexToCoreKey.put(index, objects);
                }
                final boolean added = objects.add(coreKey);
                assert added;
                // 设置关闭监听器
                IndexReader.ClosedListener listener = ownerCoreCacheKey -> {
                    assert coreKey == ownerCoreCacheKey;
                    synchronized (ShardCoreKeyMap.this) {
                        coreKeyToShard.remove(ownerCoreCacheKey);
                        final Set<IndexReader.CacheKey> coreKeys = indexToCoreKey.get(index);
                        final boolean removed = coreKeys.remove(coreKey);
                        assert removed;
                        if (coreKeys.isEmpty()) {
                            indexToCoreKey.remove(index);
                        }
                    }
                };
                boolean addedListener = false;
                try {
                    cacheHelper.addClosedListener(listener);
                    addedListener = true;

                    // Only add the core key to the map as a last operation so that
                    // if another thread sees that the core key is already in the
                    // map (like the check just before this synchronized block),
                    // then it means that the closed listener has already been
                    // registered.
                    ShardId previous = coreKeyToShard.put(coreKey, shardId);
                    assert previous == null;
                } finally {
                    if (false == addedListener) {
                        try {
                            listener.onClose(coreKey);
                        } catch (IOException e) {
                            throw new RuntimeException("Blow up trying to recover from failure to add listener", e);
                        }
                    }
                }
            }
        }
    }

    /**
     * Return the {@link ShardId} that holds the given segment, or {@code null}
     * if this segment is not tracked.
     */
    public synchronized ShardId getShardId(Object coreKey) {
        return coreKeyToShard.get(coreKey);
    }

    /**
     * Get the set of core cache keys associated with the given index.
     */
    public synchronized Set<Object> getCoreKeysForIndex(String index) {
        final Set<IndexReader.CacheKey> objects = indexToCoreKey.get(index);
        if (objects == null) {
            return Collections.emptySet();
        }
        // we have to copy otherwise we risk ConcurrentModificationException
        return Collections.unmodifiableSet(new HashSet<>(objects));
    }

    /**
     * Return the number of tracked segments.
     */
    public synchronized int size() {
        assert assertSize();
        return coreKeyToShard.size();
    }

    private synchronized boolean assertSize() {
        if (!Assertions.ENABLED) {
            throw new AssertionError("only run this if assertions are enabled");
        }
        Collection<Set<IndexReader.CacheKey>> values = indexToCoreKey.values();
        int size = 0;
        for (Set<IndexReader.CacheKey> value : values) {
            size += value.size();
        }
        return size == coreKeyToShard.size();
    }

}
