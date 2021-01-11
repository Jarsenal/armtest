package tccc.bib.transform.functions;

//import org.eclipse.jetty.jndi.java.javaNameParser;
import tccc.bib.transform.interfaces.TransformToHashMap;
import com.microsoft.azure.functions.ExecutionContext;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.security.NoTypePermission;
import com.thoughtworks.xstream.security.NullPermission;
import com.thoughtworks.xstream.security.PrimitiveTypePermission;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import java.util.LinkedList;
import java.util.Collection;


public class XMLtoHashMap implements TransformToHashMap {

	public Object transform(String payload, AbstractMap<String, Object> properties, final ExecutionContext context)
			throws Exception {


		DocumentBuilderFactory fact =
				DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = fact.newDocumentBuilder();
		Document doc = builder.parse(new ByteArrayInputStream(payload.getBytes("UTF-8")));
		Node node = doc.getDocumentElement();
		String _root = node.getNodeName();

		String request = payload;
        XStream converter = new XStream();
        
        converter.addPermission(NoTypePermission.NONE);
        converter.addPermission(NullPermission.NULL);
        converter.addPermission(PrimitiveTypePermission.PRIMITIVES);
        converter.allowTypeHierarchy(Collection.class);
        converter.allowTypesByWildcard(new String[] {
            "tccc.bib.transform.*","java.util.*"
        });
        
        converter.registerConverter(new MapEntryConverter());
        converter.alias(_root, AbstractMap.class);
        return converter.fromXML(request);
    }
	
	@SuppressWarnings("rawtypes")
	public static class MapEntryConverter implements Converter {

        public boolean canConvert(Class clazz) {
            return AbstractMap.class.isAssignableFrom(clazz);
        }

        public void marshal(Object value, HierarchicalStreamWriter writer, MarshallingContext context) {
        	writer = writeObject(value, writer, context);
        }
        
        public HierarchicalStreamWriter writeObject(Object value, HierarchicalStreamWriter writer, MarshallingContext context){
        	
        	AbstractMap<?,?> map = (AbstractMap<?,?>) value;
            
            for (Object obj : map.entrySet()) {
                
            	Map.Entry<?,?> entry = (Map.Entry<?,?>) obj;
            	
                writer.startNode(entry.getKey().toString());
                
                Object val = entry.getValue();
                
                if(null != val){
	                if (val != null && (val instanceof HashMap)){
	                	writer = writeObject(val, writer, context);
	                }
	                else {
	                    writer.setValue(val.toString());
	                }
                }
                writer.endNode();
            }
        	return writer;
        }

        public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {

            return writeObject(reader, context);
        }
		
		@SuppressWarnings("unchecked")
        public Object writeObject(HierarchicalStreamReader reader, UnmarshallingContext context){
        	Map<String, Object> map = new LinkedHashMap<String, Object>();

        	if(reader.getAttributeCount() > 0){
        		Iterator<?> attributes = reader.getAttributeNames();
        		while(attributes.hasNext()){
					String name = (String)attributes.next();
					
					if(name.indexOf(":") > -1){
						name = name.substring(name.indexOf(":")+1);
					}

        			map.put("@" + name, reader.getAttribute(name));
        		}
        	}
        	        	
        	if(reader.hasMoreChildren()){
	        	while(reader.hasMoreChildren()) {
	                reader.moveDown();
	                
	                	String key = reader.getNodeName();
						Object value = writeObject(reader, context);
						
						if(key.indexOf(":") > -1){
							key = key.substring(key.indexOf(":")+1);
						}
	
	                	
	                	if(map.containsKey(key)){
	                		Object v = map.get(key);
	                		if(v instanceof LinkedList){
	                			((LinkedList<Object>)v).add(value);
	                			value = v;
	                		}
	                		else{
	                			LinkedList<Object> l = new LinkedList<Object>();
	                			l.add(v);
	                			l.add(value); 
	                			value = l;
	                		}
	                	} 
	
	                	map.put(key, value);
		                
	                	reader.moveUp();
	                }
	                
	                return map;
	            }
	
	        	
	        	
        	else {
        		if(map.isEmpty()){
        			return reader.getValue();
        		}
        		else {
        			 map.put("%", reader.getValue());
        			 return map;
        		}
        	}
        }

    }


}
