package net.jonathangiles.tool.maven.dependencies.model;

import org.jboss.shrinkwrap.resolver.api.maven.coordinate.MavenCoordinate;

public class DependencyManagement {
    private final String groupId;
    private final String artifactId;
    private final String version;
    private State state;

    public static DependencyManagement fromMaven(MavenCoordinate coordinate) {
        return new DependencyManagement(coordinate.getGroupId(), coordinate.getArtifactId(), coordinate.getVersion(), State.UNKNOWN);
    }

    public static DependencyManagement fromUnmanagedDependency(Dependency dep) {
        return new DependencyManagement(dep.getGroupId(), dep.getArtifactId(), null, State.UNMANAGED);
    }

    private DependencyManagement(String groupId, String artifactId, String version, State state) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.state = state;
    }

    public String getGA() {
        return groupId + ":" + artifactId;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public String getVersion() {
        return version;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public enum State {
        CONSISTENT("All libraries are using the managed version of this dependency"),
        INCONSISTENT("One or more libraries are inconsistent with the managed version of this dependency"),
        UNKNOWN("Unknown"),
        UNMANAGED("One or more libraries use this dependency, but it is not declared in the dependencyManagement section of the project"),
        UNUSED("No libraries use this managed dependency");

        private final String description;

        State(String description) {
            this.description = description;
        }

        @Override
        public String toString() {
            return description;
        }
    }
}
