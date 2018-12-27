/*
 * Copyright (C) 2006 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.elasticsearch.common.inject;

/**
 * 我们正在进行的阶段
 *
 * @author crazybob@google.com (Bob Lee)
 */
public enum Stage {

    /**
     * We're running in a tool (an IDE plugin for example). We need binding meta data but not a
     * functioning Injector. Do not inject members of instances. Do not load eager singletons. Do as
     * little as possible so our tools run nice and snappy. Injectors created in this stage cannot
     * be used to satisfy injections.
     * 我们正在运行一个工具（例如IDE插件）。 我们需要绑定元数据，但不需要功能正常的注入器。 不要注入实例成员。 不要加载渴望的单例。
     * 尽量少做，所以我们的工具运行得很好而且活泼。 在此阶段创建的注射器不能用于满足注射。
     */
    TOOL,

    /**
     * 我们希望以运行时性能和一些前期错误检查为代价来快速启动。
     */
    DEVELOPMENT,

    /**
     * 我们希望尽早发现错误，并预先考虑性能问题。
     */
    PRODUCTION
}
