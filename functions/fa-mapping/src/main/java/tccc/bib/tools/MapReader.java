package tccc.bib.tools;

import java.util.AbstractMap;
import java.util.Objects;

public class MapReader {

    static public <T> T read(AbstractMap<String,Object> record, String chain){
        
        T result = null;
        
        for(String item : chain.split("\\.")){
            if(record.get(item) instanceof AbstractMap){
                record = (AbstractMap)record.get(item);
            }
            else {
                result = (T)record.get(item);
            }
        }
        
        return (T)result;
    }

}