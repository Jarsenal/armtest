package tccc.bib.main;

import java.util.*;
import com.microsoft.azure.functions.annotation.*;
import com.microsoft.azure.functions.*;
import com.microsoft.azure.storage.*;
import com.microsoft.azure.storage.queue.*;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.Cipher;

/**
 * Azure Functions with HTTP Trigger.
 */
public class Function {
    
    @FunctionName("readStorageQueue")
    public HttpResponseMessage run(
            @HttpTrigger(name = "req", methods = {HttpMethod.GET}, authLevel = AuthorizationLevel.FUNCTION) HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {
    	
        context.getLogger().info("Azure HTTP trigger processed a request.");
        
        String queuename = request.getQueryParameters().get("queuename");
        
        if(queuename == null || queuename == "") {
            request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                .body("Pass queue name")
                .build();
        }

        context.getLogger().info(String.format("Pulling message from queue: %s",queuename));
        

        String responseBody = "";

        try {
            context.getLogger().info(String.format("Retrieving message."));
            String storageConnectionString =
            
            String.format("DefaultEndpointsProtocol=https;AccountName=%s;AccountKey=%s",
                        System.getenv("ACCOUNT_NAME"),System.getenv("ACCOUNT_KEY"));


            CloudStorageAccount storageAccount =
            CloudStorageAccount.parse(storageConnectionString);

            // Create the queue client.
            CloudQueueClient queueClient = storageAccount.createCloudQueueClient();

            // Retrieve a reference to a queue.
            CloudQueue queue = queueClient.getQueueReference(queuename);

            // Peek at the next message.
            CloudQueueMessage peekedMessage = queue.retrieveMessage();
            
            

            // Output the message value.
            if (peekedMessage != null)
            {
                System.out.println(peekedMessage.getMessageContentAsString());
            }
        
        }
        catch (Exception e){
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(e.getMessage())
            .build();
        }

        return request.createResponseBuilder(HttpStatus.OK).body(responseBody).build();
    }
    
    public static String decrypt(String strEncrypted) throws Exception{
        String strData="";
        
        try {
            SecretKeySpec skeyspec=new SecretKeySpec(System.getenv("SECRET_KEY").getBytes(),"Blowfish");
            Cipher cipher=Cipher.getInstance("Blowfish");
            cipher.init(Cipher.DECRYPT_MODE, skeyspec);
            byte[] decrypted=cipher.doFinal(
                Base64.getDecoder().decode(strEncrypted.getBytes())
            );
            
            strData=new String(decrypted);
            
        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception(e);
        }
        return strData;
    }

   

}
