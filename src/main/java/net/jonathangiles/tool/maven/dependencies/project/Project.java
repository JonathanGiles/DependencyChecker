package net.jonathangiles.tool.maven.dependencies.project;

import java.util.List;

public interface Project {

    String getProjectName();

    String getFullProjectName();

    List<String> getPomUrls();

    List<WebProject> getModules();
}
