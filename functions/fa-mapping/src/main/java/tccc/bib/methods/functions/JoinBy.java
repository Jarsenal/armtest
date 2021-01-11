package tccc.bib.methods.functions;

import com.microsoft.azure.functions.ExecutionContext;
import tccc.bib.methods.interfaces.Method;

import java.lang.StringBuilder;
import tccc.bib.tools.IndexTracker;

import java.util.AbstractMap;
import java.util.AbstractList;

public class JoinBy implements  Method {
    public Object run(Object value, Object request, Object root, Object bookmark, AbstractMap<String, Object> inboundAttributes, AbstractMap<String, Object> config, IndexTracker index, final ExecutionContext context )
            throws Exception 
    {
        StringBuilder resultvalue = new StringBuilder();
        String chars = "";

        if(config.containsKey("joinby")){
            AbstractMap<String, Object> config_ = 
            (AbstractMap<String, Object>)config.get("joinby");
            if(config_.containsKey("string"))
                chars = (String)config_.get("string");
        }
        
        
        if(value != null && value instanceof AbstractList){
            for(Object item : (AbstractList)value){
                if(item != null){
                    if(resultvalue.length() > 0)
                        resultvalue.append(chars);
                    resultvalue.append(item);
                }
            }
            return resultvalue.toString();
        }
        return value;

        
    }

}