/*
 * Copyright 2007 the original author or authors.
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

package org.gradle.api.tasks.util;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import groovy.lang.Closure;
import org.gradle.api.Action;
import org.gradle.api.Incubating;
import org.gradle.api.file.FileTreeElement;
import org.gradle.api.specs.Spec;
import org.gradle.api.specs.Specs;
import org.gradle.api.tasks.AntBuilderAware;
import org.gradle.api.tasks.util.internal.PatternSetAntBuilderDelegate;
import org.gradle.api.tasks.util.internal.PatternSpecFactory;
import org.gradle.internal.typeconversion.NotationParser;
import org.gradle.internal.typeconversion.NotationParserBuilder;

import java.util.Set;

/**
 * Standalone implementation of {@link PatternFilterable}.
 */
public class PatternSet implements AntBuilderAware, PatternFilterable {

    private static final NotationParser<Object, String> PARSER = NotationParserBuilder.toType(String.class).fromCharSequence().toComposite();
    private final PatternSpecFactory patternSpecFactory;

    private ImmutableSet<String> includes = ImmutableSet.of();
    private ImmutableSet<String> excludes = ImmutableSet.of();
    private ImmutableSet<Spec<FileTreeElement>> includeSpecs = ImmutableSet.of();
    private ImmutableSet<Spec<FileTreeElement>> excludeSpecs = ImmutableSet.of();
    private boolean caseSensitive = true;

    public PatternSet() {
        this(PatternSpecFactory.INSTANCE);
    }

    @Incubating
    protected PatternSet(PatternSet patternSet) {
        this(patternSet.patternSpecFactory);
    }

