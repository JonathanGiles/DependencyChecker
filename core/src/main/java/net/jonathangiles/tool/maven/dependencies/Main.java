package net.jonathangiles.tool.maven.dependencies;

import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.jonathangiles.tool.maven.dependencies.gson.DeserializerForProject;
import net.jonathangiles.tool.maven.dependencies.misc.Result;
import net.jonathangiles.tool.maven.dependencies.model.Dependency;
import net.jonathangiles.tool.maven.dependencies.model.DependencyKey;
import net.jonathangiles.tool.maven.dependencies.model.DependencyManagement;
import net.jonathangiles.tool.maven.dependencies.model.Version;
import net.jonathangiles.tool.maven.dependencies.project.MavenReleasedProject;
import net.jonathangiles.tool.maven.dependencies.project.Project;
import net.jonathangiles.tool.maven.dependencies.project.WebProject;
import net.jonathangiles.tool.maven.dependencies.report.*;
import org.apache.commons.cli.*;
import org.jboss.shrinkwrap.resolver.api.maven.*;
import org.jboss.shrinkwrap.resolver.api.maven.coordinate.MavenDependency;
import org.jboss.shrinkwrap.resolver.impl.maven.MavenWorkingSessionContainer;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.*;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Collectors;

import static net.jonathangiles.tool.maven.dependencies.misc.Util.*;

public class Main {

    private static final String COMMAND_SHOW_ALL = "showall";
    private static final String COMMAND_DEPENDENCY_MANAGEMENT = "dependencymanagement";
    private static final String COMMAND_ANALYSE_BOM = "analysebom";
    private static final String COMMAND_REPORTERS = "reporters";

    // maps from a Maven GA to a Dependency instance, containing all dependencies on this GA
    private Map<DependencyKey, Dependency> dependencies;

    // maps from a Maven GA to a Maven Coordinate
    private Map<DependencyKey, DependencyManagement> dependencyManagementMap;

    private final File outputDir;

    // configuration properties
    private String[] reporters;
    private boolean showAll;
    private boolean dependencyManagement;
    private boolean analyseBom;

    public static void main(String[] args) {
        Main main = new Main();
        readCommandLine(args, main);
        main.run();
    }

