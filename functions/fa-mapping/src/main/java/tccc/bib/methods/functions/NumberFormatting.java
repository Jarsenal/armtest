package tccc.bib.methods.functions;

import com.microsoft.azure.functions.ExecutionContext;
import tccc.bib.methods.interfaces.Method;

import java.util.AbstractMap;
import tccc.bib.tools.IndexTracker;

import java.math.BigDecimal;

public class NumberFormatting implements  Method {

    @SuppressWarnings("unchecked")
    public Object run(Object value,Object request, Object root, Object bookmark, AbstractMap<String, Object> inboundAttributes, AbstractMap<String, Object> config, IndexTracker index, final ExecutionContext context )
            throws Exception 
            {
                String ntos = "";

                if(config.get("number") != null){
                    ntos = (String)((AbstractMap<String, Object>)config.get("number")).get("target");
                }

                if(ntos != "") {
                    if(value instanceof Number){
                        value = (new java.text.DecimalFormat(ntos)).format(value);
                    }
                    else {
                        value = (new java.text.DecimalFormat(ntos)).format(new BigDecimal((String) value));
                    }
                }

                return value;
            }

}