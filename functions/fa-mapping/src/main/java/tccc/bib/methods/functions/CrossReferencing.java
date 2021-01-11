package tccc.bib.methods.functions;

import com.microsoft.azure.functions.ExecutionContext;
import tccc.bib.methods.interfaces.Method;
import java.util.AbstractList;
import java.util.AbstractMap;
import tccc.bib.tools.IndexTracker;

public class CrossReferencing implements  Method {
    @SuppressWarnings("unchecked")
    public Object run(Object value,Object request, Object root, Object bookmark, AbstractMap<String, Object> inboundAttributes, AbstractMap<String, Object> config, IndexTracker index, final ExecutionContext context )
            throws Exception 
            {
                String _default = null;
                Boolean match = false;
                AbstractList<AbstractMap<String, String>> xref = (AbstractList<AbstractMap<String, String>>)config.get("xref");

                for(AbstractMap<String, String> item : xref){
                    if(((String)item.get("key")).equals("_default")){
                        _default = item.get("value");
                    }
                }
            
                for(AbstractMap<String, String> item : xref){
                    if(((String)item.get("key")).equals(value)){
                        value = item.get("value");
                        match = true;
                        break;
                    }
                }

                if(!match && _default != null)
                    value = _default;
            
                return value;
            }

}