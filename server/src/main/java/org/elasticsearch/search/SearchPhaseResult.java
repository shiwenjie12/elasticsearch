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

package org.elasticsearch.search;

import org.elasticsearch.common.io.stream.Streamable;
import org.elasticsearch.search.fetch.FetchSearchResult;
import org.elasticsearch.search.query.QuerySearchResult;
import org.elasticsearch.transport.TransportResponse;

/**
 * 此类是所有搜索相关结果的基类。它包含执行的分片目标，用于在协调节点上引用结果的分片索引以及用于在执行节点上引用请求上下文的请求ID。
 * 请求ID特别重要，因为它用于跨搜索阶段引用和维护上下文，以确保使用相同的时间点快照进行查询和获取等。
 */
public abstract class SearchPhaseResult extends TransportResponse implements Streamable {

    private SearchShardTarget searchShardTarget;
    private int shardIndex = -1;
    protected long requestId;

    /**
     * Returns the results request ID that is used to reference the search context on the executing
     * node
     */
    public long getRequestId() {
        return requestId;
    }

    /**
     * Returns the shard index in the context of the currently executing search request that is
     * used for accounting on the coordinating node
     */
    public int getShardIndex() {
        assert shardIndex != -1 : "shardIndex is not set";
        return shardIndex;
    }

    public SearchShardTarget getSearchShardTarget() {
        return searchShardTarget;
    }

    public void setSearchShardTarget(SearchShardTarget shardTarget) {
        this.searchShardTarget = shardTarget;
    }

    public void setShardIndex(int shardIndex) {
        assert shardIndex >= 0 : "shardIndex must be >= 0 but was: " + shardIndex;
        this.shardIndex = shardIndex;
    }

    /**
     * Returns the query result iff it's included in this response otherwise <code>null</code>
     */
    public QuerySearchResult queryResult() {
        return null;
    }

    /**
     * Returns the fetch result iff it's included in this response otherwise <code>null</code>
     */
    public FetchSearchResult fetchResult() { return null; }
}
