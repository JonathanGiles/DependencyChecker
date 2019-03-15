package net.jonathangiles.tool.maven.dependencies.report;

import com.google.gson.GsonBuilder;
import net.jonathangiles.tool.maven.dependencies.gson.SerializerForDependencyChain;
import net.jonathangiles.tool.maven.dependencies.model.Dependency;
import net.jonathangiles.tool.maven.dependencies.model.DependencyChain;
import net.jonathangiles.tool.maven.dependencies.model.DependencyManagement;
import net.jonathangiles.tool.maven.dependencies.project.Project;

import java.io.File;
import java.io.FileWriter;
import java.util.Collection;
import java.util.List;

public class JSONReport implements Reporter {

    @Override
    public String getName() {
        return "json";
    }

    @Override
    public void report(List<Project> projects, List<Dependency> problems, Collection<DependencyManagement> dependencyManagement, File outDir, String outFileName) {
        try {
            File outFile = new File(outDir, outFileName + ".json");

            new GsonBuilder()
                    .setPrettyPrinting()
                    .registerTypeAdapter(DependencyChain.class, new SerializerForDependencyChain())
                    .create()
                    .toJson(problems, new FileWriter(outFile));

            System.out.println("JSON report written to " + outFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
