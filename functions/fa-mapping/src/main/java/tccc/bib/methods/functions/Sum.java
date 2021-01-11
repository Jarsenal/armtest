package tccc.bib.methods.functions;

import com.microsoft.azure.functions.ExecutionContext;
import tccc.bib.methods.interfaces.Method;

import java.util.AbstractMap;
import java.util.AbstractList;
import java.math.BigDecimal;
import tccc.bib.tools.IndexTracker;

public class Sum implements  Method {
    public Object run(Object value, Object request, Object root, Object bookmark, AbstractMap<String, Object> inboundAttributes, AbstractMap<String, Object> config, IndexTracker index, final ExecutionContext context )
            throws Exception 
    {
        BigDecimal resultvalue = new BigDecimal(0);
        
        if(value != null && value instanceof AbstractList){
            for(Object item : (AbstractList)value){
                if(item != null){
                    if(item instanceof BigDecimal)
                        resultvalue = ((BigDecimal)resultvalue).add((BigDecimal)item);
                    if(item instanceof Integer)
                        resultvalue = ((BigDecimal)resultvalue).add(new BigDecimal((Integer)item));
                    if(item instanceof Double)
                        resultvalue = ((BigDecimal)resultvalue).add(new BigDecimal((Double)item));
                }
            }
            return resultvalue;
        }
        return value;

        
    }

}