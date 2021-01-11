package jar;

import java.util.*;
import com.microsoft.azure.functions.annotation.*;
import com.microsoft.azure.functions.*;

import javax.crypto.spec.SecretKeySpec;
import javax.crypto.Cipher;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;


/**
 * Azure Functions with HTTP Trigger.
 */
public class Function {
    /**
     */

    @FunctionName("MappingConfigStore")
    @StorageAccount("BLOBSTORAGE")
    public HttpResponseMessage runMappingStore(
            @HttpTrigger(
                name = "req", 
                methods = {HttpMethod.POST}, 
                authLevel = AuthorizationLevel.FUNCTION) HttpRequestMessage<Optional<String>> request,
            @BlobOutput(
                    name = "mappingConfiguration", 
                    path = "configuration/mapping/{event}") OutputBinding<String> outputFile,
                final ExecutionContext context) 
            {
        outputFile.setValue(request.getBody().orElse(""));
                
        return request.createResponseBuilder(HttpStatus.OK)
                .header("Content-Type", "application/json")
                .body("{\"message\":\"success\"}")
                .build();
    }
        

    @FunctionName("MappingConfigRetrieve")
    @StorageAccount("BLOBSTORAGE")
    public HttpResponseMessage runMappingRetrieve(
            @HttpTrigger(
                name = "req", 
                methods = {HttpMethod.GET}, 
                authLevel = AuthorizationLevel.FUNCTION) HttpRequestMessage<Optional<String>> request,
                @BlobInput(
                    name = "mappingConfiguration", 
                    path = "configuration/mapping/{event}") String inputFile,
            final ExecutionContext context) {
        context.getLogger().info("Java HTTP trigger processed a request.");

        return request
                .createResponseBuilder(HttpStatus.OK)
                .body(inputFile)
                .header("Content-Type", "application/json")
                .build();
    }

    @FunctionName("RoutingConfigStore")
    @StorageAccount("BLOBSTORAGE")
    public HttpResponseMessage runRoutingStore(
            @HttpTrigger(
                name = "req", 
                methods = {HttpMethod.POST}, 
                authLevel = AuthorizationLevel.FUNCTION) HttpRequestMessage<Optional<String>> request,
                @BlobInput(
                    name = "routingInConfiguration", 
                    path = "configuration/routing/{event}") String inputFile,
                @BlobOutput(
                    name = "routingOutConfiguration", 
                    path = "configuration/routing/{event}") OutputBinding<String> outputFile,
                final ExecutionContext context) 
            {

                Gson gson = new Gson();

                AbstractMap<String, Object> keyvalues = gson.fromJson(request.getBody().orElse(""),new TypeToken<LinkedHashMap<String, Object>>(){}.getType());
                AbstractMap<String, Object> pulledvalues = gson.fromJson(inputFile, new TypeToken<LinkedHashMap<String, Object>>(){}.getType());
                
                
                if(keyvalues.containsKey("_encrypt") && keyvalues.get("_encrypt") instanceof AbstractList){
                    @SuppressWarnings("unchecked")
                    AbstractList<String> _fields = (AbstractList<String>)keyvalues.get("_encrypt");
                    
                    for(String _field : _fields){
                        
                        String _value = (String)keyvalues.get(_field);
                        
                        if(_value.equals("_hidden_")){
                            keyvalues.put(_field,(String)pulledvalues.get(_field));
                            continue;
                        }

                        try {
                            keyvalues.put(_field, encrypt((String)keyvalues.get(_field)));
                        }
                        catch (Exception e){
                            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                            .header("Content-Type", "application/json")
                            .body("{\"message\":\"failed\"}")
                            .build();
                        }
                    }
                }
        
                outputFile.setValue(gson.toJson(keyvalues));
                
        return request.createResponseBuilder(HttpStatus.OK)
                .header("Content-Type", "application/json")
                .body("{\"message\":\"success\"}")
                .build();
    }
        

    @FunctionName("RoutingConfigRetrieve")
    @StorageAccount("BLOBSTORAGE")
    public HttpResponseMessage runRoutingRetrieve(
            @HttpTrigger(
                name = "req", 
                methods = {HttpMethod.GET}, 
                authLevel = AuthorizationLevel.FUNCTION) HttpRequestMessage<Optional<String>> request,
                @BlobInput(
                    name = "routingConfiguration", 
                    path = "configuration/routing/{event}") String inputFile,
            final ExecutionContext context) {
        context.getLogger().info("Java HTTP trigger processed a request.");

        Gson gson = new Gson();
        AbstractMap<String, Object> pulledvalues = gson.fromJson(inputFile, new TypeToken<LinkedHashMap<String, Object>>(){}.getType());

        if(pulledvalues.containsKey("_encrypt") && pulledvalues.get("_encrypt") instanceof AbstractList){
            
            @SuppressWarnings("unchecked")
            AbstractList<String> _fields = (AbstractList<String>)pulledvalues.get("_encrypt");
            
            for(String _field : _fields){

                try {
                    pulledvalues.put(_field, "_hidden_");
                }
                catch (Exception e){
                    return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                    .header("Content-Type", "application/json")
                    .body("{\"message\":\"failed\"}")
                    .build();
                }
            }
        }

        return request
                .createResponseBuilder(HttpStatus.OK)
                .body(gson.toJson(pulledvalues))
                .header("Content-Type", "application/json")
                .build();
    }


