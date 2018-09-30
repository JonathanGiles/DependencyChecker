package net.jonathangiles.tool.maven.dependencies.gson;

import com.google.gson.*;
import net.jonathangiles.tool.maven.dependencies.project.MavenReleasedProject;
import net.jonathangiles.tool.maven.dependencies.project.Project;
import net.jonathangiles.tool.maven.dependencies.project.WebProject;

import java.lang.reflect.Type;

public class DeserializerForProject implements JsonDeserializer<Project> {

    @Override
    public Project deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        JsonObject jsonObject = jsonElement.getAsJsonObject();

        Project project;
        if (jsonObject.has("groupId")) {
            project = new MavenReleasedProject(
                jsonObject.get("groupId").getAsString(),
                jsonObject.get("artifactId").getAsString(),
                jsonObject.get("version").getAsString()
            );
        } else {
            project = new WebProject(jsonObject.get("projectName").getAsString());
            JsonArray poms = jsonObject.getAsJsonArray("pomUrls");
            for (int i = 0; i < poms.size(); i++) {
                project.getPomUrls().add(poms.get(i).getAsString());
            }
        }

        return project;
    }
}