package tccc.bib.methods.functions;

import com.microsoft.azure.functions.ExecutionContext;
import tccc.bib.methods.interfaces.Method;
import java.util.AbstractList;
import java.util.LinkedList;
import java.util.AbstractMap;
import java.util.LinkedHashMap;
import tccc.bib.tools.GetValue;
import tccc.bib.tools.IndexTracker;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class ConcatList implements  Method {
    @SuppressWarnings("unchecked")
    public Object run(Object value,Object request, Object root, Object bookmark, AbstractMap<String, Object> inboundAttributes, AbstractMap<String, Object> config, IndexTracker index, final ExecutionContext context )
            throws Exception 
            {
                AbstractMap<String, String> ziplist = (AbstractMap<String, String>)config.get("concatlist");


                // not a list, let's ignore this function
                if(value != null && !(value instanceof java.util.AbstractList<?>)){
                    return value;
                }

                Object concatme = GetValue.get(ziplist.get("list"),request,root,bookmark,inboundAttributes, index, context);
                
                if(!(concatme instanceof AbstractList)){
                    LinkedList<Object> temp = new LinkedList<>();
                    temp.add(concatme);
                    concatme = temp;
                }


                    ((AbstractList)value).addAll((AbstractList)concatme);
                
                return value;
            }

}