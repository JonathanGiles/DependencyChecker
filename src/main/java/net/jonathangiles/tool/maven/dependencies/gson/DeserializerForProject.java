package net.jonathangiles.tool.maven.dependencies.gson;

import com.google.gson.*;
import net.jonathangiles.tool.maven.dependencies.project.MavenReleasedProject;
import net.jonathangiles.tool.maven.dependencies.project.Project;
import net.jonathangiles.tool.maven.dependencies.project.WebProject;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.jboss.shrinkwrap.resolver.api.maven.MavenArtifactInfo;

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
                Project project = new WebProject(jsonObject.get("projectName").getAsString());
                JsonArray poms = jsonObject.getAsJsonArray("pomUrls");
                for (int i = 0; i < poms.size(); i++) {
                    project.getPomUrls().add(poms.get(i).getAsString());
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