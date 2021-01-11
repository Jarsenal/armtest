package tccc.bib.transform.functions;

import tccc.bib.transform.interfaces.TransformToHashMap;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.microsoft.azure.functions.ExecutionContext;

import java.util.AbstractMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;


public class JSONtoHashMap implements TransformToHashMap{

    @Override
    public Object transform(String payload, AbstractMap<String, Object> properties, final ExecutionContext context) throws Exception {
        Gson gson = new Gson();

        if(payload.trim().indexOf("[") == 0) {
            return gson.fromJson(payload, new TypeToken<LinkedList<LinkedHashMap<String, Object>>>() {}.getType());
        }
        else {
            return gson.fromJson(payload, new TypeToken<LinkedHashMap<String, Object>>() {}.getType());
        }

    }
}
