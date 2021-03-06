/*
 * Copyright 2007-2008 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.api.internal.artifacts.dependencies;

import org.gradle.api.artifacts.Dependency;
import org.gradle.api.internal.artifacts.DependencyResolveContext;
import org.gradle.api.internal.artifacts.ResolvableDependency;

public abstract class AbstractDependency implements ResolvableDependency, Dependency {
    protected void copyTo(AbstractDependency target) {
    }

    public void resolve(DependencyResolveContext context) {
    }

    @Override
    public int hashCode() {
        int result = getGroup() != null ? getGroup().hashCode() : 0;
        result = 31 * result + getName().hashCode();
        result = 31 * result + (getVersionConstraint() != null ? getVersionConstraint().hashCode() : 0);
        return result;
    }
}
