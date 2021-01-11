package tccc.bib.main;

import java.util.AbstractMap;
import java.util.Optional;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.OutputBinding;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.BlobInput;
import com.microsoft.azure.functions.annotation.BlobOutput;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;
import com.microsoft.azure.functions.annotation.StorageAccount;

import tccc.bib.factories.RoutesFactory;
import tccc.bib.interfaces.IRoute;
import tccc.bib.objects.Request;
import tccc.bib.objects.Response;
import tccc.bib.tools.GetConfig;
import tccc.bib.tools.PutLogs;



public class RoutingAsynch {
	
	private static final String NEW_LINE = "\n";

    @FunctionName("RunHTTPRoutingJob")
    @StorageAccount("BLOBSTORAGE")
    public HttpResponseMessage run(
    		@HttpTrigger(name = "req", methods = {HttpMethod.POST}, authLevel = AuthorizationLevel.FUNCTION) HttpRequestMessage<Optional<String>> request,
            @BlobInput(name = "source", path = "{source}") String inputFile,
            @BlobOutput(name = "target", path = "{target}") OutputBinding<String> outputFile,
            final ExecutionContext context) {
        
        
        context.getLogger().info(String.format("Logic App Run Id: %s", request.getHeaders().get("x-ms-workflow-run-id")));
        context.getLogger().info(String.format("Transaction Id: %s", request.getHeaders().get("tid")));
        context.getLogger().info(String.format("Routing for event: %s",request.getQueryParameters().get("event")));
		context.getLogger().info(String.format("Reprocess: %s   --  Action: %s",request.getHeaders().get("reprocesscount"),request.getHeaders().get("actioncount")));

        Integer actionCount = Integer.parseInt(request.getHeaders().get("actioncount"));
        Integer reprocessCount = Integer.parseInt(request.getHeaders().get("reprocesscount"));


        AbstractMap<String, Object> routeConfig = GetConfig.pull(request.getQueryParameters().get("event"));
        context.getLogger().info(String.format("Type found: %s",(String)routeConfig.get("type")));

        String routeType = "simple";
        if(routeConfig.containsKey("split.field")){
            routeType = "split";
        }
        
        Request _request = null;
        Response _response = new Response("",null);
        
        String attributes = Optional.ofNullable(request.getHeaders().get("attributes")).orElse("{}");

        String event = request.getHeaders().get("event");
        String transactionId = request.getHeaders().get("tid");
        String target = request.getQueryParameters().get("target");
        String run = request.getHeaders().get("x-ms-workflow-run-id");
        String flow = request.getHeaders().get("x-ms-workflow-name");
        String action = "Routing HTTP";


        IRoute route = RoutesFactory.getRoute(routeType);
        StringBuilder outputFileContent = new StringBuilder();
        try {
            context.getLogger().info("Making Request.");
            _request = new Request(inputFile, attributes, routeConfig);
            _response = route.send(_request,context);
        }
        catch(Exception e){
            e.printStackTrace();
            
            outputFileContent.append("Java exception:").append(NEW_LINE);
            for (StackTraceElement element : e.getStackTrace()) {
            	outputFileContent.append(element.toString()).append(NEW_LINE);
            }
            outputFileContent.append(NEW_LINE).append(outputFile.getValue());
            
            
            outputFile.setValue(outputFileContent.toString());

            PutLogs.log(transactionId, "Failed", 
                action, target, attributes, 
                event, run, flow, e.getMessage(),
                actionCount, reprocessCount, context);

            return request
                .createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                .header("attributes",attributes)
                .body(e.getMessage())
                .build();
        }
        
        int status = Integer.parseInt(_response.getAttribute("http_status"));
        if(status == 204) status = 200;

        if (status >= 400) {
        	outputFileContent.append("Remote system error:").append(NEW_LINE)
        						.append(_response.getPayload()).append(NEW_LINE).append(NEW_LINE)
        						.append(outputFile.getValue());
        } else {
        	outputFileContent.append(_response.getPayload());
        }
        
        
        outputFile.setValue(outputFileContent.toString());

        PutLogs.log(transactionId, (status >= 400)?"Failed":"Succeeded", 
			action, target, attributes, 
            event, run, flow, (status >= 400)?String.format("We got remote system status: %d.  Please review the resulting payload",status):"OK",
            actionCount, reprocessCount, context);

        return request.createResponseBuilder(HttpStatus.valueOf(status))
                .header("attributes",_response.getAttributes())
                .body((status < 400)?"OK":String.format("We got remote system status: %d.  Please review the resulting payload",status))
                .build();
    }
}
