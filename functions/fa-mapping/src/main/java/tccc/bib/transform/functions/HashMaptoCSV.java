package tccc.bib.transform.functions;

import tccc.bib.transform.interfaces.TransformFromHashMap;
import com.microsoft.azure.functions.ExecutionContext;

import java.util.AbstractMap;
import java.util.AbstractList;
import java.util.LinkedList;

public class HashMaptoCSV  implements TransformFromHashMap {

    @Override
    @SuppressWarnings("unchecked")
    public String transform(Object payload, AbstractMap<String, Object> properties, final ExecutionContext context)
            throws Exception {
        StringBuilder result = new StringBuilder();
        StringBuilder line = new StringBuilder();

        Boolean header = (Boolean)properties.get("targetHeader");
        Boolean quotes = (Boolean)properties.get("targetQuotes");
        String delimiter = (String)properties.get("targetDelimiter");

        AbstractList<AbstractMap<String, Object>> list = null;
        if(payload instanceof AbstractList){
            list = (AbstractList<AbstractMap<String, Object>>)payload;
        }
        else{
            list = new LinkedList<AbstractMap<String, Object>>();
            list.add((AbstractMap<String, Object>)payload);
        }


        if(header){

            AbstractMap<String, Object> item = list.get(0);

            for(String key : item.keySet()){
                if(!(line.length() == 0)) line.append(delimiter);
                if(quotes) line.append("\"");
                line.append(key);
                if(quotes) line.append("\"");
            }

            line.append("\n");
            result.append(line.toString());
            line.setLength(0);
        }

        for(AbstractMap<String, Object> item : list){

            for(String key : item.keySet()){
                if(!(line.length() == 0)) line.append(delimiter);
                if(quotes) line.append("\"");
                line.append(item.get(key).toString());
                if(quotes) line.append("\"");
            }

            line.append("\n");
            result.append(line.toString());
            line.setLength(0);
        }


        return result.toString();
    }


}
