package tccc.bib.transform.functions;

import tccc.bib.transform.interfaces.TransformToHashMap;
import com.microsoft.azure.functions.ExecutionContext;

import java.util.*;

public class FFtoHashMap implements TransformToHashMap {
    @Override
    @SuppressWarnings("unchecked")
    public Object transform(String payload, AbstractMap<String, Object> properties, ExecutionContext context) throws Exception {

        AbstractMap<String, Object> config = (AbstractMap<String, Object>)properties.get("sourceFF");
        AbstractList<Object> schemas = (AbstractList<Object>)config.get("schemas");
        AbstractList<Object> schemaList = schemas;
        Stack<Object> schemaStack = new Stack<>();
        String[] lines = payload.split((String)config.get("newLine"));


        AbstractMap<String, Object> result = new LinkedHashMap<>();
        AbstractMap<String, Object> target = result;
        Stack<Object> targetStack = new Stack<>();

        for(String line : lines){

            if(line == "") continue;
            // add logic here to handle record type

            AbstractMap<String, Object> schema = null;
            Integer start = 0;
            schemaStack.push(schemaList);
            targetStack.push(target);

            while (schemaStack.size() > 0 && schema == null){
                schemaList = (AbstractList<Object>)schemaStack.pop();
                target = (AbstractMap<String, Object>)targetStack.pop();

                for(Object _schema : schemaList){
                    AbstractMap<String, Object> s = (AbstractMap<String, Object>)((AbstractMap<?,?>)_schema).get("key");

                    if(!(Boolean)s.get("enable")){
                        schema = (AbstractMap<String,Object>)_schema;
                        break;
                    }

                    if(line.indexOf((String)s.get("value")) == 0){
                        schema=(AbstractMap<String, Object>)_schema;
                        start = ((String)s.get("value")).length();
                        break;
                    }
                }
            }

            if(schema == null){
                schemaList = schemas;
                schemaStack.clear();
                target = result;
            }
            else {
                AbstractMap<String, Object> map = (AbstractMap<String,Object>)parseLine((AbstractList<Object>)schema.get("fields"), line, start);

                if(! target.containsKey((String)schema.get("name"))){
                    target.put((String)schema.get("name"), new LinkedList<>());
                }

                ((AbstractList<Object>)target.get((String)schema.get("name"))).add(map);
                targetStack.push(target);
                target = map;
                schemaStack.push(schemaList);
                schemaList = (AbstractList<Object>)schema.get("nodes");
            }

        }

        return result;
    }

    @SuppressWarnings("unchecked")
    private AbstractMap<String, Object> parseLine(AbstractList<Object> fields, String line, Integer start){
        java.util.LinkedHashMap<String, Object> map = new java.util.LinkedHashMap<String, Object>();
        Integer index = start;

        for(Object field : fields){
            AbstractMap<String, Object> _field = (AbstractMap<String, Object>)field;

            Integer length = Integer.parseInt((String)_field.get("length"));
            if(index < line.length()){
                map.put((String)_field.get("name"),
                        line.substring(index, Math.min(index + length, line.length())));
            }
            else {
                map.put((String)_field.get("name"), null);
            }
            index += length;
        }

        return map;
    }
}
