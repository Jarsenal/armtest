package tccc.bib.tools;

import java.util.*;
import com.microsoft.azure.functions.ExecutionContext;
import tccc.bib.methods.factories.MethodFactory;


public class BuildObject {
 
    @SuppressWarnings("unchecked")
    static Object build(
        AbstractList<AbstractMap<String, Object>> nodes, 
        Object request, 
        Object root,
        Object bookmark,
        AbstractMap<String, Object> inboundAttributes,
        IndexTracker index,
        final ExecutionContext context)
    throws java.lang.Exception {

        //context.getLogger().info("Entering buildObject method.");

        java.util.LinkedHashMap<String, Object> newmap = new LinkedHashMap<>();

        for(AbstractMap<String, Object> node : nodes){
            AbstractList<AbstractMap<String, Object>> listspecial = (AbstractList<AbstractMap<String, Object>>)node.get("listspecial");

            AbstractList<AbstractMap<String, Object>> whenThis =
                    (AbstractList<AbstractMap<String, Object>>)node.get("when");

            Object mapValue = null;

            if((Boolean)node.get("list")){

                AbstractList<Object> list = new LinkedList<>();

                Object nodeList = null;

                if(((String)node.get("value")).indexOf("$$") > -1 ||
                        ((String)node.get("value")).indexOf("&&") > -1 ||
                        ((String)node.get("value")).indexOf("%%") > -1 ||
                        ((String)node.get("value")).indexOf("@@") > -1){
                    nodeList = GetValue.get(((String)node.get("value")), request, root, bookmark, inboundAttributes, index, context);                        // GET VALUE WITH X and Q
                }

                if((Boolean)node.get("ignore")  && 
                    (Objects.isNull(nodeList) || 
                    (nodeList instanceof AbstractList && 
                    ((AbstractList)nodeList).size() == 0)))  {
                    continue;
                }

                for(AbstractMap<String,Object> config : listspecial){
                        if(listspecial != null){
                        //context.getLogger().info("Running list special method: " + (String)config.get("type"));

                        nodeList = MethodFactory.getMethod((String)config.get("type"))
                            .run(nodeList, request, root, bookmark, inboundAttributes, config, index, context );
                    
                        //context.getLogger().info("End list special method: " + (String)config.get("type"));
                    }
                }

                // make it a list if it is not
                if(nodeList != null && !(nodeList instanceof java.util.AbstractList<?>)){
                    Object temp = nodeList;
                    nodeList = new LinkedList<Object>();
                    ((AbstractList<Object>)nodeList).add(temp);
                }

                IndexTracker _index = new IndexTracker();
                if(nodeList != null){
                    for(Object item : ((AbstractList<Object>)nodeList)){

                        _index.incrementSource();
                        
                        if(whenThis != null
                                && whenThis.size() > 0
                                && !WhenThisCheck.check("AND", node, whenThis, item, null, "", root, bookmark, inboundAttributes, index, context))
                            continue;
                        
                        _index.incrementTarget();

                        list.add(Transform.transform(item, node, request, root, bookmark, inboundAttributes, _index, context));
                        
                    }

                    mapValue = list;
                }
                else {
                    mapValue = null;
                }
            }
            else{

                if(whenThis != null
                        && whenThis.size() > 0
                        && !WhenThisCheck.check("AND", node, whenThis, request, null, "", root, bookmark, inboundAttributes, index, context))
                    continue;

                if(((String)node.get("value")).indexOf("$$") > -1 ||
                        ((String)node.get("value")).indexOf("&&") > -1 ||
                        ((String)node.get("value")).indexOf("%%") > -1 ||
                        ((String)node.get("value")).indexOf("@@") > -1){

                    if(((String)node.get("value")).indexOf("$$") == 0 ||
                            ((String)node.get("value")).indexOf("&&") == 0 ||
                            ((String)node.get("value")).indexOf("%%") == 0 ||
                            ((String)node.get("value")).indexOf("@@") == 0){
                        mapValue = GetValue.get((String)node.get("value"),	request, root, bookmark, inboundAttributes, index, context);                         // GET VALUE with X and Q
                    }
                    
                }
                else{
                    mapValue = ((!(((String)(node.get("type"))).equals("OBJECT")) && !(((String)node.get("type")).equals("CUSTOM")))?node.get("value"):request);
                }
                
                if((Boolean)node.get("ignore")  && (mapValue == null || (mapValue instanceof String && ((String)mapValue).isEmpty())))  {
                    continue;
                }

                if(mapValue instanceof AbstractList){
                    for(AbstractMap<String,Object> config : listspecial){
                            if(listspecial != null){
                            //context.getLogger().info("Running list special method: " + (String)config.get("type"));

                            mapValue = MethodFactory.getMethod((String)config.get("type"))
                                .run(mapValue, request, root, bookmark, inboundAttributes, config, index, context );
                        
                            //context.getLogger().info("End list special method: " + (String)config.get("type"));
                        }
                    }
                }

                mapValue = Transform.transform(mapValue, node, request, root, bookmark, inboundAttributes, index, context);

                if((Boolean)node.get("required") && mapValue == null){
                    throw new Exception("Required field is missing: " + (String)node.get("name"));
                }
                
            }

            if(newmap.containsKey(node.get("name"))){
                Object item = newmap.get(node.get("name"));

                if(item instanceof java.util.AbstractList){
                    if(mapValue instanceof AbstractList){
                        ((AbstractList<Object>)item).addAll((AbstractList<?>)mapValue);
                        newmap.put((String)node.get("name"),item);
                    }
                    else {
                        ((AbstractList<Object>)item).add(mapValue);
                        newmap.put((String)node.get("name"),item);
                    }
                }
                else {
                    AbstractList<Object> list = new LinkedList<>();
                    list.add(item);
                    if(mapValue instanceof AbstractList){
                        list.add((AbstractList<?>)mapValue);
                    }
                    else {
                        list.add(mapValue);
                    }
                    newmap.put((String)node.get("name"),list);
                }
            }
            else {
                newmap.put((String)node.get("name"), mapValue);
            }
        }

        //context.getLogger().info("Leaving buildObject method.");


        return newmap;
    }
}
