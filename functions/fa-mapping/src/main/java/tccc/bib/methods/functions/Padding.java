package tccc.bib.methods.functions;

import com.microsoft.azure.functions.ExecutionContext;
import tccc.bib.methods.interfaces.Method;
import tccc.bib.tools.IndexTracker;

import java.util.AbstractMap;

public class Padding implements  Method {

    @SuppressWarnings("unchecked")
    public Object run(Object value,Object request, Object root, Object bookmark, AbstractMap<String, Object> inboundAttributes, AbstractMap<String, Object> config, IndexTracker index, final ExecutionContext context )
    throws Exception 
    {
        
        AbstractMap<String, Object> padding = ( AbstractMap<String, Object>)config.get("padding");
        
        if(padding != null){
            if(padding.get("direction").equals("right")) {
                value = padRight((String)value, Integer.parseInt((String)padding.get("length")), (String)padding.get("character"));
            }
            else{
                value = padLeft((String)value, Integer.parseInt((String)padding.get("length")), (String)padding.get("character"));
            }
        }
        
        return value;
    }

    private static String padRight(String s, int size, String pad) {
        StringBuilder builder = new StringBuilder(s);
        if(pad.isEmpty()) pad = " ";
        while(builder.length()<size) {
            builder.append(pad);
        }
        return builder.toString();
    }

    private static String padLeft(String s, int size, String pad) {
        StringBuilder builder = new StringBuilder(s);
        if(pad.isEmpty()) pad = " ";
        builder = builder.reverse(); // reverse initial string
        while(builder.length()<size) {
            builder.append(pad); // append at end
        }
        return builder.reverse().toString(); // reverse again!
    }
}