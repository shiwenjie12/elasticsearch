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

package org.elasticsearch.tasks;

import org.elasticsearch.common.Nullable;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 可以取消的任务
 */
public abstract class CancellableTask extends Task {

    private final AtomicReference<String> reason = new AtomicReference<>();

    public CancellableTask(long id, String type, String action, String description, TaskId parentTaskId, Map<String, String> headers) {
        super(id, type, action, description, parentTaskId, headers);
    }

    /**
     * 取消此任务时，任务管理器将调用此方法。
     */
    final void cancel(String reason) {
        assert reason != null;
        this.reason.compareAndSet(null, reason);
        onCancelled();
    }

    /**
     * Returns true if this task should be automatically cancelled if the coordinating node that
     * requested this task left the cluster.
     * 如果请求此任务的协调节点离开集群，则应自动取消此任务，则返回true。
     */
    public boolean cancelOnParentLeaving() {
        return true;
    }

    /**
     * Returns true if this task can potentially have children that need to be cancelled when it parent is cancelled.
     */
    public abstract boolean shouldCancelChildrenOnCancellation();

    public boolean isCancelled() {
        return reason.get() != null;
    }

    /**
     * The reason the task was cancelled or null if it hasn't been cancelled.
     */
    @Nullable
    public String getReasonCancelled() {
        return reason.get();
    }

    /**
     * 在任务被取消后调用，以便它可以采取它必须采取的任何操作。
     */
    protected void onCancelled() {
    }
}
