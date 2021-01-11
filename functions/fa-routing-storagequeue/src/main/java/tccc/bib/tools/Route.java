package tccc.bib.tools;

import tccc.bib.objects.Request;
import tccc.bib.objects.Response;
import java.util.AbstractMap;
import java.util.Objects;

import com.microsoft.azure.functions.*;
import java.io.IOException;


import com.microsoft.azure.storage.*;
import com.microsoft.azure.storage.queue.*;



public class Route {
    
    public static Response send(Request request,final ExecutionContext context)
        throws Exception{
        
        context.getLogger().info("We are routing the message.");
        String queuename = (String)request.getConfig().get("queueName");
        String storageConnectionString =
            String.format("DefaultEndpointsProtocol=https;AccountName=%s;AccountKey=%s",
                        (String)request.getConfig().get("accountName"),
                        (String)request.getConfig().get("accountKey"));
        

        if(Objects.isNull(queuename) || queuename.isEmpty()) {
            throw new Exception("Queue name is missing in the request");
        }

        if (Objects.isNull(request.getPayload()) || request.getPayload().isEmpty()) {
            throw new Exception ("Body is missing in the request");
        }

        context.getLogger().info(String.format("Pushing to queue: %s", queuename));
        
        CloudStorageAccount storageAccount =
        CloudStorageAccount.parse(storageConnectionString);
        CloudQueueClient queueClient = storageAccount.createCloudQueueClient();
        CloudQueue queue = queueClient.getQueueReference(queuename);
        CloudQueueMessage message = new CloudQueueMessage(request.getPayload());
        queue.addMessage(message);
        return new Response("{\"message\":\"success\"}", request.getAttributes());
    }
}