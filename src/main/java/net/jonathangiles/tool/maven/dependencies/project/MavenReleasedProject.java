package net.jonathangiles.tool.maven.dependencies.project;

import net.jonathangiles.tool.maven.dependencies.misc.Util;
import net.jonathangiles.tool.maven.dependencies.model.Version;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;

import java.util.ArrayList;
import java.util.List;

public class MavenReleasedProject implements Project {

    private final String groupId;
    private final String artifactId;
    private final Version version;

    private final List<WebProject> modules;

    public MavenReleasedProject(String groupId, String artifactId, String version) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version != null ? Version.build(version) : Util.getLatestVersionInMavenCentral(groupId, artifactId, false);

        if (this.version == Version.UNKNOWN) {
            System.err.println("failure on " + groupId + ":" + artifactId + ":" + version);
            System.exit(-1);
        }

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
    public Project getParent() {
        return null;
    }

    @Override
    public List<String> getPomUrls() {
        List<String> urls = new ArrayList<>();

        String url;

        if (version.isSnapshot()) {
            url = "https://oss.sonatype.org/content/repositories/snapshots/";
        } else {
            url = "http://central.maven.org/maven2/";
        }

        url += groupId.replace(".", "/")
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
