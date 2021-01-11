package tccc.bib.tools;

import com.google.gson.Gson;

public class ObjectToJson {

    static Gson gson = new Gson();
    
    public static <T> String from(T item){
        return gson.toJson(item);
    }
}

