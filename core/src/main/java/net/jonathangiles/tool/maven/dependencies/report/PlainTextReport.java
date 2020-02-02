package net.jonathangiles.tool.maven.dependencies.report;

import net.jonathangiles.tool.maven.dependencies.model.Dependency;
import net.jonathangiles.tool.maven.dependencies.model.DependencyChain;
import net.jonathangiles.tool.maven.dependencies.model.DependencyManagement;
import net.jonathangiles.tool.maven.dependencies.project.Project;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class PlainTextReport implements Reporter {
    private final StringBuilder sb;

    public PlainTextReport() {
        this.sb = new StringBuilder();
    }

    @Override
    public String getName() {
        return "plain-text";
    }

    @Override
    public void report(List<Project> projects, List<Dependency> problems, Collection<DependencyManagement> dependencyManagement, File outDir, String outFileName) {
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

        if (!dependencyManagement.isEmpty()) {
            out("\r\n");
            out("Dependency Management Report:");
            out("=====================================================");
            Collection<DependencyManagement> unmanagedDeps = dependencyManagement.stream()
                    .sorted(Comparator.comparing(DependencyManagement::getGA))
                    .map(d -> {
                        if (d.getState() != DependencyManagement.State.UNMANAGED) {
                            out("\r\n" + d.getGA() + ":" + d.getVersion());
                            out(d.getState().toString());
                            return null;
                        } else {
                            return d;
                        }
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            if (!unmanagedDeps.isEmpty()) {
                out("\r\n");
                out("Unmanaged Dependencies:");
                out("=====================================================");
                for (DependencyManagement d : unmanagedDeps) {
                    out("\r\n" + d.getGA());
                    out(d.getState().toString());
                }
            }
        }

        // write out to the output file
        File outFile = new File(outDir, outFileName + ".txt");
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
