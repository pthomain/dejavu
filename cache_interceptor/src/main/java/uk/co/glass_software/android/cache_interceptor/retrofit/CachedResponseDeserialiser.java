package uk.co.glass_software.android.cache_interceptor.retrofit;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class CachedResponseDeserialiser implements JsonDeserializer<CachedResponse> {
    
    @Override
    public CachedResponse deserialize(JsonElement json,
                                      Type typeOfT,
                                      JsonDeserializationContext context) throws
                                                                          JsonParseException {
        Class<?> childClass = null;
        
        if (json.isJsonArray()) {
            return getListResponse(context, childClass, json);
        }
        else if (json.isJsonObject()) {
            JsonObject jsonObject = json.getAsJsonObject();
            JsonElement response = jsonObject.get("response");
            if (response != null && response.isJsonArray()) {
                return getListResponse(context, childClass, response);
            }
        }
        
        return null;
    }
    
    @SuppressWarnings("unchecked")
    private <C> CachedResponse getListResponse(JsonDeserializationContext context,
                                               Class<C> childClass,
                                               JsonElement jsonElement) {
        CachedResponse response = new SimpleCachedResponse();
        JsonArray jsonArray = jsonElement.getAsJsonArray();
        List<C> list = new ArrayList<>(jsonArray.size());
        
        for (int i = 0; i < jsonArray.size(); i++) {
            list.add(context.deserialize(jsonArray.get(i), childClass));
        }
        
        response.setResponse(list);
        return response;
    }
    
}
