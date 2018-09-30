package net.jonathangiles.tool.maven.dependencies.report;

import com.google.gson.GsonBuilder;
import net.jonathangiles.tool.maven.dependencies.model.Dependency;
import net.jonathangiles.tool.maven.dependencies.model.DependencyVersionList;
import net.jonathangiles.tool.maven.dependencies.gson.SerializerForDependencyVersionList;
import net.jonathangiles.tool.maven.dependencies.project.Project;

import java.io.File;
import java.io.FileWriter;
import java.util.List;

public class JSONReport implements Report {
    private final File outFile;

    public JSONReport(File outFile) {
        this.outFile = outFile;
    }

    public void report(List<Project> projects, List<Dependency> problems) {
        try {
            new GsonBuilder()
                    .setPrettyPrinting()
//                    .registerTypeAdapter(DependencyVersion.class, new SerializerForDependencyVersion())
                    .registerTypeAdapter(DependencyVersionList.class, new SerializerForDependencyVersionList())
                    .create()
                    .toJson(problems, new FileWriter(outFile));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
