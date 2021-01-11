package tccc.bib.methods.functions;

import com.microsoft.azure.functions.ExecutionContext;
import tccc.bib.methods.interfaces.Method;

import java.util.AbstractMap;
import java.util.AbstractList;
import java.util.LinkedList;
import tccc.bib.tools.IndexTracker;

public class ToBoolean implements  Method {
    public Object run(Object value, Object request, Object root, Object bookmark, AbstractMap<String, Object> inboundAttributes, AbstractMap<String, Object> config, IndexTracker index, final ExecutionContext context )
            throws Exception 
    {
        AbstractList<String> trues = new LinkedList<>();
        trues.add("TRUE");
        trues.add("T");
        trues.add("1");
        trues.add( "Y");
        trues.add( "YES");

        if(value instanceof String){
            if(trues.contains(((String)value).toUpperCase())){
                return new Boolean(true);
            }
        }
        else if(value instanceof Boolean){
            return value;
        }

        return new Boolean(false);
    }

}