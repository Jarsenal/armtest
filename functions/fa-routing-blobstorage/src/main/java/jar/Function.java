package jar;

import java.util.*;
import com.microsoft.azure.functions.annotation.*;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.microsoft.azure.functions.*;
import com.microsoft.azure.storage.blob.*;
import com.microsoft.azure.storage.CloudStorageAccount;

import javax.crypto.spec.SecretKeySpec;
import javax.crypto.Cipher;

import java.util.Base64;



/**
 * Azure Functions with HTTP Trigger.
 */
public class Function {
    

    public static final String storageConnection = System.getenv("BLOBSTORAGE");

    @FunctionName("BlobRoute")
    @StorageAccount("BLOBSTORAGE")
    public HttpResponseMessage run(
            @HttpTrigger(
                    name = "req", 
                    methods = {HttpMethod.GET, HttpMethod.POST},
                    authLevel = AuthorizationLevel.FUNCTION) HttpRequestMessage<Optional<String>> request,
                    @BlobInput(
                        name = "routingConfiguration", 
                        path = "{event}") 
                        String configFile,
                    @BlobInput(
                        name = "sourceInput", 
                        path = "{source}") 
                        String inputFile,
                    @BlobOutput(
                        name = "targetOutput", 
                        path = "{target}") 
                        OutputBinding<String> outputFile,
            final ExecutionContext context) {
        
        context.getLogger().info("Java HTTP trigger processed a request.");
        //context.getLogger().info(String.format("\n\nConfiguration pulled: %s\n\n",configFile));


         //queueName
         Gson gson = new Gson();
         AbstractMap<String, Object> routeConfig = gson.fromJson(
             configFile, new TypeToken<LinkedHashMap<String, Object>>() {}.getType()
         );

          // decrypt
        if(routeConfig.containsKey("_encrypt") && routeConfig.get("_encrypt") instanceof AbstractList){
            
            @SuppressWarnings("unchecked")
            AbstractList<String> _fields = (AbstractList<String>)routeConfig.get("_encrypt");
            
            for(String _field : _fields){

                try {
                    routeConfig.put(_field, (String)decrypt((String)routeConfig.get(_field)));
                    context.getLogger().info(String.format("We got query: %s", routeConfig.get(_field)));
                }
                catch (Exception e){
                    return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                    .header("Content-Type", "application/json")
                    .body("{\"message\":\"failed\"}")
                    .build();
                }
            }
        }
        // end decrypt


         // attributes
         AbstractMap<String, Object> attributes = null;
         if(request.getHeaders().containsKey("attributes")){
             attributes = gson.fromJson(
                request.getHeaders().get("attributes"), new TypeToken<LinkedHashMap<String, String>>() {}.getType()
            );
         

             for(String akey : attributes.keySet()){
                 for(String ckey : routeConfig.keySet()){
                     if(routeConfig.get(ckey) instanceof String){
                         routeConfig.put(ckey,((String)routeConfig.get(ckey)).replaceAll(
                             String.format("<<%s>>",akey),
                             (String)attributes.get(akey)
                         ));
                     }
                 }
             }
         }
         // end attributes



         try {
            CloudStorageAccount storageAccount = CloudStorageAccount.parse(storageConnection);
            CloudBlobClient blobClient = storageAccount.createCloudBlobClient();
            CloudBlobContainer container  = blobClient.getContainerReference("storage");

            if(!container.exists()){
                return request.createResponseBuilder(HttpStatus.NOT_FOUND).body("Storage container is missing, please create a container name \"storage\" for the storage account.").build();
            }

            CloudBlockBlob blob = container.getBlockBlobReference(String.format("%s/%s", (String)routeConfig.get("folder"),(String)routeConfig.get("name")));

            if(((String)routeConfig.get("action")).equals("GET")){
                outputFile.setValue(blob.downloadText());
            } 
            else {
                blob.uploadText(inputFile);
                outputFile.setValue("{\"message\":\"success\"}");
            }
        }
        catch(Exception e){
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage()).build();
        }
        return request.createResponseBuilder(HttpStatus.OK).body("Done.").build();
    }

    @FunctionName("BlobRouteSynch")
    @StorageAccount("BLOBSTORAGE")
    public HttpResponseMessage runSynch(
            @HttpTrigger(
                    name = "req", 
                    methods = {HttpMethod.GET, HttpMethod.POST},
                    authLevel = AuthorizationLevel.FUNCTION) HttpRequestMessage<Optional<String>> request,
                    @BlobInput(
                        name = "routingConfiguration", 
                        path = "{event}") 
                        String configFile,
            final ExecutionContext context) {
        context.getLogger().info("Java HTTP trigger processed a request.");
        context.getLogger().info(String.format("\n\nConfiguration pulled: %s\n\n",configFile));


         //queueName
         Gson gson = new Gson();
         AbstractMap<String, Object> routeConfig = gson.fromJson(
             configFile, new TypeToken<LinkedHashMap<String, Object>>() {}.getType()
         );

          // decrypt
        if(routeConfig.containsKey("_encrypt") && routeConfig.get("_encrypt") instanceof AbstractList){
            
            @SuppressWarnings("unchecked")
            AbstractList<String> _fields = (AbstractList<String>)routeConfig.get("_encrypt");
            
            for(String _field : _fields){

                try {
                    routeConfig.put(_field, (String)decrypt((String)routeConfig.get(_field)));
                }
                catch (Exception e){
                    return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                    .header("Content-Type", "application/json")
                    .body("{\"message\":\"failed\"}")
                    .build();
                }
            }
        }
        // end decrypt


         // attributes
         AbstractMap<String, Object> attributes = null;
         if(request.getHeaders().containsKey("attributes")){
             attributes = gson.fromJson(
                request.getHeaders().get("attributes"), new TypeToken<LinkedHashMap<String, String>>() {}.getType()
            );
         

             for(String akey : attributes.keySet()){
                 for(String ckey : routeConfig.keySet()){
                     if(routeConfig.get(ckey) instanceof String){
                         routeConfig.put(ckey,((String)routeConfig.get(ckey)).replaceAll(
                             String.format("<<%s>>",akey),
                             (String)attributes.get(akey)
                         ));
                     }
                 }
             }
         }
         // end attributes



         try {
            CloudStorageAccount storageAccount = CloudStorageAccount.parse(storageConnection);
            CloudBlobClient blobClient = storageAccount.createCloudBlobClient();
            CloudBlobContainer container  = blobClient.getContainerReference("storage");

            if(!container.exists()){
                return request.createResponseBuilder(HttpStatus.NOT_FOUND).body("Storage container is missing, please create a container name \"storage\" for the storage account.").build();
            }

            CloudBlockBlob blob = container.getBlockBlobReference(String.format("%s/%s", (String)routeConfig.get("folder"),(String)routeConfig.get("name")));

            if(((String)routeConfig.get("action")).equals("GET")){
                return request.createResponseBuilder(HttpStatus.OK).body(
                    blob.downloadText()
                ).build();
            } 
            else {
                blob.uploadText(request.getBody().orElse("No Content"));
            }
        }
        catch(Exception e){
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage()).build();
        }
        return request.createResponseBuilder(HttpStatus.OK).body("{\"message\":\"success\"}").build();
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
