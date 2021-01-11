

package tccc.bib.transform.functions;

//import org.eclipse.jetty.jndi.java.javaNameParser;
import tccc.bib.transform.interfaces.TransformFromHashMap;
import com.microsoft.azure.functions.ExecutionContext;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.io.xml.Dom4JDriver;
import com.thoughtworks.xstream.io.xml.XmlFriendlyNameCoder;
import com.thoughtworks.xstream.security.NoTypePermission;
import com.thoughtworks.xstream.security.NullPermission;
import com.thoughtworks.xstream.security.PrimitiveTypePermission;

import java.util.AbstractMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Collection;
import java.util.Map;


public class HashMaptoXML implements TransformFromHashMap {
    
	@Override
	public String transform(Object payload, AbstractMap<String, Object> properties, final ExecutionContext context)
			throws Exception {

		XmlFriendlyNameCoder nameCoder = new XmlFriendlyNameCoder("ddd", "_");
		XStream converter = new XStream(new Dom4JDriver(nameCoder));
		converter.registerConverter(new MapEntryConverter());
		converter.alias((String)properties.get("root"), LinkedHashMap.class);

		converter.addPermission(NoTypePermission.NONE);
		converter.addPermission(NullPermission.NULL);
		converter.addPermission(PrimitiveTypePermission.PRIMITIVES);
		converter.allowTypeHierarchy(Collection.class);
		converter.allowTypesByWildcard(new String[] {
				"tccc.bib.transform.*","java.util.*"
		});

		String xml = converter.toXML(payload);

		return xml;
	}

	@SuppressWarnings("rawtypes")
	public static class MapEntryConverter implements Converter {

        public boolean canConvert(Class clazz) {
            return AbstractMap.class.isAssignableFrom(clazz);
        }

        public void marshal(Object value, HierarchicalStreamWriter writer, MarshallingContext context) {
        	writer = writeObject(value, writer, context);
        }
		
		@SuppressWarnings("unchecked")
        public HierarchicalStreamWriter writeObject(Object value, HierarchicalStreamWriter writer, MarshallingContext context){
        	
        	LinkedHashMap<String, Object> map = (LinkedHashMap<String, Object>) value;
        	
        	// attributes
        	writer = writeAttributes(map, writer, context);

        	// check to make sure % exists or not
        	if(map.containsKey("%")){
        		Object val = map.get("%");
        		
        		if(val instanceof String){
        			writer.setValue(val.toString());
        		} 
        		else if(val instanceof LinkedList){
        			String concat = "";
        			for(Object obji : (LinkedList<Object>)val){
                		if(obji instanceof String){
                			concat += obji.toString();
                		}
                		writer.setValue(concat.toString());
                	}
        		}
        		else { // assume hashmap
        			writer = writeObject(val, writer, context);
        		}
        		
        		
        		
        		
        		return writer;
        	}
        	
            // nodes and value
            for (Object obj : map.entrySet()) {
                
            	Map.Entry<?,?> entry = (Map.Entry<?,?>) obj;
            	Object val = entry.getValue();
            	
            	if(entry.getKey().toString().indexOf('@') == 0) continue;
                
            	
                
                if(null != val){
	                if (val instanceof LinkedHashMap){
	                	writer.startNode(entry.getKey().toString());
	                    writer = writeObject(val, writer, context);
	                    writer.endNode();
	                }
	                else if(val instanceof LinkedList){
	                	for(Object obji : (LinkedList<Object>)val){
	                		
	                		if(obji == null) continue;
	                		
	                		if(obji instanceof LinkedHashMap){
	                			writer.startNode(entry.getKey().toString());
	    	                    writer = writeObject(obji, writer, context);
	    	                    writer.endNode();
	                		}
	                		else {
	                			writer.startNode(entry.getKey().toString());
	    	                    writer.setValue(obji.toString());
	    	                    writer.endNode();
	                		}
	                	}
	                }
	                else {
	                	writer.startNode(entry.getKey().toString());
	                    writer.setValue(val.toString());
	                    writer.endNode();
	                }
                }
                
            }
        	return writer;
        }
        
        public HierarchicalStreamWriter writeAttributes(Object value, HierarchicalStreamWriter writer, MarshallingContext context){
        
        	LinkedHashMap<?,?> map = (LinkedHashMap<?,?>) value;
        	
        	for (Object obj : map.entrySet()) {
            	Map.Entry<?,?> entry = (Map.Entry<?,?>) obj;
                Object val = entry.getValue();
                String key = entry.getKey().toString();
                if(key.indexOf('@') != 0) continue;
                writer.addAttribute(key.replaceAll("^@", ""), val.toString());
            }
        	return writer;
        }
        

        public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {

            Map<String, String> map = new LinkedHashMap<String, String>();

            while(reader.hasMoreChildren()) {
                reader.moveDown();

                String key = reader.getNodeName(); // nodeName aka element's name
                String value = reader.getValue();
                
                map.put(key, value);

                reader.moveUp();
            }

            return map;
        }

    }


}
