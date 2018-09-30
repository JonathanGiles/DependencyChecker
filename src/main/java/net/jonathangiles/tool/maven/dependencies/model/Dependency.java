package net.jonathangiles.tool.maven.dependencies.model;

import net.jonathangiles.tool.maven.dependencies.project.Project;
import org.jboss.shrinkwrap.resolver.api.maven.MavenArtifactInfo;

import java.util.*;

public class Dependency {
    private final String groupId;
    private final String artifactId;

    // map version string to all dependencies on that version
    private final Map<String, Map<Project, DependencyVersionList>> dependenciesOnVersion;

    public Dependency(String groupId, String artifactId) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.dependenciesOnVersion = new HashMap<>();
    }

    public void addArtifact(Project project, MavenArtifactInfo artifact, List<MavenArtifactInfo> depChain) {
        String version = artifact.getCoordinate().getVersion();
        dependenciesOnVersion
                .computeIfAbsent(version, s -> new HashMap<>())
                .computeIfAbsent(project, p -> new DependencyVersionList())
                .add(new DependencyVersion(artifact, depChain));
    }

    public String getGA() {
        return getGroupId() + ":" + getArtifactId();
    }

    public String getGroupId() {
        return groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public Collection<String> getVersions() {
        return dependenciesOnVersion.keySet();
    }

    public Map<Project, DependencyVersionList> getDependenciesOnVersion(String version) {
        return dependenciesOnVersion.get(version);
    }

    /**
     * Returns true when there is more than one version being depended on.
     */
    public boolean isProblemDependency() {
        return dependenciesOnVersion.size() > 1;
    }

}
