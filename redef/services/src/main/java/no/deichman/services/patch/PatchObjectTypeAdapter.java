package no.deichman.services.patch;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;

public class PatchObjectTypeAdapter implements JsonDeserializer<List<PatchObject>> {
    public List<PatchObject> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext ctx) {
        List<PatchObject> vals = new ArrayList<PatchObject>();
        if (json.isJsonArray()) {
            for (JsonElement e : json.getAsJsonArray()) {
                vals.add((PatchObject) ctx.deserialize(e, PatchObject.class));
            }
        } else if (json.isJsonObject()) {
            vals.add((PatchObject) ctx.deserialize(json, PatchObject.class));
        } else {
            throw new RuntimeException("Unexpected JSON type: " + json.getClass());
        }
        return vals;
    }

}