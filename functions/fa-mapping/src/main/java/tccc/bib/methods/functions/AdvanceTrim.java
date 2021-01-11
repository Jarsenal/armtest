package tccc.bib.methods.functions;

import com.microsoft.azure.functions.ExecutionContext;
import tccc.bib.methods.interfaces.Method;
import tccc.bib.tools.IndexTracker;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Objects;

public class AdvanceTrim implements  Method {

    @SuppressWarnings("unchecked")
    public Object run(Object value,Object request, Object root, Object bookmark, AbstractMap<String, Object> inboundAttributes, AbstractMap<String, Object> config, IndexTracker index, final ExecutionContext context )
    throws Exception 
    {
        
        AbstractMap<String, Object> trim = ( AbstractMap<String, Object>)config.get("advancetrim");
        String character = (String)trim.get("character");

        if(value instanceof String && !Objects.isNull(trim) && character.length() > 0 ){
            
            String _value = (String)value;

            if(trim.get("direction").equals("left") || 
               trim.get("direction").equals("both")) {
                while(_value.indexOf(character)==0){
                    _value = _value.substring(character.length());
                }
            }

            if(trim.get("direction").equals("right") || 
               trim.get("direction").equals("both")) {
                while(
                    _value.substring((_value.length()-character.length()),
                    (_value.length())).equals(character)){
                        _value = _value.substring(0,_value.length()-character.length());
                }
            }
            value = _value;
        }
        else if (value instanceof ArrayList<?>){

            ArrayList<String> result = new ArrayList<String>();

            ArrayList arr = (ArrayList)value;

            for (int i = 0; i < arr.size(); i++) {
                String _value = ((String)arr.get(i)).trim();

                if(trim.get("direction").equals("left") || 
                trim.get("direction").equals("both")) {

                    

                        while(_value.indexOf(character)==0){
                            _value = _value.substring(character.length());
                        }


                        
                    }

                
                

                if(trim.get("direction").equals("right") || 
                trim.get("direction").equals("both")) {


                        while(
                        _value.substring((_value.length()-character.length()),
                        (_value.length())).equals(character)){
                            _value = _value.substring(0,_value.length()-character.length());
                    }
                
                }
            result.add(_value);

            }



            value = result;

        }
        
        return value;
    }
}