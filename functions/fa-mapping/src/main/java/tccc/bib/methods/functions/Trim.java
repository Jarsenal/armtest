package tccc.bib.methods.functions;

import com.microsoft.azure.functions.ExecutionContext;
import tccc.bib.methods.interfaces.Method;
import tccc.bib.tools.IndexTracker;

import java.util.AbstractMap;
import java.util.ArrayList;

import com.google.gson.*;

public class Trim implements  Method {
    public Object run(Object value, Object request, Object root, Object bookmark, AbstractMap<String, Object> inboundAttributes, AbstractMap<String, Object> config, IndexTracker index, final ExecutionContext context )
            throws Exception 
    {
        if(value != null){
            if(value instanceof String){

               return ((String)value).trim();

            }
             else if (value instanceof ArrayList<?>){
                ArrayList<String> result = new ArrayList<String>();

                ArrayList arr = (ArrayList) value;
                for (int i = 0; i < arr.size(); i++) {
                    String string = ((String)arr.get(i)).trim();
                    result.add(string);
    
                    
                }
                return result;
            }
        


         }
        
        return value;

    }

}