package net.jonathangiles.tool.maven.dependencies.report;

import net.jonathangiles.tool.maven.dependencies.model.Dependency;
import net.jonathangiles.tool.maven.dependencies.model.DependencyManagement;
import net.jonathangiles.tool.maven.dependencies.project.Project;

import java.io.File;
import java.util.Collection;
import java.util.List;

public interface Reporter {

    /**
     * The name of the reporter, for use by the user of the app to request that this reporter be run.
     *
     * @return The name of the reporter.
     */
    String getName();

    /**
     * Generate the report based on the provided projects list, problems list, and the specified output directory and
     * output filename (which is missing a specific file extension - this should be added by the concrete subclass).
     */
    void report(List<Project> projects, List<Dependency> problems, Collection<DependencyManagement> dependencyManagement, File outDir, String outFileName);
}
