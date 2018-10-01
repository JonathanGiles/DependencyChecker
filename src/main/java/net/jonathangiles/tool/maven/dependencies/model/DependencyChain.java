package net.jonathangiles.tool.maven.dependencies.model;

import org.jboss.shrinkwrap.resolver.api.maven.MavenArtifactInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DependencyChain {

    private final List<String> dependencyChain;

    DependencyChain(List<MavenArtifactInfo> depChain) {
        dependencyChain = depChain.stream()
                .map(MavenArtifactInfo::getCoordinate)
                .map(coordinate -> coordinate.getGroupId() + ":" + coordinate.getArtifactId() + ":" + coordinate.getVersion())
                .collect(Collectors.toList());
    }

    public int getDependencyChainSize() { return dependencyChain.size(); }

    public boolean hasDependencyChain() {
        return dependencyChain.size() > 1;
    }

    public List<String> getDependencyChain() {
        return dependencyChain;
    }

    @Override
    public String toString() {
        return "DependencyVersion{" +
                "dependencyChain=" + (dependencyChain.isEmpty() ? "<no chain>" : dependencyChain) +
                '}';
    }
}
