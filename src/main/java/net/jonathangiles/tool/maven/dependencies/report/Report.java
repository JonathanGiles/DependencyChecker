package net.jonathangiles.tool.maven.dependencies.report;

import net.jonathangiles.tool.maven.dependencies.model.Dependency;
import net.jonathangiles.tool.maven.dependencies.project.Project;

import java.util.List;

public interface Report {
    void report(List<Project> projects, List<Dependency> problems);
}