    private static void readCommandLine(String[] args, Main main) {
        try {
            Options options = new Options();

            // Enabled with the -showAll flag
            options.addOption(COMMAND_SHOW_ALL, false, "If specified, report all dependencies. If false, only report dependency conflicts");

            // Enabled with the -dependencyManagement flag
            options.addOption(COMMAND_DEPENDENCY_MANAGEMENT, false, "If specified, include dependency management information in report.");

            // Enabled with the -analyseBom flag
            options.addOption(COMMAND_ANALYSE_BOM, false, "If specified, the dependencies listed in the dependencyManagement section are analysed transitively.");

            // A string, e.g. '-reporters html,json,plain-text'
            options.addOption(COMMAND_REPORTERS, "A comma-separated string of the reporters to use to generate reports");

            CommandLineParser parser = new DefaultParser();
            CommandLine commands = parser.parse(options, args);

            main.setReporters(commands.getOptionValue(COMMAND_REPORTERS));
            main.setShowAll(commands.hasOption(COMMAND_SHOW_ALL));
            main.setDependencyManagement(commands.hasOption(COMMAND_DEPENDENCY_MANAGEMENT));
            main.setAnalyseBom(commands.hasOption(COMMAND_ANALYSE_BOM));
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    public Main() {
        System.setProperty("os.detected.classifier", "linux-x86_64");
        outputDir = new File("output");
    }

    public void setReporters(final String reporters) {
        this.reporters = reporters != null && !reporters.isEmpty() ? reporters.split(",") : null;
    }

    public void setShowAll(final boolean showAll) {
        this.showAll = showAll;
    }

    public void setDependencyManagement(final boolean dependencyManagement) {
        this.dependencyManagement = dependencyManagement;
    }

    public void setAnalyseBom(final boolean analyseBom) {
        this.analyseBom = analyseBom;
    }

    public Optional<Result> run() {
        if (outputDir.exists()) {
            Arrays.stream(outputDir.listFiles()).forEach(File::delete);
        } else {
            outputDir.mkdirs();
        }

        System.out.println("Found the following reporters available for use:");
        Reporters.getReporterNames().forEach(r -> System.out.println("  - " + r));

        System.out.println("Will output the following report types:");
        Reporters.getReporters(reporters).forEach(r -> System.out.println("  - " + r.getName()));

        // we run zero or more iterations, once for each input in the input directory
        return Arrays.stream(loadInputs())
                       .map(this::runScan)
                       .filter(Optional::isPresent)
                       .map(Optional::get)
                       .reduce(Result::compare);
    }

    protected File[] loadInputs() {
        return new File("input").listFiles(((dir, name) -> name.endsWith("json")));
    }

    protected List<Project> loadProjects(File inputFile) {
        if (inputFile.getName().endsWith(".xml")) {
            // we are being given a pom file directly
            final Project project = new WebProject(inputFile.getName(), analyseBom);
            project.getPomUrls().add(inputFile.getAbsolutePath());
            return Collections.singletonList(project);
        } else {
            // we will assume we have a json config file
            try {
                return new GsonBuilder()
                               .registerTypeAdapter(Project.class, new DeserializerForProject())
                               .create()
                               .fromJson(new FileReader(inputFile), new TypeToken<ArrayList<Project>>() {
                               }.getType());
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                return Collections.emptyList();
            }
        }
    }

    private Optional<Result> runScan(File inputFile) {
        dependencies = new HashMap<>();
        dependencyManagementMap = new HashMap<>();

        // load all projects from the json file and start processing the poms, and any sub-module poms (recursively)
        List<Project> projects = loadProjects(inputFile);
        projects.stream()
                .peek(project -> System.out.println("Processing project " + project.getProjectName()))
                .forEach(project -> project.getPomUrls().forEach(pom -> processProjectPom(project, pom)));

        // analyse results
        final List<Dependency> problems = dependencies.values().stream()
                .filter(dependency -> showAll || dependency.isProblemDependency())
                .collect(Collectors.toList());

        dependencyManagementMap.forEach(this::updateManagementState);

        // output reports
        // strip .json file extension from input file name
        String outputFileName = inputFile.getName().substring(0, inputFile.getName().lastIndexOf("."));

        Reporters.getReporters(reporters)
                .forEach(reporter -> reporter.report(projects, problems, dependencyManagementMap.values(), outputDir, outputFileName));

        // look at the results, and determine what the result type is for this scan
        return dependencies.values().stream()
                .map(dep -> dep.isProblemDependency() ? Result.DEPENDENCY_VERSION_CONFLICTS : Result.CLEAN)
                .reduce(Result::compare);
    }

    private void updateManagementState(DependencyKey dependencyKey, DependencyManagement managedDep) {
        if (managedDep.getState() != DependencyManagement.State.UNKNOWN) {
            return;
        }

        Dependency dep = dependencies.get(dependencyKey);
        if (dep == null) {
            managedDep.setState(DependencyManagement.State.UNUSED);
            return;
        }

        if (dep.isProblemDependency()) {
            managedDep.setState(DependencyManagement.State.INCONSISTENT);
            return;
        }

        Version ver = dep.getVersions().iterator().next();
        if (ver.getVersionString().equals(managedDep.getVersion())) {
            managedDep.setState(DependencyManagement.State.CONSISTENT);
        } else {
            managedDep.setState(DependencyManagement.State.INCONSISTENT);
        }
    }

    private void processProjectPom(Project p, String pomUrl) {
        System.out.println(" - Processing project pom " + pomUrl);
        downloadPom(p, pomUrl).ifPresent(pomFile -> {
            scanManagedDependencies(pomFile);
            processPom(p, pomUrl, pomFile);
        });
    }

    private void processModulePom(Project p, String pomUrl) {
        System.out.println(" - Processing module pom " + pomUrl);
        downloadPom(p, pomUrl).ifPresent(pomFile -> processPom(p, pomUrl, pomFile));
    }

    private void processPom(Project p, String pomUrl, File pomFile) {
        System.out.println("   Processing...");

        // we need to analyse the pom file to see if it has any modules, and if so, we download the pom files for
        // these modules and also process them
        // TODO This was deprecated temporarily because we mainly care about released Maven artifacts, not web-based POMs
        scanForModules(pomFile, p);

        // collect all dependencies for this project
        try {
            MavenResolvedArtifact[] result = getMavenResolver().loadPomFromFile(pomFile)
                    .importDependencies(ScopeType.values())
                    .resolve()
                    .withTransitivity()
                    .asResolvedArtifact();

            if (result.length == 0) {
                System.err.println("Failed to find any dependencies for " + p.getFullProjectName() + " - exiting");
                System.exit(-1);
            }

            Arrays.stream(result)
                    .forEach(artifact -> processArtifact(p, artifact, new ArrayList<>()));
        } catch (IllegalArgumentException e) {
            // we get an IAE if there are no dependencies specified in the resolution. This is fine - we just carry on
        } catch (Exception e) {
            System.err.println("Skipped printing exception");
        }

        // now process all modules that we found
        p.getModules().forEach(module -> {
            String moduleUrl = pomUrl.substring(0, pomUrl.lastIndexOf("/") + 1) + module.getProjectName() + "/pom.xml";
            processModulePom(module, moduleUrl);
        });
    }

    private void processArtifact(Project p, MavenArtifactInfo a, List<MavenArtifactInfo> depChain) {
        System.out.println("Scope is '" + a.getScope() + "'");
        // add in artifact
        String groupId = a.getCoordinate().getGroupId();
        String artifactId = a.getCoordinate().getArtifactId();
        String ga = groupId + ":" + artifactId;

        dependencies.computeIfAbsent(new DependencyKey(ga, a.getScope()), s -> new Dependency(a)).addArtifact(p, a, depChain);

        System.out.println("   Processing artifact " + ga + " (gav: " + a.getCoordinate() + ")");

        final List<MavenArtifactInfo> updatedDepChain = updateDependencyChain(depChain, a);

        // and then add in all dependencies required by the artifact
        Arrays.stream(a.getDependencies())
//              .filter(artifact -> !artifact.getScope().equals(ScopeType.TEST))
              .forEach(dependency -> processArtifact(p, dependency, updatedDepChain));
    }

    private List<MavenArtifactInfo> updateDependencyChain(List<MavenArtifactInfo> chain, MavenArtifactInfo child) {
        chain = new ArrayList<>(chain);
        chain.add(child);
        return chain;
    }

    private Optional<File> downloadPom(Project project, String pomPath) {
        final String f = ("temp/" + project.getFullProjectName() + "/pom.xml").replace(":", "-");
        final File outputFile = new File(f);
        outputFile.getParentFile().mkdirs();

        try {
            // test if file is local, or else attempt internet download
            File file = new File(pomPath);
            if (file.exists()) {
                Files.copy(file.toPath(), outputFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } else {
                System.out.print("   Downloading...");

                URL url = new URL(pomPath);
                ReadableByteChannel rbc = Channels.newChannel(url.openStream());
                FileOutputStream fos = new FileOutputStream(outputFile);
                fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
                System.out.println("Success");
            }
            return Optional.of(outputFile);
        } catch (Exception e) {
            System.err.println("Failed to download pom file " + pomPath);
            e.printStackTrace();
            return Optional.empty();
        }
    }

    private void scanForModules(File pomFile, Project project) {
        try {
            FileInputStream fileIS = new FileInputStream(pomFile);
            DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = builderFactory.newDocumentBuilder();
            Document xmlDocument = builder.parse(fileIS);
            XPath xPath = XPathFactory.newInstance().newXPath();
            String expression = "/project/modules/module";
            NodeList nodeList = (NodeList) xPath.compile(expression).evaluate(xmlDocument, XPathConstants.NODESET);
            for (int i = 0; i < nodeList.getLength(); i++) {
                String name = nodeList.item(i).getTextContent();

                System.out.println("Found module: " + name);
                project.getModules().add(new WebProject(name, project));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void scanManagedDependencies(File pomFile/*, MavenResolverSystemBase<?,?,?,?> mavenResolver*/) {
        MavenResolveStageBase resolver = getMavenResolver().loadPomFromFile(pomFile);
        MavenWorkingSession session = ((MavenWorkingSessionContainer) resolver).getMavenWorkingSession();
        for (MavenDependency mavenDep : session.getDependencyManagement()) {
            String groupId = mavenDep.getGroupId();
            String artifactId = mavenDep.getArtifactId();
            String ga = groupId + ":" + artifactId;

            if (dependencyManagement) {
                DependencyManagement dep = DependencyManagement.fromMaven(mavenDep);
                dependencyManagementMap.putIfAbsent(new DependencyKey(ga, mavenDep.getScope()), dep);
            }

            if (analyseBom) {
                MavenResolvedArtifact[] result = getMavenResolver().addDependency(mavenDep).resolve().withTransitivity().asResolvedArtifact();
                Arrays.stream(result)
                        .forEach(artifact -> processArtifact(new MavenReleasedProject(groupId, artifactId, mavenDep.getVersion()) {
                            @Override
                            public boolean isBom() {
                                return true;
                            }
                        }, artifact, new ArrayList<>()));
            }
        }

        if (dependencyManagement) {
            for (Map.Entry<DependencyKey, Dependency> entry : dependencies.entrySet()) {
                // Record all dependencies that were discovered in projects that aren't in DependencyManagement
                dependencyManagementMap.putIfAbsent(entry.getKey(), DependencyManagement.fromUnmanagedDependency(entry.getValue()));
            }
        }
    }
}
