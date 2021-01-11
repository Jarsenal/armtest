package tccc.bib.main;

import java.util.*;
import tccc.bib.tools.*;

import tccc.bib.transform.factories.CreateFromHashMap;
import tccc.bib.transform.factories.CreateToHashMap;
import tccc.bib.transform.interfaces.TransformFromHashMap;
import tccc.bib.transform.interfaces.TransformToHashMap;
import com.microsoft.azure.functions.annotation.*;
import com.microsoft.azure.functions.*;

public class MappingTest {
    
    @FunctionName("RunMappingTest")
    @SuppressWarnings("unchecked")
    public HttpResponseMessage run(
            @HttpTrigger(name = "req", methods = {HttpMethod.POST}, authLevel = AuthorizationLevel.FUNCTION)
                    HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {

        context.getLogger().info("Running Mapping Test");

        AbstractMap<String, String> contentTypes = new HashMap<>();
            contentTypes.put("JSON", "application/json;charset=UTF-8");
            contentTypes.put("XML", "application/xml;charset=UTF-8");
            contentTypes.put("CSV", "application/csv;charset=UTF-8");
            contentTypes.put("FF", "data/text;charset=UTF-8");

        String body = request.getBody().orElse("");

        if(body.isEmpty()){
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body("Request body is missing!")
                    .build();
        }

        
        //////////////////////////////////////////////////////////////////////
        // START: extract request properties from payload request
        //////////////////////////////////////////////////////////////////////

        AbstractMap<String, Object> mapConfig = JsonToObject.from(body);
        AbstractMap<String, Object> map = JsonToObject.from((String) mapConfig.get("mapping"));
        AbstractMap<String, Object> attmap = JsonToObject.from((String) mapConfig.get("mappingProperty"));
        AbstractMap<String, Object> inboundAttributes = 
            Optional.ofNullable((AbstractMap<String, Object>)mapConfig.get("payloadProperty"))
            .orElse(new LinkedHashMap<String, Object>());
        
        String type = (String) mapConfig.get("type");
        String responseType = (String) mapConfig.get("response");

        String payload = (String) mapConfig.get("payload");

        Boolean test = Optional.ofNullable((Boolean)mapConfig.get("test"))
                                    .orElse(false);
            
        //if(mapConfig.containsKey("test")) test = (Boolean) mapConfig.get("test");

        ////////////////////////////////////////////////////////
        // properties for pre and post mapping transformation
        AbstractMap<String, Object> properties = new LinkedHashMap<>();
        properties.put("sourceFF", JsonToObject.from((String) mapConfig.get("sourceFF")));
        properties.put("targetFF", JsonToObject.from((String) mapConfig.get("targetFF")));
        properties.put("root", MapReader.read(map,"name"));
        properties.put("sourceHeader", MapReader.read(map,"source.header"));
        properties.put("sourceQuotes", MapReader.read(map,"source.quotes"));
        properties.put("sourceDelimiter", MapReader.read(map,"source.delimiter"));
        properties.put("targetHeader", MapReader.read(map,"target.header")); 
        properties.put("targetQuotes", MapReader.read(map,"target.quotes")); 
        properties.put("targetDelimiter", MapReader.read(map,"target.delimiter"));
        String contenttype = contentTypes.get(responseType);
        
        //////////////////////////////////////////////////////////////////////
        // END: extract request properties from payload request
        //////////////////////////////////////////////////////////////////////


        // initialize response and initial transform request
        String response = "";
        String outboundAttributes = "";
        Object _request = null;

        // pull out needed transformers
        TransformToHashMap toMap = CreateToHashMap.getTransform(type);
        TransformFromHashMap fromMap = CreateFromHashMap.getTransform(responseType);
    
        try {
        
            
            if (toMap != null) _request = toMap.transform(payload, properties, context);
            else return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(String.format("THIS SOURCE TYPE HAS NOT BEEN DEVELOPED YET: " + type))
                    .build();

            context.getLogger().info("Start mapping service.");

            // run main mapping function
            Object resultMapped = Mapping.run(map, _request, inboundAttributes, test, context);
            AbstractMap<String,Object> _atts = (AbstractMap<String, Object>)Mapping.run(attmap, _request, inboundAttributes, test, context);


            for(String k : inboundAttributes.keySet()){
                    if(!_atts.containsKey(k))
                        _atts.put(k,inboundAttributes.get(k));
            }
            
            outboundAttributes = CreateFromHashMap
                                        .getTransform("JSON")
                                        .transform(_atts, null, context);
            
            if (fromMap != null) response = fromMap.transform(resultMapped, properties, context);
            else return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(String.format("THIS TARGET TYPE HAS NOT BEEN DEVELOPED YET: " + responseType))
                    .build();

        } catch (Exception e) {
            e.printStackTrace();
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(e.getMessage())
                    .build();
        }

        context.getLogger().info("End mapping service.");

        return request.createResponseBuilder(HttpStatus.OK)
                .header("Content-Type", contenttype)
                .header("attributes", outboundAttributes)
                .header("Access-Control-Expose-Headers","attributes")
                .body(response)
                .build();

    }

  
}