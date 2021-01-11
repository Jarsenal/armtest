package tccc.bib;

import org.junit.jupiter.api.Test;

import tccc.bib.methods.functions.*;
import tccc.bib.tools.IndexTracker;
import tccc.bib.tools.JsonToObject;
import tccc.bib.tools.MapReader;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.AbstractList;
import java.util.AbstractMap;
import java.util.logging.Logger;
import com.microsoft.azure.functions.*;
import static org.mockito.Mockito.*;
import tccc.bib.tools.Mapping;
import tccc.bib.transform.factories.CreateFromHashMap;
import tccc.bib.transform.factories.CreateToHashMap;
import tccc.bib.transform.interfaces.TransformFromHashMap;
import tccc.bib.transform.interfaces.TransformToHashMap;

import java.math.BigDecimal;


/**
 * Unit test for Function class.
 */
public class MappingTests {

    @Test
    public void testIndexTracker() throws Exception {

        // context
        final ExecutionContext context = mock(ExecutionContext.class);
        doReturn(Logger.getGlobal()).when(context).getLogger();

        // mapping config string
        String mappingConfig = "{\"name\":\"root\",\"value\":\"$$.test\",\"type\":\"OBJECT\",\"list\":true,\"when\":[{\"action\":\"STATEMENT\",\"loop\":\"\",\"left\":\"$$.val\",\"type\":\"NUMBER\",\"leftDateFormat\":\"\",\"op\":\"GREATER THAN\",\"right\":\"1\",\"rightDateFormat\":\"\",\"nodes\":[],\"$$hashKey\":\"object:152\"}],\"special\":[],\"listspecial\":[],\"nodes\":[{\"name\":\"targetIndex\",\"value\":\"$$.index_\",\"type\":\"SIMPLE\",\"list\":false,\"script\":\"\",\"when\":[],\"required\":false,\"ignore\":false,\"special\":[],\"listspecial\":[],\"nodes\":[]},{\"name\":\"sourceIndex\",\"value\":\"$$._index\",\"type\":\"SIMPLE\",\"list\":false,\"script\":\"\",\"when\":[],\"required\":false,\"ignore\":false,\"special\":[],\"listspecial\":[],\"nodes\":[]},{\"name\":\"value\",\"value\":\"$$.val\",\"type\":\"SIMPLE\",\"list\":false,\"script\":\"\",\"when\":[],\"required\":false,\"ignore\":false,\"special\":[],\"listspecial\":[],\"nodes\":[]}],\"source\":{\"header\":false,\"quotes\":false,\"delimiter\":\",\"},\"target\":{\"header\":false,\"quotes\":false,\"delimiter\":\",\"}}";
        String payload = "{\"test\":[{\"val\":1},{\"val\":2},{\"val\":3}]}";

        AbstractMap<String, Object> map = JsonToObject.from(mappingConfig);
        String type = "JSON";
        String responseType = "JSON";

        //properties
        AbstractMap<String, Object> properties = new LinkedHashMap<>();
        properties.put("sourceFF", "{\"newLine\":\"\\n\",\"schemas\":[]}");
        properties.put("targetFF", "{\"newLine\":\"\\n\",\"schemas\":[]}"); 
        properties.put("root", (String) map.get("name"));
        properties.put("sourceHeader", MapReader.read(map,"source.header"));
        properties.put("sourceQuotes", MapReader.read(map,"source.quotes"));
        properties.put("sourceDelimiter", MapReader.read(map,"source.delimiter"));
        properties.put("targetHeader", MapReader.read(map,"target.header")); 
        properties.put("targetQuotes", MapReader.read(map,"target.quotes")); 
        properties.put("targetDelimiter", MapReader.read(map,"target.delimiter"));

        // pull out needed transformers
        TransformToHashMap toMap = CreateToHashMap.getTransform(type);
        TransformFromHashMap fromMap = CreateFromHashMap.getTransform(responseType);

        AbstractMap<String,Object> inboundAttributes = new LinkedHashMap<>();
        
        Object _request = toMap.transform(payload, properties, context);

        // run main mapping function
        Object resultMapped = Mapping.run(map, _request, inboundAttributes, true, context);


        String response = fromMap.transform(resultMapped, properties, context);
        String expected = "[{\"targetIndex\":1,\"sourceIndex\":2,\"value\":2.0},{\"targetIndex\":2,\"sourceIndex\":3,\"value\":3.0}]";
            
        assertTrue(expected.equals(response),String.format("%s does not match %s",response.toString(),expected.toString()));
        
    }
}