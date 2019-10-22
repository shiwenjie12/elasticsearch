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

package org.elasticsearch.action;

import org.elasticsearch.action.support.IndicesOptions;

/**
 * 需要由与一个或多个索引相关的所有{@link org.elasticsearch.action.ActionRequest}子类实现。允许检索操作与哪些索引相关。
 * 如果在分布式执行外部请求期间发出内部请求，它们仍将返回原始请求所涉及的索引。
 */
public interface IndicesRequest {

    /**
     * 返回操作与之关联的索引数组
     */
    String[] indices();

    /**
     * 返回用于解析索引的索引选项。它们告诉我们是否接受单个索引，是否将空数组转换为_all，以及如何在需要时扩展通配符。
     */
    IndicesOptions indicesOptions();

    interface Replaceable extends IndicesRequest {
        /**
         * Sets the indices that the action relates to.
         */
        IndicesRequest indices(String... indices);
    }
}
