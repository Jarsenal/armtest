package tccc.bib.transform.functions;

import tccc.bib.transform.interfaces.TransformFromHashMap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.microsoft.azure.functions.ExecutionContext;

import java.util.AbstractMap;

public class HashMaptoJSON implements TransformFromHashMap {
    @Override
    public String transform(Object payload, AbstractMap<String, Object> properties, final ExecutionContext context) throws Exception {
        Gson gson = new GsonBuilder().serializeNulls().create();
        return gson.toJson(payload);
    }
}
