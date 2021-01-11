package tccc.bib.methods.functions;

import com.microsoft.azure.functions.ExecutionContext;
import tccc.bib.methods.interfaces.Method;

import java.util.AbstractMap;
import java.util.LinkedList;
import java.util.Objects;
import java.util.AbstractList;
import tccc.bib.tools.GetValue;
import tccc.bib.tools.IndexTracker;


// will need some work
// should pass the list
// set parameters
// test field name
// test value equals
// target field name to extract


public class Qualifier implements  Method {

    @SuppressWarnings("unchecked")
    public Object run(Object value, Object request, Object root, 
        Object bookmark, AbstractMap<String, Object> inboundAttributes, 
        AbstractMap<String, Object> config, IndexTracker index, final ExecutionContext context )
            throws Exception 
    {
        AbstractMap<String, Object> config_ = (AbstractMap<String, Object>)config.get("qualifier");

        String qkey = (String)config_.get("key");
        String qvalue = (String)config_.get("value");
        String qreturn = (String)config_.get("return");

        AbstractList<Object> result = new LinkedList<>();

        
        if(!qkey.equals("") && value instanceof AbstractMap<?,?>){
            Object temp = value;
            value = new LinkedList<Object>();
            ((LinkedList<Object>)value).add(temp);
        }

        if(!qkey.equals("") && value instanceof AbstractList<?>){
                Object temprequest = value;
                value = null;

                for(Object r : (AbstractList<?>)temprequest){
                    Object _value = GetValue.get(qkey, r, root, bookmark,inboundAttributes, index, context);
                     
                    if(_value != null && (_value.toString()).equals(qvalue)){ 
                        Object _result = GetValue.get(qreturn, r, root, bookmark,inboundAttributes, index, context);
                        if(!Objects.isNull(_result))
                            result.add(_result);
                    }
                    else {
                        value = null;
                    }
                }


            }

        switch(result.size()){
            case 0: return null;
            default: return result.get(0);
        }            
    }

}