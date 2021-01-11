package tccc.bib.tools;

import java.util.*;
import com.microsoft.azure.functions.ExecutionContext;
import groovy.lang.GroovyClassLoader;
import tccc.bib.methods.factories.MethodFactory;
import java.lang.reflect.Method;



public class Transform {

    static LinkedHashMap<String, Class> mapScripts;

    static {
        mapScripts = new LinkedHashMap<>();
    }

    static void clearMaps(){
        mapScripts.clear();
    }

    // publicnow
    @SuppressWarnings("unchecked")
    static Object transform(
        Object value,  
        AbstractMap<String, Object> node, 
        Object request,
        Object root, 
        Object bookmark,  
        AbstractMap<String, Object> inboundAttributes,
        IndexTracker index,
        final ExecutionContext context)
        throws java.lang.Exception {
            //context.getLogger().info("Entered transform method");

            // list of special functions that need to run
            AbstractList<AbstractMap<String,Object>> special = 
                (AbstractList<AbstractMap<String,Object>>)node.get("special");
    
             String script = (String)node.get("script");
            
             try {
    
    
                switch((String)node.get("type")) {
                    case "SIMPLE":
                        for(AbstractMap<String,Object> config : special){
                            //context.getLogger().info("Running special method: " + (String)config.get("type"));
    
                            value = MethodFactory.getMethod((String)config.get("type"))
                                .run(value, request, root, bookmark, inboundAttributes, config, index, context );
                        
                            //context.getLogger().info("End special method: " + (String)config.get("type"));
    
                        }
                        break;
                    case "OBJECT":
                        value = BuildObject.build((AbstractList<AbstractMap<String, Object>>)node.get("nodes"), value, root, bookmark, inboundAttributes, index, context);
                        break;
                    case "CUSTOM":

                        String clazzKey = String.format("%s_%d", (String)node.get("name"), Math.abs(script.hashCode()));
    
                        //context.getLogger().info(String.format("Inside custom: %s",clazzKey));

                        if(!(mapScripts.containsKey(clazzKey))){
                            //context.getLogger().info(String.format("Generating class for: %s",clazzKey));
                            GroovyClassLoader groovyClassLoader = new GroovyClassLoader();
                            String myClass = "class Custom" + UUID.randomUUID().toString().replaceAll("-","_") +
                                    " { \n " + " public static Object run(LinkedHashMap VARS) { \n " + script + " \n }} \n";
                            mapScripts.put(clazzKey, (Class)groovyClassLoader.parseClass(myClass));
                            groovyClassLoader.close();
                            //context.getLogger().info(String.format("End generating class for: %s",(String)node.get("name")));
                       }
    
                        Class myCustomClass = (Class)mapScripts.get(clazzKey);
                        Object myvars = BuildObject.build((AbstractList<AbstractMap<String, Object>>)node.get("nodes"), value, root, bookmark, inboundAttributes, index, context);
                        Method _method = myCustomClass.getMethod("run", LinkedHashMap.class);
                        //context.getLogger().info(String.format("Got method for: %s",clazzKey));
                        value = _method.invoke(null, (AbstractMap<String, Object>)myvars);
                        //context.getLogger().info(String.format("Just ran method for: %s",clazzKey));
                        
                        break;
                }
    
    
            }
            catch (Exception ex){
                throw new Exception("Failed at field <" + (String)node.get("name") + "> with exception: " + ex.toString());
            }
    
            //context.getLogger().info("Leaving transform method.");
    
            return value;
    }

}
