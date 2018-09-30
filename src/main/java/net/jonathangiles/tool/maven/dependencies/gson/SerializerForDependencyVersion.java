package net.jonathangiles.tool.maven.dependencies.gson;

import com.google.gson.*;
import net.jonathangiles.tool.maven.dependencies.model.DependencyVersion;

import java.lang.reflect.Type;

public class SerializerForDependencyVersion implements JsonSerializer<DependencyVersion> {

    @Override
    public JsonElement serialize(DependencyVersion obj, Type type, JsonSerializationContext jsc) {
        Gson gson = new Gson();
        JsonObject jObj = (JsonObject)gson.toJsonTree(obj);
        if (!obj.hasDependencyChain()) {
            jObj.remove("dependencyChain");
        }
        return jObj;
    }
}