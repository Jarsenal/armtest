package tccc.bib.objects;

import java.util.AbstractMap;
import com.google.gson.Gson;

public class Response{

    String payload;
    AbstractMap<String, String> attributes;

    public Response(String payload, AbstractMap<String, String> attributes){
        this.payload = payload;
        this.attributes = attributes;
    }

    public void addAttribute(String key, String value){
        this.attributes.put(key,value);
    }

    public String getAttributes(){
        Gson gson = new Gson();
        return gson.toJson(this.attributes);
    }

    public String getPayload(){
        return payload;
    }
}