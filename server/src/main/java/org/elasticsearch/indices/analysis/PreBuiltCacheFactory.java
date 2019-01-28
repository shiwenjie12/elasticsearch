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
package org.elasticsearch.indices.analysis;

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.Version;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class PreBuiltCacheFactory {

    /**
     * 缓存分析器的策略
     *
     * ONE               正好存储了一个版本。 对于不存储版本信息的分析器很有用
     * LUCENE            每个lucene版本都存储了一个版本。 用于防止具有相同版本的不同分析仪
     * ELASTICSEARCH     每个elasticsearch版本只存储一个版本。 如果在elasticsearch版本之间更改分析器，则lucene版本不会更改时很有用
     */
    public enum CachingStrategy { ONE, LUCENE, ELASTICSEARCH };

    public interface PreBuiltCache<T> {

        T get(Version version);

        void put(Version version, T t);

        Collection<T> values();
    }

    private PreBuiltCacheFactory() {}

    public static <T> PreBuiltCache<T> getCache(CachingStrategy cachingStrategy) {
        switch (cachingStrategy) {
            case ONE:
                return new PreBuiltCacheStrategyOne<>();
            case LUCENE:
                return new PreBuiltCacheStrategyLucene<>();
            case ELASTICSEARCH:
                return new PreBuiltCacheStrategyElasticsearch<>();
            default:
                throw new ElasticsearchException("No action configured for caching strategy[" + cachingStrategy + "]");
        }
    }

    /**
     * 这是一个非常简单的缓存，它只包含一个版本
     */
    private static class PreBuiltCacheStrategyOne<T> implements PreBuiltCache<T> {

        private T model = null;

        @Override
        public T get(Version version) {
            return model;
        }

        @Override
        public void put(Version version, T model) {
            this.model = model;
        }

        @Override
        public Collection<T> values() {
            return Collections.singleton(model);
        }
    }

    /**
     * 此缓存包含每个elasticsearch版本对象的一个版本
     */
    private static class PreBuiltCacheStrategyElasticsearch<T> implements PreBuiltCache<T> {

        Map<Version, T> mapModel = new HashMap<>(2);

        @Override
        public T get(Version version) {
            return mapModel.get(version);
        }

        @Override
        public void put(Version version, T model) {
            mapModel.put(version, model);
        }

        @Override
        public Collection<T> values() {
            return mapModel.values();
        }
    }

    /**
     * 此缓存使用lucene版本进行缓存
     */
    private static class PreBuiltCacheStrategyLucene<T> implements PreBuiltCache<T> {

        private Map<org.apache.lucene.util.Version, T> mapModel = new HashMap<>(2);

        @Override
        public T get(Version version) {
            return mapModel.get(version.luceneVersion);
        }

        @Override
        public void put(org.elasticsearch.Version version, T model) {
            mapModel.put(version.luceneVersion, model);
        }

        @Override
        public Collection<T> values() {
            return mapModel.values();
        }
    }
}
