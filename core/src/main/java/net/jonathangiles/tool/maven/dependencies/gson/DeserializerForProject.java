package net.jonathangiles.tool.maven.dependencies.gson;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.jonathangiles.tool.maven.dependencies.project.MavenReleasedProject;
import net.jonathangiles.tool.maven.dependencies.project.Project;
import net.jonathangiles.tool.maven.dependencies.project.WebProject;

import java.lang.reflect.Type;

public class DeserializerForProject implements JsonDeserializer<Project> {

    @Override
    public Project deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {

        if (jsonElement.isJsonObject()) {
            JsonObject jsonObject = jsonElement.getAsJsonObject();

            if (jsonObject.has("groupId")) {
                return new MavenReleasedProject(
                    jsonObject.get("groupId").getAsString(),
                    jsonObject.get("artifactId").getAsString(),
                    jsonObject.has("version") ? jsonObject.get("version").getAsString() : null
                );
            } else if (jsonObject.has("projectName")) {
                // we special-case for BOMs
                String projectName = jsonObject.get("projectName").getAsString();
                String bom = jsonObject.has("bom") ? jsonObject.getAsJsonPrimitive("bom").getAsString() : null;

                Project project;
                if (bom != null && !bom.isEmpty()) {
                    project = new WebProject(projectName, true);
                    project.getPomUrls().add(bom);
                } else {
                    project = new WebProject(projectName, false);
                    JsonArray poms = jsonObject.getAsJsonArray("pomUrls");
                    for (int i = 0; i < poms.size(); i++) {
                        project.getPomUrls().add(poms.get(i).getAsString());
                    }
                }
                return project;
            }
        } else if (jsonElement.isJsonPrimitive()) {
            String[] str = jsonElement.getAsJsonPrimitive().getAsString().split(":");
            return str.length == 3 ?
                new MavenReleasedProject(str[0], str[1], str[2]) :
                new MavenReleasedProject(str[0], str[1], null);
        }

        System.out.println("Can't parse JSON element: " + jsonElement);
        System.exit(-1);
        return null;
    }
}