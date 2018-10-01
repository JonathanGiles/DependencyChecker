package net.jonathangiles.tool.maven.dependencies;

import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.jonathangiles.tool.maven.dependencies.gson.DeserializerForProject;
import net.jonathangiles.tool.maven.dependencies.model.Dependency;
import net.jonathangiles.tool.maven.dependencies.project.Project;
import net.jonathangiles.tool.maven.dependencies.report.HTMLReport;
import net.jonathangiles.tool.maven.dependencies.report.JSONReport;
import net.jonathangiles.tool.maven.dependencies.report.PlainTextReport;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.jboss.shrinkwrap.resolver.api.maven.MavenArtifactInfo;
import org.jboss.shrinkwrap.resolver.api.maven.MavenResolverSystem;

import java.io.*;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.*;
import java.util.stream.Collectors;

public class Main {

    // maps from a Maven GA to a Dependency instance, containing all dependencies on this GA
    private final Map<String, Dependency> dependencies;

    private final File outputDir;

    public Main() {
        dependencies = new HashMap<>();
         outputDir = new File("output");
    }

    public void run() {
        if (outputDir.exists()) {
            Arrays.stream(outputDir.listFiles()).forEach(File::delete);
        } else {
            outputDir.mkdirs();
        }

        // we run zero or more iterations, once for each input in the input directory
        Arrays.stream(loadInputs()).forEach(this::runScan);
    }

    private MavenResolverSystem loadMavenResolver() {
        return Maven.resolver();
    }

    private File[] loadInputs() {
        return new File("input").listFiles(((dir, name) -> name.endsWith("json")));
    }

    private void runScan(File inputFile) {
        // load all projects from the json file and start processing the poms, and any sub-module poms (recursively)
        List<Project> projects = loadProjects(inputFile);
        projects.stream()
                .peek(project -> System.out.println("Processing project " + project.getProjectName()))
                .forEach(project -> project.getPomUrls().forEach(pom -> processPom(project, pom)));

        // analyse results
        final List<Dependency> problems = dependencies.values().stream()
                .filter(Dependency::isProblemDependency)
                .collect(Collectors.toList());


//        System.out.println(problems.stream().map(Dependency::toString).collect(Collectors.joining("\r\n\r\n")));

        // output reports
        // strip .json file extension from input file name
        String outputFileName = inputFile.getName().substring(0, inputFile.getName().length() - 5);

        new PlainTextReport().report(projects, problems);
        new HTMLReport(new File(outputDir, outputFileName + ".html")).report(projects, problems);
        new JSONReport(new File(outputDir, outputFileName + ".json")).report(projects, problems);
    }

    private List<Project> loadProjects(File inputFile) {
        try {
            return new GsonBuilder()
                    .registerTypeAdapter(Project.class, new DeserializerForProject())
                    .create()
                    .fromJson(new FileReader(inputFile), new TypeToken<ArrayList<Project>>(){}.getType());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    private void processPom(Project p, String pomUrl) {
        System.out.println(" - Processing pom " + pomUrl);
        downloadPom(p, pomUrl).ifPresent(pomFile -> processPom(p, pomUrl, pomFile));
    }

    private void processPom(Project p, String pomUrl, File pomFile) {
        // we need to analyse the pom file to see if it has any modules, and if so, we download the pom files for
        // these modules and also process them
        // TODO This was deprecated temporarily because we mainly care about released Maven artifacts, not web-based POMs
        // scanForModules(pomFile);

        // collect all dependencies for this project
        try {
            Arrays.stream(loadMavenResolver().loadPomFromFile(pomFile)
                    .importCompileAndRuntimeDependencies()
                    .resolve()
                    .withTransitivity()
                    .asResolvedArtifact())
                    .forEach(artifact -> processArtifact(p, artifact, new ArrayList<>()));
        } catch (IllegalArgumentException e) {
            // we get an IAE if there are no dependencies specified in the resolution. This is fine - we just carry on
        } catch (Exception e) {
            System.out.println("Skipped printing exception");
        }

        // now process all modules that we found
        p.getModules().forEach(module -> processPom(module, pomUrl + "/" + module.getProjectName()));
    }

    private void processArtifact(Project p, MavenArtifactInfo a, List<MavenArtifactInfo> depChain) {
        // add in artifact
        String groupId = a.getCoordinate().getGroupId();
        String artifactId = a.getCoordinate().getArtifactId();
        String ga = groupId + ":" + artifactId;
        dependencies.computeIfAbsent(ga, s -> new Dependency(groupId, artifactId)).addArtifact(p, a, depChain);

        System.out.println("   Processing artifact " + ga);

        final List<MavenArtifactInfo> updatedDepChain = updateDependencyChain(depChain, a);

        // and then add in all dependencies required by the artifact
        Arrays.stream(a.getDependencies())
              .forEach(dependency -> processArtifact(p, dependency, updatedDepChain));
    }

    private List<MavenArtifactInfo> updateDependencyChain(List<MavenArtifactInfo> chain, MavenArtifactInfo child) {
        chain = new ArrayList<>(chain);
        chain.add(child);
        return chain;
    }

    private Optional<File> downloadPom(Project project, String pomPath) {
        System.out.print("   Downloading...");
        try {
            URL url = new URL(pomPath);
            File outputFile = new File("temp/" + project.getFullProjectName() + "/pom.xml");
            outputFile.getParentFile().mkdirs();
            ReadableByteChannel rbc = Channels.newChannel(url.openStream());
            FileOutputStream fos = new FileOutputStream(outputFile);
            fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
            System.out.println("Success");
            return Optional.of(outputFile);
        } catch (Exception e) {
            System.out.println("Failed - exiting");
            e.printStackTrace();
            System.exit(-1);
        }
        return Optional.empty();
    }

//    private void scanForModules(File pomFile) {
//        try {
//            FileInputStream fileIS = new FileInputStream(pomFile);
//            DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
//            DocumentBuilder builder = builderFactory.newDocumentBuilder();
//            Document xmlDocument = builder.parse(fileIS);
//            XPath xPath = XPathFactory.newInstance().newXPath();
//            String expression = "/project/modules/module";
//            NodeList nodeList = (NodeList) xPath.compile(expression).evaluate(xmlDocument, XPathConstants.NODESET);
//            for (int i = 0; i < nodeList.getLength(); i++) {
//                String name = nodeList.item(i).getTextContent();
//
//                // TODO enable this for modules to work
//                System.out.println("WARNING: Found modules - but ignoring in code for now!");
//                project.getModules().add(new WebProject(name, project));
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }

    public static void main(String[] args) {
        new Main().run();
    }
}
