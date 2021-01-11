package tccc.bib.methods.functions;

import com.microsoft.azure.functions.ExecutionContext;
import tccc.bib.methods.interfaces.Method;
import tccc.bib.tools.GetValue;
import tccc.bib.tools.IndexTracker;

import java.util.AbstractMap;

public class Concat implements  Method {

    @SuppressWarnings("unchecked")
    public Object run(Object value, Object request, Object root, Object bookmark, AbstractMap<String, Object> inboundAttributes, AbstractMap<String, Object> config, IndexTracker index, final ExecutionContext context )
            throws Exception 
    {
     
        AbstractMap<String, Object> concat = (AbstractMap<String, Object>)config.get("concat");
        String append = "";

        if(concat != null){

            String string = (String)concat.get("string");

            if(string.indexOf("$$") > -1 ||
                string.indexOf("&&") > -1 ||
                string.indexOf("%%") > -1 ||
                string.indexOf("@@") > -1){
                append = (String)GetValue.get(((String)concat.get("string")), request, root, bookmark,inboundAttributes, index, context);                        // GET VALUE WITH X and Q
            }
            else {
                append = string;
            }
            
            if(append == null) append = "";


            if(concat.get("direction").equals("right")) {
                value = ((String)value) + append;
            }
            else{
                value = append + ((String)value);
            }
        }

        return value;
    }

}