package tccc.bib.methods.functions;

import java.util.AbstractMap;

import com.microsoft.azure.functions.ExecutionContext;
import tccc.bib.tools.IndexTracker;

import tccc.bib.methods.interfaces.Method;

public class Substring implements  Method {

    @SuppressWarnings("unchecked")
    public Object run(Object value,Object request, Object root, Object bookmark, AbstractMap<String, Object> inboundAttributes, AbstractMap<String, Object> config, IndexTracker index, final ExecutionContext context )
            throws Exception 
    {
     
        AbstractMap<String, Object> config_ = (AbstractMap<String, Object>)config.get("substring");
        Integer length = ((String)value).length();
        Integer grabLength = Integer.parseInt((String)config_.get("length"));
        Integer _index = Integer.parseInt((String)config_.get("start"));

        if(_index < 0){
            _index = length + _index;
        }

        if(!config_.containsKey("length") || ((String)config_.get("length")).isEmpty()){
            return ((String)value).substring(_index);
        }
        else {
            if(length > (_index + grabLength)){
                return ((String)value).substring(_index, _index + grabLength);
            } 
            else {
                return ((String)value).substring(_index);
            }
        }
    }

}