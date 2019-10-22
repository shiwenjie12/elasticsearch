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

package org.elasticsearch.index.query.functionscore;

import org.apache.lucene.search.Explanation;

/**
 * 实现此接口以提供在距离上执行的衰减功能。例如，这可能是指数下降，三角函数或类似的东西。
 * 例如，{@link GaussDecayFunctionBuilder}使用它。
 * 
 */
public interface DecayFunction {

    // 评估
    double evaluate(double value, double scale);

    Explanation explainFunction(String valueString, double value, double scale);

    /**
     * The final scale parameter is computed from the scale parameter given by
     * the user and a value. This value is the value that the decay function
     * should compute if document distance and user defined scale equal. The
     * scale parameter for the function must be adjusted accordingly in this
     * function
     * 最终比例参数根据用户给出的比例参数和值计算。如果文档距离和用户定义的比例相等，
     * 则此值是衰减函数应计算的值。必须在此功能中相应调整功能的比例参数
     * @param scale
     *            用户给出的原始比例值
     * @param decay
     *            一旦距离达到这个尺度，衰减函数应该采取的值
     * */
    double processScale(double scale, double decay);
}
