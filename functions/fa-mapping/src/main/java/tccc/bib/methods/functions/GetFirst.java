package tccc.bib.methods.functions;

import com.microsoft.azure.functions.ExecutionContext;
import tccc.bib.methods.interfaces.Method;

import java.util.AbstractMap;
import java.util.LinkedList;
import java.util.AbstractList;
import tccc.bib.tools.GetValue;
import tccc.bib.tools.WhenThisCheck;
import tccc.bib.tools.IndexTracker;


// will need some work
// should pass the list
// set parameters
// test field name
// test value equals
// target field name to extract


public class GetFirst implements  Method {

    @SuppressWarnings("unchecked")
    public Object run(Object value, Object request, Object root, 
        Object bookmark, AbstractMap<String, Object> inboundAttributes, 
        AbstractMap<String, Object> config, IndexTracker index, final ExecutionContext context )
            throws Exception 
    {
        AbstractMap<String, Object> config_ = (AbstractMap<String, Object>)config.get("getfirst");

        AbstractList<AbstractMap<String,Object>> whenThis = (AbstractList<AbstractMap<String,Object>>)config_.get("when");
        String val = (String)config_.get("target");
        
        if(!(value instanceof AbstractList<?>)){
            Object temp = value;
            value = new LinkedList<Object>();
            ((LinkedList<Object>)value).add(temp);
        }

        Object temprequest = value;
        value = null;

        for(Object r : (AbstractList<?>)temprequest){
            if(WhenThisCheck.check("AND", null, whenThis, r, null, "", root, bookmark, inboundAttributes, index, context)){
                value = GetValue.get(val, r, root, bookmark,inboundAttributes, index, context);   
                return value;            
            }
        }
         return value;
    }

}