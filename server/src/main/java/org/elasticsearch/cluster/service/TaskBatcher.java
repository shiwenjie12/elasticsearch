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

package org.elasticsearch.cluster.service;

import org.apache.logging.log4j.Logger;
import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.Priority;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.util.concurrent.EsRejectedExecutionException;
import org.elasticsearch.common.util.concurrent.PrioritizedEsThreadPoolExecutor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 批量支持{@link PrioritizedEsThreadPoolExecutor}
 * 共享相同批处理密钥的任务是批处理的（请参阅{@link BatchedTask＃batchingKey}）
 */
public abstract class TaskBatcher {

    private final Logger logger;
    private final PrioritizedEsThreadPoolExecutor threadExecutor;
    // package visible for tests
    final Map<Object, LinkedHashSet<BatchedTask>> tasksPerBatchingKey = new HashMap<>();

    public TaskBatcher(Logger logger, PrioritizedEsThreadPoolExecutor threadExecutor) {
        this.logger = logger;
        this.threadExecutor = threadExecutor;
    }

    public void submitTasks(List<? extends BatchedTask> tasks, @Nullable TimeValue timeout) throws EsRejectedExecutionException {
        if (tasks.isEmpty()) {
            return;
        }
        final BatchedTask firstTask = tasks.get(0);
        // 判断BatchedKey是否相同
        assert tasks.stream().allMatch(t -> t.batchingKey == firstTask.batchingKey) :
            "tasks submitted in a batch should share the same batching key: " + tasks;
        // 转换为身份映射以根据任务标识检查重复
        final Map<Object, BatchedTask> tasksIdentity = tasks.stream().collect(Collectors.toMap(
            BatchedTask::getTask,
            Function.identity(),
            (a, b) -> { throw new IllegalStateException("cannot add duplicate task: " + a); },
            IdentityHashMap::new));

        synchronized (tasksPerBatchingKey) {
            LinkedHashSet<BatchedTask> existingTasks = tasksPerBatchingKey.computeIfAbsent(firstTask.batchingKey,
                k -> new LinkedHashSet<>(tasks.size()));
            for (BatchedTask existing : existingTasks) {
                // 检查同一批处理密钥不会有两个具有相同标识的任务
                BatchedTask duplicateTask = tasksIdentity.get(existing.getTask());
                if (duplicateTask != null) {
                    throw new IllegalStateException("task [" + duplicateTask.describeTasks(
                        Collections.singletonList(existing)) + "] with source [" + duplicateTask.source + "] is already queued");
                }
            }
            existingTasks.addAll(tasks);
        }

        if (timeout != null) {
            threadExecutor.execute(firstTask, timeout, () -> onTimeoutInternal(tasks, timeout));
        } else {
            threadExecutor.execute(firstTask);
        }
    }

    private void onTimeoutInternal(List<? extends BatchedTask> tasks, TimeValue timeout) {
        final ArrayList<BatchedTask> toRemove = new ArrayList<>();
        for (BatchedTask task : tasks) {
            if (task.processed.getAndSet(true) == false) {
                logger.debug("task [{}] timed out after [{}]", task.source, timeout);
                toRemove.add(task);
            }
        }
        if (toRemove.isEmpty() == false) {
            BatchedTask firstTask = toRemove.get(0);
            Object batchingKey = firstTask.batchingKey;
            assert tasks.stream().allMatch(t -> t.batchingKey == batchingKey) :
                "tasks submitted in a batch should share the same batching key: " + tasks;
            synchronized (tasksPerBatchingKey) {
                LinkedHashSet<BatchedTask> existingTasks = tasksPerBatchingKey.get(batchingKey);
                if (existingTasks != null) {
                    existingTasks.removeAll(toRemove);
                    if (existingTasks.isEmpty()) {
                        tasksPerBatchingKey.remove(batchingKey);
                    }
                }
            }
            onTimeout(toRemove, timeout);
        }
    }

    /**
     * Action to be implemented by the specific batching implementation.
     * All tasks have the same batching key.
     */
    protected abstract void onTimeout(List<? extends BatchedTask> tasks, TimeValue timeout);

    void runIfNotProcessed(BatchedTask updateTask) {
        // if this task is already processed, it shouldn't execute other tasks with same batching key that arrived later,
        // to give other tasks with different batching key a chance to execute.
        // 如果此任务已经处理，则不应执行具有相同批处理密钥的其他任务，以便稍后到达，以使具有不同批处理密钥的其他任务有机会执行。
        if (updateTask.processed.get() == false) {
            // 需要执行的任务
            final List<BatchedTask> toExecute = new ArrayList<>();
            // 按照源区分的任务
            final Map<String, List<BatchedTask>> processTasksBySource = new HashMap<>();
            synchronized (tasksPerBatchingKey) {
                LinkedHashSet<BatchedTask> pending = tasksPerBatchingKey.remove(updateTask.batchingKey);
                if (pending != null) {
                    for (BatchedTask task : pending) {
                        if (task.processed.getAndSet(true) == false) {
                            logger.trace("will process {}", task);
                            toExecute.add(task);
                            processTasksBySource.computeIfAbsent(task.source, s -> new ArrayList<>()).add(task);
                        } else {
                            logger.trace("skipping {}, already processed", task);
                        }
                    }
                }
            }

            if (toExecute.isEmpty() == false) {
                // 任务概要
                final String tasksSummary = processTasksBySource.entrySet().stream().map(entry -> {
                    String tasks = updateTask.describeTasks(entry.getValue());
                    return tasks.isEmpty() ? entry.getKey() : entry.getKey() + "[" + tasks + "]";
                }).reduce((s1, s2) -> s1 + ", " + s2).orElse("");

                run(updateTask.batchingKey, toExecute, tasksSummary);
            }
        }
    }

    /**
     * 特定批处理实现要执行的操作所有任务都具有给定的批处理键。
     */
    protected abstract void run(Object batchingKey, List<? extends BatchedTask> tasks, String tasksSummary);

    /**
     * 表示支持批处理的可运行任务。
     * TaskBatcher的实现者可以将其子类化以向任务添加有效负载。
     */
    protected abstract class BatchedTask extends SourcePrioritizedRunnable {
        /**
         * 该任务是否已经处理完毕
         */
        protected final AtomicBoolean processed = new AtomicBoolean();

        /**
         * 用作批处理密钥的对象
         */
        protected final Object batchingKey;

        /**
         * 包装的任务对象
         */
        protected final Object task;

        protected BatchedTask(Priority priority, String source, Object batchingKey, Object task) {
            super(priority, source);
            this.batchingKey = batchingKey;
            this.task = task;
        }

        @Override
        public void run() {
            runIfNotProcessed(this);
        }

        @Override
        public String toString() {
            String taskDescription = describeTasks(Collections.singletonList(this));
            if (taskDescription.isEmpty()) {
                return "[" + source + "]";
            } else {
                return "[" + source + "[" + taskDescription + "]]";
            }
        }

        public abstract String describeTasks(List<? extends BatchedTask> tasks);

        public Object getTask() {
            return task;
        }
    }
}
