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

package org.elasticsearch.common.recycler;

import org.elasticsearch.common.lease.Releasable;

/**
 * 一个回收的对象，注意，实现应该支持调用获取，然后在不同的线程上回收。
 */
public interface Recycler<T> extends Releasable {

    interface Factory<T> {
        Recycler<T> build();
    }

    interface C<T> {

        /** 创建给定大小的新空实例。 */
        T newInstance(int sizing);

        /** 回收数据。释放数据结构时调用此操作。 */
        void recycle(T value);

        /** 销毁数据。此操作允许数据结构在GC之前释放任何内部资源。 */
        void destroy(T value);
    }

    interface V<T> extends Releasable {

        /** 参考价值。 */
        T v();

        /** 此实例是已回收（true）还是新分配（false）。 */
        boolean isRecycled();

    }

    void close();

    V<T> obtain();

    V<T> obtain(int sizing);
}
