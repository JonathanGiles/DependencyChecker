package net.jonathangiles.tool.maven.dependencies.report;

import net.jonathangiles.tool.maven.dependencies.model.Dependency;
import net.jonathangiles.tool.maven.dependencies.model.DependencyChain;
import net.jonathangiles.tool.maven.dependencies.project.Project;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PlainTextReport implements Report {
    private final File outFile;
    private final StringBuilder sb;

    public PlainTextReport(File outFile) {
        this.outFile = outFile;
        this.sb = new StringBuilder();
    }

    public void report(List<Project> projects, List<Dependency> problems) {
        out("Dependency Issues Report:");
        out("=====================================================");
        problems.stream()
                .filter(Dependency::isProblemDependency)
                .peek(dep -> out("\r\nIssues for " + dep.getGA() + ":"))
                .forEach(dependency -> dependency.getVersions().forEach(version -> {
                    Map<Project, List<DependencyChain>> dependencyVersions = dependency.getDependenciesOnVersion(version);
                    out("   For version " + version + ", there are " + dependencyVersions.size() + " projects that depend on this library:");

                    dependencyVersions.entrySet().stream()
                            .peek(e -> out("    - " + e.getKey().getFullProjectName()))
                            .forEach(e -> {
                                e.getValue().stream()
                                    .filter(DependencyChain::hasDependencyChain)
                                    .sorted(Comparator.comparingInt(DependencyChain::getDependencyChainSize).reversed())
                                    .limit(1)
                                    .forEach(depVersion -> out("       - Dependency chain: " + depVersion.getDependencyChain().stream().collect(Collectors.joining(" -> "))));
                            });
                }));

        // write out to the output file
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outFile))) {
            writer.write(sb.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("Plain text report written to " + outFile);
    }

    private void out(String s) {
        sb.append(s);
        sb.append("\r\n");
    }
}
