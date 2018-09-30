package net.jonathangiles.tool.maven.dependencies.gson;

import com.google.gson.*;
import net.jonathangiles.tool.maven.dependencies.model.DependencyVersionList;

import java.lang.reflect.Type;

public class SerializerForDependencyVersionList implements JsonSerializer<DependencyVersionList> {

    @Override
    public JsonElement serialize(DependencyVersionList obj, Type type, JsonSerializationContext jsc) {
        Gson gson = new Gson();
        JsonArray jArr = (JsonArray)gson.toJsonTree(obj);
        if (!obj.hasAnyDependencyChains()) {
            for (int i = jArr.size() - 1; i >= 0; i--) {
                jArr.remove(i);
            }
        }
        return jArr;
    }

}