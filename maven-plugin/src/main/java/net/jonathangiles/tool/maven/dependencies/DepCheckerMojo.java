package net.jonathangiles.tool.maven.dependencies;

import net.jonathangiles.tool.maven.dependencies.misc.Result;
import net.jonathangiles.tool.maven.dependencies.project.Project;
import net.jonathangiles.tool.maven.dependencies.project.WebProject;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.util.List;
import java.util.Optional;

@Mojo(name = "check")
public class DepCheckerMojo extends AbstractMojo {
    @Parameter(defaultValue="${project}", readonly=true, required=true)
    private MavenProject project;

    @Parameter(property = "check.reporters", defaultValue = "")
    private String reporters;

    @Parameter
    private boolean analyseBom;

    @Parameter
    private boolean dependencyManagement;

    @Parameter
    private boolean showAll;

    @Parameter
    private boolean failOnDependencyConflict;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        Main main = new Main() {
            @Override
            protected File[] loadInputs() {
                return new File[] { project.getFile() };
            }
        };
        main.setReporters(reporters);
        main.setShowAll(showAll);
        main.setAnalyseBom(analyseBom);
        main.setDependencyManagement(dependencyManagement);

        getLog().info("Hello: " + project.getFile());
        getLog().info("Running with configuration: [ " +
                              "reporters='" + reporters + '\'' +
                              ", analyseBom=" + analyseBom +
                              ", dependencyManagement=" + dependencyManagement +
                              ", showAll=" + showAll +
                              ", failOnDependencyConflict=" + failOnDependencyConflict +
                              " ]");

        Optional<Result> resultOptional = main.run();

        if (resultOptional.isPresent()) {
            Result result = resultOptional.get();
            if (failOnDependencyConflict && result == Result.DEPENDENCY_VERSION_CONFLICTS) {
                throw new MojoFailureException("Dependency conflicts found. Refer to generated report file for more details.");
            }
        }
    }
}
