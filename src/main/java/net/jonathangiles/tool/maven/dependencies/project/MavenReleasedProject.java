package net.jonathangiles.tool.maven.dependencies.project;

import org.jboss.shrinkwrap.resolver.api.maven.Maven;

import java.util.ArrayList;
import java.util.List;

public class MavenReleasedProject implements Project {

    private final String groupId;
    private final String artifactId;
    private final String version;



    private final List<WebProject> modules;

    public MavenReleasedProject(String groupId, String artifactId, String version) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.modules = new ArrayList<>();
    }

    @Override
    public String getProjectName() {
        return groupId + ":" + artifactId;
    }

    @Override
    public String getFullProjectName() {
        return getProjectName() + ":" + version;
    }

    @Override
    public List<String> getPomUrls() {
        List<String> urls = new ArrayList<>();

        String url = "http://central.maven.org/maven2/"
                + groupId.replace(".", "/")
                + "/"
                + artifactId.replace(".", "/")
                + "/"
                + version
                + "/"
                + artifactId + "-" + version + ".pom";
        urls.add(url);

        return urls;
    }

    @Override
    public List<WebProject> getModules() {
        return modules;
    }

    @Override
    public String toString() {
        return getFullProjectName();
    }
}
