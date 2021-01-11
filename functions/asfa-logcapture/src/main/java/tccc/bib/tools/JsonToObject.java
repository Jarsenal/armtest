package tccc.bib.tools;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class JsonToObject {

    static Gson gson = new Gson();
    
    public static <T> T from(String config){
        return gson.fromJson(config, 
                new TypeToken<T>() {}.getType());
    }
}

