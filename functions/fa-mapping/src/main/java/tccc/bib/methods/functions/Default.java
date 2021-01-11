package tccc.bib.methods.functions;

import com.microsoft.azure.functions.ExecutionContext;
import tccc.bib.methods.interfaces.Method;
import java.util.Objects;
import tccc.bib.tools.IndexTracker;

import java.util.AbstractMap;

public class Default implements  Method {

    @SuppressWarnings("unchecked")
    public Object run(Object value,Object request, Object root, Object bookmark, AbstractMap<String, Object> inboundAttributes, AbstractMap<String, Object> config, IndexTracker index, final ExecutionContext context )
            throws Exception 
            {
                String def = "";
                def = (String)((AbstractMap<String, Object>)config.get("default")).get("value");
                return Objects.isNull(value)?def:value;
            }

}