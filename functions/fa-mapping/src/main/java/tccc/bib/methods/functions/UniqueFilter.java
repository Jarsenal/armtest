package tccc.bib.methods.functions;

import com.microsoft.azure.functions.ExecutionContext;
import tccc.bib.methods.interfaces.Method;
import java.util.AbstractList;
import java.util.LinkedList;
import java.util.Objects;
import java.util.AbstractMap;
import java.util.LinkedHashMap;
import tccc.bib.tools.GetValue;
import tccc.bib.tools.IndexTracker;

public class UniqueFilter implements  Method {
    @SuppressWarnings("unchecked")
    public Object run(Object value,Object request, Object root, Object bookmark, AbstractMap<String, Object> inboundAttributes, AbstractMap<String, Object> config, IndexTracker index, final ExecutionContext context )
            throws Exception 
            {
                AbstractList<AbstractMap<String, String>> list = (AbstractList<AbstractMap<String, String>>)config.get("uniquefilter");


                // not a list, let's ignore this function
                if(value != null && !(value instanceof java.util.AbstractList<?>)){
                    return value;
                }

                LinkedList<Object> newList = new LinkedList<>();
                LinkedHashMap<String,String> keys = new LinkedHashMap<>();

                for(Object row : (AbstractList)value){
                    if(Objects.isNull(row)) continue;
                    String key = "";
                    for(AbstractMap<String, String> item : list){
                        key += (GetValue.get(item.get("value"),row,root,bookmark,inboundAttributes,index,context)).toString();
                    }
                    if(keys.containsKey(key)) continue;
                    keys.put(key,key);
                    newList.add(row);
                }

                return newList;
            }

}