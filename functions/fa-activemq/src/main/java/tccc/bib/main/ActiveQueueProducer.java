package tccc.bib.main;

import java.util.*;

import tccc.bib.help.GetConfig;
import tccc.bib.help.Route;
import tccc.bib.objects.Request;
import tccc.bib.objects.Response;
import tccc.bib.help.PutLogs;

import com.microsoft.azure.functions.annotation.*;
import com.microsoft.azure.functions.*;

public class ActiveQueueProducer {
	
    @FunctionName("activemq-jms-queue-producer")
    @StorageAccount("BLOBSTORAGE")
    public HttpResponseMessage run(
            @HttpTrigger(
                    name = "req", 
                    methods = {HttpMethod.POST}, 
                    authLevel = AuthorizationLevel.FUNCTION) 
                    HttpRequestMessage<Optional<String>> request,
                    @BlobInput(
                        name = "sourceInput", 
                        path = "{source}") 
                        String inputFile,
                    @BlobOutput(
                        name = "targetOutput", 
                        path = "{target}") 
                        OutputBinding<String> outputFile,
            final ExecutionContext context) {
    	
		context.getLogger().info(String.format("Logic App Run Id: %s", request.getHeaders().get("x-ms-workflow-run-id")));
		context.getLogger().info(String.format("Transaction Id: %s", request.getHeaders().get("tid")));
		context.getLogger().info(String.format("Routing for event: %s",request.getQueryParameters().get("event")));
		context.getLogger().info(String.format("Reprocess: %s   --  Action: %s",request.getHeaders().get("reprocesscount"),request.getHeaders().get("actioncount")));

        Integer actionCount = Integer.parseInt(request.getHeaders().get("actioncount"));
        Integer reprocessCount = Integer.parseInt(request.getHeaders().get("reprocesscount"));
                
        AbstractMap<String, Object> routeConfig = 
            GetConfig.pull(request.getQueryParameters().get("event"));

        String event = request.getHeaders().get("event");
        String transactionId = request.getHeaders().get("tid");
        String target = request.getQueryParameters().get("target");
        String run = request.getHeaders().get("x-ms-workflow-run-id");
        String flow = request.getHeaders().get("x-ms-workflow-name");
        String action = "Routing ActiveMQ";
        
        Request _request = null;
        Response _response = new Response("{\"message\":\"failed\"}",null);
        String attributes = Optional.ofNullable(request.getHeaders().get("attributes")).orElse("{}");
        
        context.getLogger().info(String.format("Attributes: %s", attributes));

        try {
            _request = new Request(inputFile,
            attributes,
            routeConfig);
            if(_request != null){
                _response = Route.send(_request,context);
            }
        }
        catch(Exception e){
            e.printStackTrace();

            StringBuilder builder = new StringBuilder();
            for (StackTraceElement element : e.getStackTrace()) {
                    builder.append(element.toString());
                    builder.append("\n");
            }

            outputFile.setValue(builder.toString());


            PutLogs.log(transactionId, "Failed", 
                action, target, attributes, 
                event, run, flow, e.getMessage(),
                actionCount, reprocessCount,context);
            
            return request
                .createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                .header("attributes",attributes)
                .body(e.getMessage())
                .build();
        }

        outputFile.setValue(_response.getPayload());

        PutLogs.log(transactionId, "Succeeded", 
			action, target, attributes, 
			event, run, flow, "OK",
            actionCount, reprocessCount,context);
       
        return request.createResponseBuilder(HttpStatus.OK)
            .header("attributes",_response.getAttributes())
            .body("OK")
            .build();
    }

}
