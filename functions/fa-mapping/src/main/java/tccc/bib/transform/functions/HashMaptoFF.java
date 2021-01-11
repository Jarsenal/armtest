package tccc.bib.transform.functions;

import tccc.bib.transform.interfaces.TransformFromHashMap;
import com.microsoft.azure.functions.ExecutionContext;

import java.util.AbstractList;
import java.util.AbstractMap;

public class HashMaptoFF implements TransformFromHashMap{
    @Override
    @SuppressWarnings("unchecked")
    public String transform(Object payload, AbstractMap<String, Object> properties, ExecutionContext context) throws Exception {

        String result = "";

        AbstractMap<String, Object> schema = (AbstractMap<String, Object>)properties.get("targetFF");
        String lineDelimiter = (String)schema.get("newLine");

        if(payload instanceof AbstractList){
            for(Object item : (AbstractList<?>)payload){
                result += buildLine((AbstractList<AbstractMap<String, Object>>)schema.get("schemas"),
                        (AbstractMap<String, Object>)item, lineDelimiter);
            }
        }
        else {
            result += buildLine((AbstractList<AbstractMap<String, Object>>)schema.get("schemas"),
                    (AbstractMap<String, Object>)payload, lineDelimiter);
        }
        return result;

    }

    @SuppressWarnings("unchecked")
    private String buildLine(AbstractList<AbstractMap<String, Object>> nodes, AbstractMap<String, Object> map, String lineDelimiter){
        String line = "";
        
        for(AbstractMap<String, Object> node : nodes){
            if(map.get((String)node.get("name")) != null){
                for(Object item : (AbstractList<Object>)map.get((String)node.get("name"))){
                    AbstractMap<String, Object> _item = (AbstractMap<String, Object>)item;

                    if((Boolean)((AbstractMap<String, Object>)node.get("key")).get("enable")){
                        line += (String)((AbstractMap<String, Object>)node.get("key")).get("value");
                    }

                    for(Object field : (AbstractList<Object>)node.get("fields")){
                        AbstractMap<String, Object> _field = (AbstractMap<String, Object>)field;

                        if(_item.get((String)_field.get("name")) == null) continue;

                        Integer length = Integer.parseInt((String)_field.get("length"));
                        String value = (String)_item.get((String)_field.get("name"));

                        if(value.length() <= length){
                            switch((String)_field.get("placement")){
                                case "LEFT":
                                    line += padRight(value,length, ((String)_field.get("padding")).isEmpty()?" ":(String)_field.get("padding"));
                                    break;
                                case "CENTER":
                                    String temp = padRight(value,((length-value.length())/2)+value.length(), ((String)_field.get("padding")).isEmpty()?" ":(String)_field.get("padding"));
                                    line += padLeft(temp,length, ((String)_field.get("padding")).isEmpty()?" ":(String)_field.get("padding"));
                                    break;
                                default:
                                    line += padLeft(value,length, ((String)_field.get("padding")).isEmpty()?" ":(String)_field.get("padding"));
                                    break;
                            }
                        }
                        else {
                            line += value.substring(0,length);
                        }
                    }
                    line += lineDelimiter;
                    line += buildLine((AbstractList<AbstractMap<String,Object>>) node.get("nodes"), _item, lineDelimiter);
                }
            }
        }
        return line;
    }

    private String padRight(String s, int size, String pad) {
        StringBuilder builder = new StringBuilder(s);
        while(builder.length()<size) {
            builder.append(pad);
        }
        return builder.toString();
    }

    private String padLeft(String s, int size, String pad) {
        StringBuilder builder = new StringBuilder(s);
        builder = builder.reverse(); // reverse initial string
        while(builder.length()<size) {
            builder.append(pad); // append at end
        }
        return builder.reverse().toString(); // reverse again!
    }
}
