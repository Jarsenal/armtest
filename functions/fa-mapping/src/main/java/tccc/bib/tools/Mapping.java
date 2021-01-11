package tccc.bib.tools;

import java.util.*;
import com.microsoft.azure.functions.ExecutionContext;


public class Mapping {

    @SuppressWarnings("unchecked")
    public static Object run(
            AbstractMap<String, Object> payload, 
            Object request, 
            AbstractMap<String, Object> inboundAttributes,
            Boolean test,
            final ExecutionContext context)
    throws java.lang.Exception{

        //context.getLogger().info("Entered run method.");

        Object map = null;
        Object root = request;
        Object bookmark = null;

        // check for test and do some test specific changes
        if(test){
            Transform.clearMaps();
        }

        
        if(payload.get("list") != null && (boolean)payload.get("list")){

            map = new LinkedList<Object>();

            Object nodeList = GetValue.get((String)payload.get("value"), request, root, bookmark, inboundAttributes, new IndexTracker(), context);     // GET VALUE

            if(nodeList instanceof AbstractMap<?,?>){
                AbstractMap<String, Object> temp = (AbstractMap<String, Object>)nodeList;
                nodeList = new LinkedList<AbstractMap<String, Object>>();
                ((LinkedList<AbstractMap<String, Object>>)nodeList).add(temp);
            }

            AbstractList<AbstractMap<String, Object>> whenThis =
                    (AbstractList<AbstractMap<String, Object>>)payload.get("when");

            IndexTracker index = new IndexTracker();

            for(Map<String, Object> item : (AbstractList<AbstractMap<String, Object>>)nodeList){

                index.incrementSource();

                if(whenThis != null
                    && whenThis.size() > 0
                    && !WhenThisCheck.check("AND", null, whenThis, item, null, "", root, bookmark, inboundAttributes, index, context))
                    continue;
                
                index.incrementTarget();
                
                ((AbstractList<Object>)map).add(
                    BuildObject.build((AbstractList<AbstractMap<String, Object>>)payload.get("nodes"), item, root, bookmark, inboundAttributes, index, context)
                );
                
            }

        }
        else {
            map = BuildObject.build((AbstractList<AbstractMap<String, Object>>)payload.get("nodes"), request, root, bookmark, inboundAttributes, new IndexTracker(), context );
        }

        //context.getLogger().info("Leaving run method.");

        return removeParents(map);
    }

    @SuppressWarnings("unchecked")
    private static Object removeParents(Object map){
        if(map instanceof AbstractList){
            for(Object item : (AbstractList<?>)map){
                item = removeParents(item);
            }
        } 
        else if(map instanceof AbstractMap){
            if(((AbstractMap<?,?>)map).containsKey("^^")){
                ((AbstractMap<?,?>)map).remove("^^");
            }
            
            for(String i : ((AbstractMap<String, Object>)map).keySet()){
                Object item = ((AbstractMap<String, Object>)map).get(i);
                if(item instanceof AbstractMap ||
                    item instanceof AbstractList){
                    ((AbstractMap<String, Object>)map)
                        .put(i,removeParents(item));
                }
            }
            
        }
        return map;
    }
    
}
