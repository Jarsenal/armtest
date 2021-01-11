package tccc.bib.objects;

import java.util.LinkedHashMap;
import java.util.AbstractMap;
import java.util.AbstractList;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import tccc.bib.tools.Decrypt;


public class Request{

    AbstractMap<String, String> _attributes = new LinkedHashMap<>();
    AbstractMap<String, Object> _routeConfig = null;
    String _payload = null;

    public Request(String payload, String attributes, AbstractMap<String, Object> routeConfig)
        throws Exception {

        _routeConfig = routeConfig;
        _payload = payload;

        // decrypt
        if (_routeConfig.containsKey("_encrypt") && _routeConfig.get("_encrypt") instanceof AbstractList){
            _routeConfig = Decrypt.decryptItems(
                _routeConfig, 
                    (AbstractList<String>)routeConfig.get("_encrypt"), 
                    System.getenv("SECRET_KEY"));
            _routeConfig.remove("_encrypt");
        }

        // attributes
        if (attributes != null) {
            Gson gson = new Gson();
            
            AbstractMap<String, Object> temp = gson.fromJson(attributes,new TypeToken<LinkedHashMap<String, Object>>() {}.getType());

            for(String key : temp.keySet()){
                if(temp.get(key) != null){
                _attributes.put(key, temp.get(key).toString());
                }
            }

            for (String akey : _attributes.keySet()) {
                for (String ckey : _routeConfig.keySet()) {
                    if (_routeConfig.get(ckey) instanceof String) {
                        _routeConfig.put(ckey, ((String) _routeConfig.get(ckey))
                                .replaceAll(String.format("<<%s>>", akey),
                                (String) _attributes.get(akey).toString()));
                    }
                }
            }
        }
    
    }

    public AbstractMap<String,String> getAttributes(){
        return _attributes;
    }

    public AbstractMap<String, Object> getConfig(){
        return _routeConfig;
    }

    public String getPayload(){
        return _payload;
    }


}