    @Incubating
    protected PatternSet(PatternSpecFactory patternSpecFactory) {
        this.patternSpecFactory = patternSpecFactory;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PatternSet)) {
            return false;
        }

        PatternSet that = (PatternSet) o;

        if (caseSensitive != that.caseSensitive) {
            return false;
        }
        if (!excludeSpecs.equals(that.excludeSpecs)) {
            return false;
        }
        if (!excludes.equals(that.excludes)) {
            return false;
        }
        if (!includeSpecs.equals(that.includeSpecs)) {
            return false;
        }
        if (!includes.equals(that.includes)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = includes.hashCode();
        result = 31 * result + excludes.hashCode();
        result = 31 * result + includeSpecs.hashCode();
        result = 31 * result + excludeSpecs.hashCode();
        result = 31 * result + (caseSensitive ? 1 : 0);
        return result;
    }

    public PatternSet copyFrom(PatternFilterable sourcePattern) {
        return doCopyFrom((PatternSet) sourcePattern);
    }

    protected PatternSet doCopyFrom(PatternSet from) {
        caseSensitive = from.caseSensitive;

        if (from instanceof IntersectionPatternSet) {
            includes = ImmutableSet.of();
            excludes = ImmutableSet.of();
            excludeSpecs = ImmutableSet.of();

            PatternSet other = ((IntersectionPatternSet) from).other;
            PatternSet otherCopy = new PatternSet(other).copyFrom(other);
            PatternSet intersectCopy = new IntersectionPatternSet(otherCopy);
            intersectCopy.include(from.includes);
            intersectCopy.exclude(from.excludes);
            intersectCopy.includeSpecs(from.includeSpecs);
            intersectCopy.excludeSpecs(from.excludeSpecs);
            includeSpecs = ImmutableSet.of(intersectCopy.getAsSpec());
        } else {
            includes = from.includes;
            excludes = from.excludes;
            includeSpecs = from.includeSpecs;
            excludeSpecs = from.excludeSpecs;
        }

        return this;
    }

    public PatternSet intersect() {
        if(isEmpty()) {
            return new PatternSet(this.patternSpecFactory);
        } else {
            return new IntersectionPatternSet(this);
        }
    }

    /**
     * The PatternSet is considered empty when no includes or excludes have been added.
     *
     * The Spec returned by getAsSpec method only contains the default excludes patterns
     * in this case.
     *
     * @return true when no includes or excludes have been added to this instance
     */
    public boolean isEmpty() {
        return getExcludes().isEmpty() && getIncludes().isEmpty() && getExcludeSpecs().isEmpty() && getIncludeSpecs().isEmpty();
    }

    private static class IntersectionPatternSet extends PatternSet {

        private final PatternSet other;

        public IntersectionPatternSet(PatternSet other) {
            super(other);
            this.other = other;
        }

        public Spec<FileTreeElement> getAsSpec() {
            return Specs.intersect(super.getAsSpec(), other.getAsSpec());
        }

        public Object addToAntBuilder(Object node, String childNodeName) {
            return PatternSetAntBuilderDelegate.and(node, new Action<Object>() {
                public void execute(Object andNode) {
                    IntersectionPatternSet.super.addToAntBuilder(andNode, null);
                    other.addToAntBuilder(andNode, null);
                }
            });
        }

        @Override
        public boolean isEmpty() {
            return other.isEmpty() && super.isEmpty();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            if (!super.equals(o)) {
                return false;
            }

            IntersectionPatternSet that = (IntersectionPatternSet) o;

            return other != null ? other.equals(that.other) : that.other == null;
        }

        @Override
        public int hashCode() {
            int result = super.hashCode();
            result = 31 * result + (other != null ? other.hashCode() : 0);
            return result;
        }
    }

    public Spec<FileTreeElement> getAsSpec() {
        return patternSpecFactory.createSpec(this);
    }

    public Spec<FileTreeElement> getAsIncludeSpec() {
        return patternSpecFactory.createIncludeSpec(this);
    }

    public Spec<FileTreeElement> getAsExcludeSpec() {
        return patternSpecFactory.createExcludeSpec(this);
    }

    public Set<String> getIncludes() {
        return includes;
    }

    public Set<Spec<FileTreeElement>> getIncludeSpecs() {
        return includeSpecs;
    }

    public PatternSet setIncludes(Iterable<String> includes) {
        this.includes = ImmutableSet.of();
        return include(includes);
    }

    public PatternSet include(String... includes) {
        this.includes = Sets.union(this.includes, ImmutableSet.copyOf(includes)).immutableCopy();
        return this;
    }

    public PatternSet include(Iterable includes) {
        ImmutableSet.Builder<String> builder = ImmutableSet.builder();
        for (Object include : includes) {
            builder.add(PARSER.parseNotation(include));
        }
        this.includes = Sets.union(this.includes, ImmutableSet.copyOf(builder.build())).immutableCopy();
        return this;
    }

    public PatternSet include(Spec<FileTreeElement> spec) {
        includeSpecs(ImmutableSet.of(spec));
        return this;
    }

    public Set<String> getExcludes() {
        return excludes;
    }

    public Set<Spec<FileTreeElement>> getExcludeSpecs() {
        return excludeSpecs;
    }

    public PatternSet setExcludes(Iterable<String> excludes) {
        this.excludes = ImmutableSet.of();
        return exclude(excludes);
    }


    public boolean isCaseSensitive() {
        return caseSensitive;
    }

    public void setCaseSensitive(boolean caseSensitive) {
        this.caseSensitive = caseSensitive;
    }

    /*
    This can't be called just include, because it has the same erasure as include(Iterable<String>).
     */
    public PatternSet includeSpecs(Iterable<Spec<FileTreeElement>> includeSpecs) {
        this.includeSpecs = Sets.union(this.includeSpecs, ImmutableSet.copyOf(includeSpecs)).immutableCopy();
        return this;
    }

    public PatternSet include(Closure closure) {
        include(Specs.<FileTreeElement>convertClosureToSpec(closure));
        return this;
    }

    public PatternSet exclude(String... excludes) {
        this.excludes = Sets.union(this.excludes, ImmutableSet.copyOf(excludes)).immutableCopy();
        return this;
    }

    public PatternSet exclude(Iterable excludes) {
        ImmutableSet.Builder<String> builder = ImmutableSet.builder();
        for (Object exclude : excludes) {
            builder.add(PARSER.parseNotation(exclude));
        }
        this.excludes = Sets.union(this.excludes, ImmutableSet.copyOf(builder.build())).immutableCopy();
        return this;
    }

    public PatternSet exclude(Spec<FileTreeElement> spec) {
        excludeSpecs(ImmutableSet.of(spec));
        return this;
    }

    public PatternSet excludeSpecs(Iterable<Spec<FileTreeElement>> excludes) {
        this.excludeSpecs = Sets.union(this.excludeSpecs, ImmutableSet.copyOf(excludes)).immutableCopy();
        return this;
    }

    public PatternSet exclude(Closure closure) {
        exclude(Specs.<FileTreeElement>convertClosureToSpec(closure));
        return this;
    }

    public Object addToAntBuilder(Object node, String childNodeName) {

        if (!includeSpecs.isEmpty() || !excludeSpecs.isEmpty()) {
            throw new UnsupportedOperationException("Cannot add include/exclude specs to Ant node. Only include/exclude patterns are currently supported.");
        }

        return new PatternSetAntBuilderDelegate(includes, excludes, caseSensitive).addToAntBuilder(node, childNodeName);
    }
}
