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
package org.elasticsearch.cluster;

import org.elasticsearch.common.Nullable;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * 集群状态任务执行器
 * @param <T>
 */
public interface ClusterStateTaskExecutor<T> {
    /**
     * 根据当前状态和给定任务更新集群状态。如果不应更改状态，则返回*相同的实例*。
     */
    ClusterTasksResult<T> execute(ClusterState currentState, List<T> tasks) throws Exception;

    /**
     * 指示此执行程序是否应仅在当前节点为master时运行
     */
    default boolean runOnlyOnMaster() {
        return true;
    }

    /**
     * 发布新群集状态后调用回调。请注意，如果未更新群集状态，则不会调用此方法。
     *
     * Note that this method will be executed using system context.
     *
     * @param clusterChangedEvent the change event for this cluster state change, containing
     *                            both old and new states
     */
    default void clusterStatePublished(ClusterChangedEvent clusterChangedEvent) {
    }

    /**
     * Builds a concise description of a list of tasks (to be used in logging etc.).
     *
     * Note that the tasks given are not necessarily the same as those that will be passed to {@link #execute(ClusterState, List)}.
     * but are guaranteed to be a subset of them. This method can be called multiple times with different lists before execution.
     * This allows groupd task description but the submitting source.
     */
    default String describeTasks(List<T> tasks) {
        return String.join(", ", tasks.stream().map(t -> (CharSequence)t.toString()).filter(t -> t.length() > 0)::iterator);
    }

    /**
     * 表示批量执行群集状态更新任务的结果
     * @param <T> the type of the cluster state update task
     */
    class ClusterTasksResult<T> {
        @Nullable
        public final ClusterState resultingState;
        public final Map<T, TaskResult> executionResults;

        /**
         * 使用任务及其执行结果之间的对应关系构造执行结果实例
         * @param resultingState the resulting cluster state
         * @param executionResults the correspondence between tasks and their outcome
         */
        ClusterTasksResult(ClusterState resultingState, Map<T, TaskResult> executionResults) {
            this.resultingState = resultingState;
            this.executionResults = executionResults;
        }

        public static <T> Builder<T> builder() {
            return new Builder<>();
        }

        public static class Builder<T> {
            private final Map<T, TaskResult> executionResults = new IdentityHashMap<>();

            public Builder<T> success(T task) {
                return result(task, TaskResult.success());
            }

            public Builder<T> successes(Iterable<T> tasks) {
                for (T task : tasks) {
                    success(task);
                }
                return this;
            }

            public Builder<T> failure(T task, Exception e) {
                return result(task, TaskResult.failure(e));
            }

            public Builder<T> failures(Iterable<T> tasks, Exception e) {
                for (T task : tasks) {
                    failure(task, e);
                }
                return this;
            }

            private Builder<T> result(T task, TaskResult executionResult) {
                TaskResult existing = executionResults.put(task, executionResult);
                assert existing == null : task + " already has result " + existing;
                return this;
            }

            public ClusterTasksResult<T> build(ClusterState resultingState) {
                return new ClusterTasksResult<>(resultingState, executionResults);
            }

            ClusterTasksResult<T> build(ClusterTasksResult<T> result, ClusterState previousState) {
                return new ClusterTasksResult<>(result.resultingState == null ? previousState : result.resultingState,
                    executionResults);
            }
        }
    }

    final class TaskResult {
        private final Exception failure;

        private static final TaskResult SUCCESS = new TaskResult(null);

        public static TaskResult success() {
            return SUCCESS;
        }

        public static TaskResult failure(Exception failure) {
            return new TaskResult(failure);
        }

        private TaskResult(Exception failure) {
            this.failure = failure;
        }

        public boolean isSuccess() {
            return this == SUCCESS;
        }

        public Exception getFailure() {
            assert !isSuccess();
            return failure;
        }
    }
}
