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

package org.elasticsearch.script;

import org.elasticsearch.common.settings.ClusterSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.plugins.ScriptPlugin;
import org.elasticsearch.search.aggregations.pipeline.MovingFunctionScript;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 管理构建 {@link ScriptService}。
 */
public class ScriptModule {

    public static final Map<String, ScriptContext<?>> CORE_CONTEXTS;
    static {
        CORE_CONTEXTS = Stream.of(
            FieldScript.CONTEXT,
            AggregationScript.CONTEXT,
            ScoreScript.CONTEXT,
            NumberSortScript.CONTEXT,
            StringSortScript.CONTEXT,
            TermsSetQueryScript.CONTEXT,
            UpdateScript.CONTEXT,
            BucketAggregationScript.CONTEXT,
            BucketAggregationSelectorScript.CONTEXT,
            SignificantTermsHeuristicScoreScript.CONTEXT,
            IngestScript.CONTEXT,
            IngestConditionalScript.CONTEXT,
            FilterScript.CONTEXT,
            SimilarityScript.CONTEXT,
            SimilarityWeightScript.CONTEXT,
            TemplateScript.CONTEXT,
            MovingFunctionScript.CONTEXT,
            ScriptedMetricAggContexts.InitScript.CONTEXT,
            ScriptedMetricAggContexts.MapScript.CONTEXT,
            ScriptedMetricAggContexts.CombineScript.CONTEXT,
            ScriptedMetricAggContexts.ReduceScript.CONTEXT
        ).collect(Collectors.toMap(c -> c.name, Function.identity()));
    }

    private final ScriptService scriptService;

    public ScriptModule(Settings settings, List<ScriptPlugin> scriptPlugins) {
        Map<String, ScriptEngine> engines = new HashMap<>();
        Map<String, ScriptContext<?>> contexts = new HashMap<>(CORE_CONTEXTS);
        for (ScriptPlugin plugin : scriptPlugins) {
            for (ScriptContext<?> context : plugin.getContexts()) {
                ScriptContext<?> oldContext = contexts.put(context.name, context);
                if (oldContext != null) {
                    throw new IllegalArgumentException("Context name [" + context.name + "] defined twice");
                }
            }
        }
        for (ScriptPlugin plugin : scriptPlugins) {
            ScriptEngine engine = plugin.getScriptEngine(settings, contexts.values());
            if (engine != null) {
                ScriptEngine existing = engines.put(engine.getType(), engine);
                if (existing != null) {
                    throw new IllegalArgumentException("scripting language [" + engine.getType() + "] defined for engine [" +
                        existing.getClass().getName() + "] and [" + engine.getClass().getName());
                }
            }
        }
        scriptService = new ScriptService(settings, Collections.unmodifiableMap(engines), Collections.unmodifiableMap(contexts));
    }

    /**
     * 负责管理脚本的服务。
     */
    public ScriptService getScriptService() {
        return scriptService;
    }

    /**
     * 允许脚本服务在群集设置上注册任何设置更新处理程序
     */
    public void registerClusterSettingsListeners(ClusterSettings clusterSettings) {
        scriptService.registerClusterSettingsListeners(clusterSettings);
    }
}
