package tccc.bib.main;

import java.util.*;

import com.microsoft.azure.functions.annotation.*;
import com.microsoft.azure.functions.*;

import com.microsoft.azure.storage.blob.*;
import com.microsoft.azure.storage.CloudStorageAccount;





/**
 * Azure Functions with HTTP Trigger.
 */
public class GrabPayload {

    @FunctionName("Get-Payload")
    public HttpResponseMessage run(@HttpTrigger(name = "req", methods = {
            HttpMethod.GET }, authLevel = AuthorizationLevel.FUNCTION) HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {
        context.getLogger().info("Java HTTP trigger processed a request.");

        String key = request.getQueryParameters().get("key");

        if(key == null){
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                .header("Content-Type","text/plain")
                .body("Missing Key")
                .build();    
        }

        if(key.indexOf("transactions/") != 0){
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                .header("Content-Type","text/plain")
                .body("Invlalid Key")
                .build();    
        }

        key = key.replace("transactions/","");
        
        context.getLogger().info(String.format("Pulling %s",key));

        String result = "";


        try {
            CloudStorageAccount storageAccount = CloudStorageAccount.parse(System.getenv("BLOBSTORAGE"));
            CloudBlobClient blobClient = storageAccount.createCloudBlobClient();
            CloudBlobContainer container  = blobClient.getContainerReference("transactions");
            if(!container.exists()){return null;}
            CloudBlockBlob blob = container.getBlockBlobReference(key);
            result = blob.downloadText();
            context.getLogger().info("Succefully made the call");
        }
        catch (Exception e) {
            e.printStackTrace();
            context.getLogger().info(e.getMessage());
        }

        if(result.isEmpty()){
            return request.createResponseBuilder(HttpStatus.NOT_FOUND)
                .header("Content-Type","text/plain")
                .body("Content is either missing or empty.")
                .build();    
        }

        return request.createResponseBuilder(HttpStatus.OK)
                .header("Content-Type","text/plain")
                .body(result)
                .build();
    }

   
}