    @FunctionName("EventsConfigStore")
    @StorageAccount("BLOBSTORAGE")
    public HttpResponseMessage runEventsStore(
            @HttpTrigger(
                name = "req", 
                methods = {HttpMethod.POST}, 
                authLevel = AuthorizationLevel.FUNCTION) HttpRequestMessage<Optional<String>> request,
            @BlobOutput(
                    name = "eventsConfiguration", 
                    path = "configuration/events") OutputBinding<String> outputFile,
                final ExecutionContext context) 
            {
        outputFile.setValue(request.getBody().orElse(""));
                
        return request.createResponseBuilder(HttpStatus.OK)
            .header("Content-Type", "application/json")
            .body("{\"message\":\"success\"}")
            .build();
    }
        

    @FunctionName("EventsConfigRetrieve")
    @StorageAccount("BLOBSTORAGE")
    public HttpResponseMessage runEventsRetrieve(
            @HttpTrigger(
                name = "req", 
                methods = {HttpMethod.GET}, 
                authLevel = AuthorizationLevel.FUNCTION) HttpRequestMessage<Optional<String>> request,
                @BlobInput(
                    name = "eventsConfiguration", 
                    path = "configuration/events") String inputFile,
            final ExecutionContext context) {
        context.getLogger().info("Java HTTP trigger processed a request.");

        return request
                .createResponseBuilder(HttpStatus.OK)
                .body(inputFile)
                .header("Content-Type", "application/json")
                .build();
    }

    @FunctionName("MQEventsConfigStore")
    @StorageAccount("BLOBSTORAGE")
    public HttpResponseMessage runMQEventsStore(
            @HttpTrigger(
                name = "req", 
                methods = {HttpMethod.POST}, 
                authLevel = AuthorizationLevel.FUNCTION) HttpRequestMessage<Optional<String>> request,
            @BlobOutput(
                    name = "eventsConfiguration", 
                    path = "configuration/mqevents") OutputBinding<String> outputFile,
                final ExecutionContext context) 
            {
        outputFile.setValue(request.getBody().orElse(""));
                
        return request.createResponseBuilder(HttpStatus.OK)
            .header("Content-Type", "application/json")
            .body("{\"message\":\"success\"}")
            .build();
    }
        

    @FunctionName("MQEventsConfigRetrieve")
    @StorageAccount("BLOBSTORAGE")
    public HttpResponseMessage runMQEventsRetrieve(
            @HttpTrigger(
                name = "req", 
                methods = {HttpMethod.GET}, 
                authLevel = AuthorizationLevel.FUNCTION) HttpRequestMessage<Optional<String>> request,
                @BlobInput(
                    name = "eventsConfiguration", 
                    path = "configuration/mqevents") String inputFile,
            final ExecutionContext context) {
        context.getLogger().info("Java HTTP trigger processed a request.");

        return request
                .createResponseBuilder(HttpStatus.OK)
                .body(inputFile)
                .header("Content-Type", "application/json")
                .build();
    }

    // @FunctionName("TransactionQueryJob")
    // public HttpResponseMessage runTransactionRetrieve(
    //         @HttpTrigger(
    //             name = "req", 
    //             methods = {HttpMethod.GET}, 
    //             authLevel = AuthorizationLevel.FUNCTION) HttpRequestMessage<Optional<String>> request,
    //         final ExecutionContext context) {
    //     context.getLogger().info("Java HTTP trigger processed a request.");

    //     String responseBody = "";
    //     try(CloseableHttpClient httpclient = HttpClients.createDefault()){
    //         context.getLogger().info("URL: " + System.getenv("TRANSACTION_URL"));

    //         HttpGet httpget = new HttpGet(System.getenv("TRANSACTION_URL"));
            
    //         ResponseHandler<String> responseHandler = new ResponseHandler<String>() {
    //             @Override
    //             public String handleResponse(final HttpResponse response) 
    //             throws ClientProtocolException, IOException {
    //                 int status = response.getStatusLine().getStatusCode();
    //                 context.getLogger().info("Status: " + status);
    //                 HttpEntity entity = response.getEntity();
    //                 return entity != null ? EntityUtils.toString(entity) : null;
    //             }
    //         };
    //         responseBody = httpclient.execute(httpget, responseHandler);
    //     }
    //     catch (Exception e){
    //         responseBody = e.getMessage();
    //         return request
    //             .createResponseBuilder(HttpStatus.OK)
    //             .body(responseBody)
    //             .build();
    //     }
        


    //     return request
    //             .createResponseBuilder(HttpStatus.OK)
    //             .body(responseBody)
    //             .header("Content-Type", "application/json")
    //             .build();
    // }

    public static String encrypt(String strClearText) throws Exception{
        String strData="";
        
        try {
            SecretKeySpec skeyspec=new SecretKeySpec(System.getenv("SECRET_KEY").getBytes(),"Blowfish");
            Cipher cipher=Cipher.getInstance("Blowfish");
            cipher.init(Cipher.ENCRYPT_MODE, skeyspec);
            byte[] encrypted=cipher.doFinal(strClearText.getBytes());
            strData=new String(Base64.getEncoder().encode(encrypted));
            
        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception(e);
        }
        return strData;
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
