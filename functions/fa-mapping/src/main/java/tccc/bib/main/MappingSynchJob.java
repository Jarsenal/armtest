package tccc.bib.main;

import java.util.*;
import tccc.bib.tools.*;

import tccc.bib.transform.factories.CreateFromHashMap;
import tccc.bib.transform.factories.CreateToHashMap;
import tccc.bib.transform.interfaces.TransformFromHashMap;
import tccc.bib.transform.interfaces.TransformToHashMap;
import com.microsoft.azure.functions.annotation.*;
import com.microsoft.azure.functions.*;

public class MappingSynchJob {
    
    @FunctionName("RunMappingJob")
    @StorageAccount("BLOBSTORAGE")
    public HttpResponseMessage run(
            @HttpTrigger(name = "req", methods = {HttpMethod.GET}, authLevel = AuthorizationLevel.FUNCTION)
                    HttpRequestMessage<Optional<String>> request,
            @BlobInput(
                    name = "sourceInput", 
                    path = "{source}") String inputFile,
            @BlobOutput(
                    name = "targetOutput", 
                    path = "{target}") OutputBinding<String> outputFile,
            final ExecutionContext context) {


        context.getLogger().info(String.format("Logic App Run Id: %s", request.getHeaders().get("x-ms-workflow-run-id")));
        context.getLogger().info(String.format("Transaction Id: %s", request.getHeaders().get("tid")));
        context.getLogger().info(String.format("Reprocess: %s   --  Action: %s",request.getHeaders().get("reprocesscount"),request.getHeaders().get("actioncount")));

        Integer actionCount = Integer.parseInt(request.getHeaders().get("actioncount"));
        Integer reprocessCount = Integer.parseInt(request.getHeaders().get("reprocesscount"));

        String event = request.getHeaders().get("event");
        String transactionId = request.getHeaders().get("tid");
        String target = request.getQueryParameters().get("target");
        String run = request.getHeaders().get("x-ms-workflow-run-id");
        String flow = request.getHeaders().get("x-ms-workflow-name");
        String action = "Mapping job";

        AbstractMap<String, String> contentTypes = new HashMap<>();
            contentTypes.put("JSON", "application/json;charset=UTF-8");
            contentTypes.put("XML", "application/xml;charset=UTF-8");
            contentTypes.put("CSV", "application/csv;charset=UTF-8");
            contentTypes.put("FF", "data/text;charset=UTF-8");

        String payload = inputFile;    
        String attributes = Optional.ofNullable(request.getHeaders().get("attributes")).orElse("{}");

        if(Objects.isNull(payload)){

                PutLogs.log(transactionId, "Failed", action, target, attributes, event,
                request.getHeaders().get("x-ms-workflow-run-id"),
                request.getHeaders().get("x-ms-workflow-name"),
                "Payload came in as null.  Please confirm the source file exists.",
                actionCount, reprocessCount,context);

                context.getLogger().severe(request.getBody().orElse("Payload is missing!"));
                
                return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                        .header("attributes", attributes)
                        .body("Payload came in as null.  Please confirm the source file exists.")
                        .build();
        }
      
        //////////////////////////////////////////////////////////////////////
        // START: extract request properties from payload request
        //////////////////////////////////////////////////////////////////////

        AbstractMap<String, Object> mapConfig = GetConfig.pull(request.getQueryParameters().get("event"));
        if(mapConfig.containsKey("payload")) mapConfig.remove("payload");
        AbstractMap<String, Object> attmap = JsonToObject.from((String) mapConfig.get("mappingProperty"));
        AbstractMap<String, Object> map = JsonToObject.from((String) mapConfig.get("mapping"));
        String type = (String) mapConfig.get("type");
        String responseType = (String) mapConfig.get("response");
        Boolean test = Optional.ofNullable((Boolean)mapConfig.get("test")).orElse(false);

        AbstractMap<String, Object> properties = new LinkedHashMap<>();
        properties.put("sourceFF", JsonToObject.from((String) mapConfig.get("sourceFF")));
        properties.put("targetFF", JsonToObject.from((String) mapConfig.get("targetFF"))); 
        properties.put("root", (String) map.get("name"));
        properties.put("sourceHeader", MapReader.read(map,"source.header"));
        properties.put("sourceQuotes", MapReader.read(map,"source.quotes"));
        properties.put("sourceDelimiter", MapReader.read(map,"source.delimiter"));
        properties.put("targetHeader", MapReader.read(map,"target.header")); 
        properties.put("targetQuotes", MapReader.read(map,"target.quotes")); 
        properties.put("targetDelimiter", MapReader.read(map,"target.delimiter"));

        //////////////////////////////////////////////////////////////////////
        // END: extract request properties from payload request
        //////////////////////////////////////////////////////////////////////


        // initialize response and initial transform request
        String response = "";
        String outboundAttributes = attributes;
        Object _request = null;

        // pull out needed transformers
        TransformToHashMap toMap = CreateToHashMap.getTransform(type);
        TransformFromHashMap fromMap = CreateFromHashMap.getTransform(responseType);

        try {
                
                AbstractMap<String,Object> inboundAttributes = null;
                if(attributes != null)
                        inboundAttributes = (AbstractMap<String,Object>)CreateToHashMap
                                .getTransform("JSON")
                                .transform(attributes, null, context);

            if (toMap != null) _request = toMap.transform(payload, properties, context);
            else throw new Exception (String.format("THIS SOURCE TYPE HAS NOT BEEN DEVELOPED YET: " + type));
            
            context.getLogger().info("Start mapping service.");

            // run main mapping function
            Object resultMapped = Mapping.run(map, _request, inboundAttributes, test, context);
            AbstractMap<String,Object> _atts = (AbstractMap<String, Object>)Mapping.run(attmap, _request, inboundAttributes, test, context);

            // carry inbound attributes with outbound
            if((inboundAttributes) != null){
                for(String k : inboundAttributes.keySet()){
                        if(!_atts.containsKey(k))
                        _atts.put(k,inboundAttributes.get(k));
                }
            }


            outboundAttributes = CreateFromHashMap
                                        .getTransform("JSON")
                                        .transform(_atts, null, context);
            

            if (fromMap != null) response = fromMap.transform(resultMapped, properties, context);
            else  throw new Exception (String.format("THIS TARGET TYPE HAS NOT BEEN DEVELOPED YET: " + type));

        } catch (Exception e) {
            e.printStackTrace();

            PutLogs.log(transactionId, "Failed", 
			action, target, attributes, 
                        event, run, flow, e.getMessage(),
                        actionCount, reprocessCount,context);
                        
                StringBuilder builder = new StringBuilder();
                for (StackTraceElement element : e.getStackTrace()) {
                        builder.append(element.toString());
                        builder.append("\n");
                }

                response = builder.toString();

            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .header("attributes", outboundAttributes)
                    .body(e.getMessage())
                    .build();
        }

        context.getLogger().info("End mapping service.");

        PutLogs.log(transactionId, "Succeeded", 
			action, target, attributes, 
			event, run, flow, "OK",
                        actionCount, reprocessCount,context);
        
        outputFile.setValue(response);
        return request.createResponseBuilder(HttpStatus.OK)
        .header("attributes", outboundAttributes)
        .body("OK")
        .build();

    }


  
}