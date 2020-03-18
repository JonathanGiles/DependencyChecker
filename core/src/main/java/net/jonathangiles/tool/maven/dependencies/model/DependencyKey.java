package net.jonathangiles.tool.maven.dependencies.model;

import org.jboss.shrinkwrap.resolver.api.maven.ScopeType;

import java.util.Objects;

public class DependencyKey {
    private final String mavenGA;
    private final ScopeType mavenScope;

    public DependencyKey(final String mavenGA, final ScopeType mavenScope) {
        this.mavenGA = mavenGA;
        this.mavenScope = mavenScope;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final DependencyKey that = (DependencyKey) o;
        return Objects.equals(mavenGA, that.mavenGA) &&
                       mavenScope == that.mavenScope;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mavenGA, mavenScope);
    }
}
