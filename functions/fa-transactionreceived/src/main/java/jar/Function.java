package jar;

import java.util.*;
import com.microsoft.azure.functions.annotation.*;
import com.microsoft.azure.functions.*;

/**
 * Azure Functions with HTTP Trigger.
 */
public class Function {
    /**
     * This function listens at endpoint "/api/HttpTrigger-Java". Two ways to invoke it using "curl" command in bash:
     * 1. curl -d "HTTP Body" {your host}/api/HttpTrigger-Java&code={your function key}
     * 2. curl "{your host}/api/HttpTrigger-Java?name=HTTP%20Query&code={your function key}"
     * Function Key is not needed when running locally, it is used to invoke function deployed to Azure.
     * More details: https://aka.ms/functions_authorization_keys
     */
    @FunctionName("TransactionReceive")
    @StorageAccount("tcccbiibuiconfiguration_STORAGE")
    public HttpResponseMessage run(
            @HttpTrigger(name = "req", methods = {HttpMethod.POST}, 
                authLevel = AuthorizationLevel.FUNCTION) 
                HttpRequestMessage<Optional<String>> request,
            @BlobOutput(
                    name = "outputFile", 
                    path = "transactions/received/{event}/{transaction}") OutputBinding<String> outputFile,
            final ExecutionContext context){
        context.getLogger().info("Java HTTP trigger processed a request.");

        
        try{
            String payload = request.getBody().orElseThrow(Exception::new);
            outputFile.setValue(payload);
        }
        catch (Exception e){
            return request.createResponseBuilder(HttpStatus.OK)
                .header("Content-Type", "applicaiton/json")
                .body(String.format("{\"message\":\"%s\"}", e.getMessage()
                                                                .replace("\"","\\\"")
                                                                .replace("\n","\\\n")
                                                                .replace("\\","\\\\")
                                    )
                    )
                .build();
        }
        return request.createResponseBuilder(HttpStatus.OK)
                .header("Content-Type", "applicaiton/json")
                .body("{\"message\":\"success\"}")
                .build();
    }
}
