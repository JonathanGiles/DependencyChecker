package net.jonathangiles.tool.maven.dependencies.model;

import org.jboss.shrinkwrap.resolver.api.maven.MavenArtifactInfo;

import java.util.List;
import java.util.stream.Collectors;

public class DependencyVersion {

    private transient final MavenArtifactInfo artifactInfo;

    private List<String> dependencyChain;

    public DependencyVersion(MavenArtifactInfo artifact, List<MavenArtifactInfo> depChain) {
        this.artifactInfo = artifact;

        // convert into string form up-front, because then this can be serialised out to json easily
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
}
