package net.jonathangiles.tool.maven.dependencies.gson;

import com.google.gson.*;
import net.jonathangiles.tool.maven.dependencies.model.DependencyChain;

import java.lang.reflect.Type;

public class SerializerForDependencyChain implements JsonSerializer<DependencyChain> {

    @Override
    public JsonElement serialize(DependencyChain obj, Type type, JsonSerializationContext jsc) {
        Gson gson = new Gson();
        JsonObject jObj = (JsonObject)gson.toJsonTree(obj);
        if (!obj.hasDependencyChain()) {
            jObj.remove("dependencyChain");
        }
        return jObj;
    }
}