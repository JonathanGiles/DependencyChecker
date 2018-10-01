package net.jonathangiles.tool.maven.dependencies.report;

import net.jonathangiles.tool.maven.dependencies.model.Dependency;
import net.jonathangiles.tool.maven.dependencies.model.DependencyChain;
import net.jonathangiles.tool.maven.dependencies.project.Project;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PlainTextReport implements Report {

    public void report(List<Project> projects, List<Dependency> problems) {
        System.out.println("Problems:");
        System.out.println("=====================================================");
        problems.stream()
                .filter(Dependency::isProblemDependency)
                .peek(dep -> System.out.println("Issues for " + dep.getGA() + ":"))
                .forEach(dependency -> dependency.getVersions().forEach(version -> {
                    Map<Project, List<DependencyChain>> dependencyVersions = dependency.getDependenciesOnVersion(version);
                    System.out.println("   For version " + version + ", there are " + dependencyVersions.size() + " projects that depend on this library:");

                    dependencyVersions.entrySet().stream()
                            .peek(e -> System.out.println("    - " + e.getKey().getFullProjectName()))
                            .forEach(e -> {
                                e.getValue().stream()
                                    .filter(DependencyChain::hasDependencyChain)
                                    .sorted(Comparator.comparingInt(DependencyChain::getDependencyChainSize).reversed())
                                    .limit(1)
                                    .forEach(depVersion -> System.out.println("       - Dependency chain: " + depVersion.getDependencyChain().stream().collect(Collectors.joining(" -> "))));
                            });
                }));
    }
}
