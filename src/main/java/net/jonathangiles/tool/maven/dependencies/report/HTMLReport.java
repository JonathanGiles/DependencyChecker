package net.jonathangiles.tool.maven.dependencies.report;

import net.jonathangiles.tool.maven.dependencies.model.Dependency;
import net.jonathangiles.tool.maven.dependencies.model.DependencyChain;
import net.jonathangiles.tool.maven.dependencies.model.DependencyManagement;
import net.jonathangiles.tool.maven.dependencies.model.Version;
import net.jonathangiles.tool.maven.dependencies.project.Project;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static net.jonathangiles.tool.maven.dependencies.misc.Util.*;

public class HTMLReport implements Reporter {
    private final StringBuilder sb;
    private boolean writeDependenciesHeader = true;

    public HTMLReport() {
        this.sb = new StringBuilder();
    }

    @Override
    public String getName() {
        return "html";
    }

    @Override
    public void report(List<Project> projects, List<Dependency> problems, Collection<DependencyManagement> dependencyManagement, File outDir, String outFileName) {
        out("<!DOCTYPE html>");
        out("<html>");
        out("  <head>");
        out("    <title>Dependency Issues Report</title>");
        out("    <meta charset=\"UTF-8\"/>");
        out("    <style>");

        // write out CSS inline
        try (BufferedReader r = Files.newBufferedReader(Paths.get(getClass().getResource("report.css").toURI()))) {
            r.lines().forEach(line -> out("      " + line));
        } catch (Exception e) {
            // no-op
        }

        out("    </style>");
        out("  </head>");
        out("  <body>");

        out("    <center>");

        // Summary table
        out("      <h1>Dependency Issues Report</h1>");
        out("      <p>This report scanned the <a href=\"#releases\">Maven releases</a> listed in the first table below, and reports on occasions where there are conflicting <a href=\"#dependencies\">dependency versions</a>.<br/>" +
                (dependencyManagement.isEmpty() ? "" : "<a href=\"#dependencyManagement\">DependencyManagement versions</a> have also been scanned and compared against the declared dependency versions in each release's libraries.<br/>") +
                "It is important to ensure the libraries analysed in the first table are correctly versioned.<br/>" +
                "Hover over dashed lines to see the dependency chain, if there is not a direct relationship between the dependency and the project.</p>");

        // summary of the projects we scanned
        out("    <a name=\"releases\"/>");
        printProjects(projects);

        out("    <br/>");
        out("    <hr/>");
        out("    <br/>");
        out("    <br/>");

        // results
        out("    <a name=\"dependencies\"/>");
        problems.stream()
                .sorted(Comparator.comparing(Dependency::getGA))
                .sorted(Comparator.comparing(Dependency::anyDependenciesOnLatestRelease))
                .sorted(Comparator.comparing(Dependency::isProblemDependency).reversed())
                .forEach(this::process);

        if (!dependencyManagement.isEmpty()) {
            out("    <br/>");
            out("    <hr/>");
            out("    <br/>");
            out("    <br/>");
            out("    <a name=\"dependencyManagement\"/>");
            printDependencyManagement(dependencyManagement);
        }

        out("      <small>Report generated using <a href=\"https://github.com/JonathanGiles/DependencyChecker\">DependencyChecker</a>, developed by <a href=\"http://jonathangiles.net\">Jonathan Giles</a></small>");
        out("    </center>");
        out("  </body>");
        out("</html>");

        // write out to the output file
        File outFile = new File(outDir, outFileName + ".html");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outFile))) {
            writer.write(sb.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("HTML report written to " + outFile);
    }

    private void printProjects(List<Project> projects) {
        out("    <table class=\"condensed\">");
        out("      <thead>");
        out("        <tr><th colspan=\"2\"><strong>Maven Releases Scanned for this Report</strong></th></tr>");
        out("        <tr><th>Library Analysed</th><th>Latest Released Version</th></tr>");
        out("      </thead>");
        out("      <tbody>");

        for (Project project : projects) {
            out("      <tr>");
            out("        <td>" + project.getFullProjectName() + "</td>");
            out("        <td>" + getLatestVersionInMavenCentral(project.getProjectName(), true) + "</td>");
            out("      </tr>");
        }

        out("      </tbody>");
        out("    </table>");
        out("    <br/>");
    }

    private void process(Dependency dependency) {
        final Version latestReleasedVersion = getLatestVersionInMavenCentral(dependency.getGA(), false);
        final Set<Version> versions = dependency.getVersions();
        final boolean noDependenciesOnLatestVersion = !versions.iterator().next().equals(latestReleasedVersion);
        final String headerClass = dependency.isProblemDependency()
                        ? "problem" : noDependenciesOnLatestVersion
                        ? "warning" : "success";

        out("    <a name=\"dep_" + getAnchor(dependency.getGA()) + "\"/>");
        out("    <table>");
        out("      <thead>");
        if (writeDependenciesHeader) {
            out("        <tr><th colspan=\"2\" class=\"" + headerClass + "\"><strong>Dependencies Discovered in Libraries</strong></th></tr>");
            writeDependenciesHeader = false;
        }
        out("        <tr><th colspan=\"2\" class=\"" + headerClass + "\"><strong>Dependency:</strong> " + dependency.getGA() +
                             "<br/>Latest Released Version: " + latestReleasedVersion + "</th></tr>");
        out("      </thead>");
        out("      <tbody>");



        if (noDependenciesOnLatestVersion) {
            out("      <tr>");
            out("        <td class=\"version\">" + latestReleasedVersion + "</td>");
            out("        <td><i>&lt;No dependencies on latest version&gt;</i></td>");
            out("      </tr>");
        }

        versions.forEach(version -> {
            out("      <tr>");
            out("        <td class=\"version\">" + version + "</td>");
            out("        <td>");

            dependency
                    .getDependenciesOnVersion(version)
                    .entrySet()
                    .stream()
                    .sorted(Comparator.comparing(e -> e.getKey().getFullProjectName()))
                    .forEach(e -> {
                        Optional<DependencyChain> dependencyChain = e.getValue()
                                .stream()
                                .filter(DependencyChain::hasDependencyChain)
                                .sorted(Comparator.comparingInt(DependencyChain::getDependencyChainSize).reversed())
                                .limit(1)
                                .findFirst();

                        if (dependencyChain.isPresent()) {
                            DependencyChain depChain = dependencyChain.get();
                            List<String> chainItems = depChain.getDependencyChain();
                            if (!chainItems.isEmpty()) {
                                chainItems.add(0, e.getKey().getFullProjectName());
                                chainItems.add(dependency.getGA() + ":" + version);

                                StringBuilder chainString = new StringBuilder();

                                chainString.append("<strong>Dependency Chain</strong><br/>");

                                for (int i = 0; i < chainItems.size(); i++) {
                                    for (int indent = 1; indent < 4 * i; indent++) {
                                        chainString.append("&nbsp;");
                                    }

                                    String gav = chainItems.get(i);

                                    int colonIndex = gav.lastIndexOf(":");
                                    String ga = colonIndex == -1 ? gav : gav.substring(0, colonIndex);
                                    Version latestVersion = getLatestVersionInMavenCentral(ga, false);

                                    if (latestVersion != null) {
                                        Version thisVersion = getVersionFromGAV(gav);
                                        if (thisVersion != null) {
                                            int diff = thisVersion.compareTo(latestVersion);

                                            switch (diff) {
                                                case 0:
                                                case 1:
                                                    chainString.append("<font class=\"success-chain\">");
                                                    break;
                                                case -1:
                                                    chainString.append("<font class=\"fail\">");
                                                    break;
                                            }
                                        }
                                    }

                                    chainString.append("- ");
                                    chainString.append(gav);


                                    if (latestVersion != null) {
                                        chainString.append(" (Latest version: " + latestVersion + ")</font>");
                                    }

                                    chainString.append("<br/>");
                                }

                                out("<div class=\"tooltip\">" + e.getKey().getFullProjectName() + "<span class=\"tooltiptext\">" + chainString.toString() + "</span></div><br/>");
                            }
                        } else {
                            out(e.getKey().getFullProjectName() + "<br/>");
                        }
                    });

            out("        </td>");
            out("      </tr>");
        });

        out("      </tbody>");
        out("    </table>");
        out("    <br/>");
    }

    private String getAnchor(String ga) {
        return ga.replace('.', '_').replace(":", "__");
    }

    private void printDependencyManagement(Collection<DependencyManagement> dependencyManagement) {
        out("    <table class=\"condensed\">");
        out("      <thead>");
        out("        <tr><th colspan=\"3\"><strong>DependencyManagement Versions in Project</strong></th></tr>");
        out("        <tr><th>Dependency</th><th>Managed Version</th><th>Dependency State</th></tr>");
        out("      </thead>");
        out("      <tbody>");

        Collection<DependencyManagement> unmanagedDeps = dependencyManagement.stream()
                .sorted(Comparator.comparing(DependencyManagement::getGA))
                .map(d -> {
                    if (d.getState() != DependencyManagement.State.UNMANAGED) {
                        out("      <tr>");
                        out("        <td><a href=\"#dep_" + getAnchor(d.getGA()) + "\">" + d.getGA() + "</a></td>");
                        out("        <td>" + d.getVersion() + "</td>");
                        out("        <td>" + getEmoji(d.getState()) + " " + d.getState().toString() + "</td>");
                        out("      </tr>");
                        return null;
                    } else {
                        return d;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        out("      </tbody>");
        out("    </table>");
        out("    <br/>");

        if (!unmanagedDeps.isEmpty()) {
            out("    <table class=\"condensed\">");
            out("      <thead>");
            out("        <tr><th colspan=\"2\" class=\"problem\"><strong>Dependencies Missing from DependencyManagement</strong></th></tr>");
            out("        <tr><th class=\"problem\">Dependency</th><th class=\"problem\">Dependency State</th></tr>");
            out("      </thead>");
            out("      <tbody>");
            for (DependencyManagement d : unmanagedDeps) {
                out("      <tr>");
                out("        <td><a href=\"#dep_" + getAnchor(d.getGA()) + "\">" + d.getGA() + "</a></td>");
                out("        <td>" + getEmoji(d.getState()) + " " + d.getState().toString() + "</td>");
                out("      </tr>");
            }
            out("      </tbody>");
            out("    </table>");
            out("    <br/>");
        }
    }

    private String getEmoji(DependencyManagement.State state) {
        if (state == DependencyManagement.State.CONSISTENT) {
            return "✅";
        } else if (state == DependencyManagement.State.UNUSED) {
            return "⚠️";
        } else {
            return "❌";
        }
    }

    private void out(String s) {
        sb.append(s);
        sb.append("\r\n");
    }
}
