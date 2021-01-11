package tccc.bib.methods.functions;

import com.microsoft.azure.functions.ExecutionContext;
import tccc.bib.methods.interfaces.Method;
import java.util.AbstractList;
import java.util.LinkedList;
import java.util.AbstractMap;
import java.util.LinkedHashMap;
import tccc.bib.tools.GetValue;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import tccc.bib.tools.IndexTracker;

public class ZipList implements  Method {
    @SuppressWarnings("unchecked")
    public Object run(Object value,Object request, Object root, Object bookmark, AbstractMap<String, Object> inboundAttributes, AbstractMap<String, Object> config, IndexTracker index, final ExecutionContext context )
            throws Exception 
            {
                AbstractMap<String, String> ziplist = (AbstractMap<String, String>)config.get("ziplist");


                // not a list, let's ignore this function
                if(value != null && !(value instanceof java.util.AbstractList<?>)){
                    return value;
                }

                LinkedList<Object> newList = new LinkedList<>();
                
                Object zipme = GetValue.get(ziplist.get("list"),request,root,bookmark,inboundAttributes,index,context);
                String name = ziplist.get("name");

                 // make it a list if it is not
                 if(zipme != null && !(zipme instanceof java.util.AbstractList<?>)){
                    Object temp = zipme;
                    zipme = new LinkedList<Object>();
                    ((AbstractList<Object>)zipme).add(temp);
                }

                // make sure we have something to zip
                // otherwise just return the returned item
                if(((AbstractList<Object>)zipme).size() == 0){
                    return value;
                }

                Gson gson = new Gson();

                for(Object row : (AbstractList)value){
                    String jsonString = gson.toJson(row);
                    
                    for(Object item : (AbstractList<Object>)zipme){
                        LinkedHashMap<String, Object> newitem = null;                        
                        if(!(row instanceof AbstractMap)){
                            newitem = new LinkedHashMap<>();
                            newitem.put("value",row);
                        }
                        else {
                            newitem = gson.fromJson(jsonString, new TypeToken<LinkedHashMap<String, Object>>(){}.getType());
                        }
                        newitem.put(name,item);
                        newList.add(newitem);
                    }
                }
                return newList;
            }

}