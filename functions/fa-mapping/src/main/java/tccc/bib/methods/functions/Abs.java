package tccc.bib.methods.functions;

import com.microsoft.azure.functions.ExecutionContext;
import tccc.bib.methods.interfaces.Method;
import tccc.bib.tools.IndexTracker;

import java.math.BigDecimal;
import java.util.AbstractMap;

public class Abs implements  Method {
    public Object run(Object value, Object request, Object root, Object bookmark, AbstractMap<String, Object> inboundAttributes, AbstractMap<String, Object> config, IndexTracker index, final ExecutionContext context )
            throws Exception 
    {
        if(value != null && value instanceof BigDecimal)
            return ((BigDecimal)value).abs();
        else if(value != null && value instanceof Integer)
            return Math.abs(((Integer)value));
        else if(value != null && value instanceof Double)
            return Math.abs(((Double)value));
        else if(value != null && value instanceof Float)
            return Math.abs(((Float)value));
            
        return value;
    }

}