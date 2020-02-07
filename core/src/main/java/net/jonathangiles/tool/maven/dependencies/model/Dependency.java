package net.jonathangiles.tool.maven.dependencies.model;

import net.jonathangiles.tool.maven.dependencies.project.Project;
import org.jboss.shrinkwrap.resolver.api.maven.MavenArtifactInfo;
import org.jboss.shrinkwrap.resolver.api.maven.ScopeType;

import java.util.*;
import java.util.stream.Collectors;

import static net.jonathangiles.tool.maven.dependencies.misc.Util.getLatestVersionInMavenCentral;

/**
 * This class contains a groupId and artifactId (from Maven) representing a single dependency from one of the
 * projects being scanned (either directly or transitively).
 */
public class Dependency {
    private final String groupId;
    private final String artifactId;

    private boolean hasCompileOrRuntimeScope;

    // map version string to all dependencies on that version.
    private final Map<Version, Map<Project, List<DependencyChain>>> dependenciesOnVersion;

    public Dependency(MavenArtifactInfo artifact) {
        this.groupId = artifact.getCoordinate().getGroupId();
        this.artifactId = artifact.getCoordinate().getArtifactId();
        this.dependenciesOnVersion = new TreeMap<>(Comparator.reverseOrder());
        this.hasCompileOrRuntimeScope = isCompileOrRuntimeScope(artifact);
    }

    public void addArtifact(Project project, MavenArtifactInfo artifact, List<MavenArtifactInfo> depChain) {
        Version version = Version.build(artifact.getCoordinate().getVersion());

        hasCompileOrRuntimeScope = hasCompileOrRuntimeScope || isCompileOrRuntimeScope(artifact);
        dependenciesOnVersion
                .computeIfAbsent(version, v -> new HashMap<>())
                .computeIfAbsent(project, p -> new ArrayList<>())
                .add(new DependencyChain(depChain));
    }

    private boolean isCompileOrRuntimeScope(MavenArtifactInfo artifact) {
        return artifact.getScope() == ScopeType.COMPILE || artifact.getScope() == ScopeType.RUNTIME;
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

    public Set<Version> getVersions() {
        return dependenciesOnVersion.keySet();
    }

    public Map<Project, List<DependencyChain>> getDependenciesOnVersion(Version version) {
        return dependenciesOnVersion.get(version);
    }

    public boolean anyDependenciesOnLatestRelease() {
        final Version latestReleasedVersionD2 = getLatestVersionInMavenCentral(getGA(), false);
        return dependenciesOnVersion.containsKey(latestReleasedVersionD2);
    }

    /**
     * Returns true when there is more than one version being depended on.
     *
     * @return True if this dependency has versioning issues.
     */
    public boolean isProblemDependency() {
        return dependenciesOnVersion.size() > 1;
    }

    public boolean hasCompileOrRuntimeScope() {
        return hasCompileOrRuntimeScope;
    }

    @Override
    public String toString() {
        return "Dependency{" +
                "groupId='" + groupId + '\'' +
                ", artifactId='" + artifactId + '\'' +
                ", dependenciesOnVersion=\r\n" +
                    dependenciesOnVersion.entrySet().stream()
                            .map(e -> {
                                Map<Project, List<DependencyChain>> map = e.getValue();
                                return "Version: " + e.getKey() + " -> \r\n" + map.entrySet().stream().map(e1 -> "  " + e1.getKey() + " -> " + e1.getValue()).collect(Collectors.joining("\r\n"));
                            }).collect(Collectors.joining("\r\n")) +
                '}';
    }
}
