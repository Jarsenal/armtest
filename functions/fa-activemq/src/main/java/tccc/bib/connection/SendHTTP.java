package tccc.bib.connection;

import java.util.*;
import com.microsoft.azure.functions.*;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.entity.StringEntity;
import java.io.IOException;

public class SendHTTP {

    public static String sendPayload(String payload,String url, ExecutionContext context){
            
            context.getLogger().info(String.format("Received message"));
            String responseBody = "";  
            try(CloseableHttpClient httpclient = HttpClients.createDefault()){
                HttpPost httppost = new HttpPost(url);

                httppost.setEntity(new StringEntity(payload));
                ResponseHandler<String> responseHandler = new ResponseHandler<String>() {
    
                    @Override
                    public String handleResponse(
                            final HttpResponse response) throws ClientProtocolException, IOException {
                        int status = response.getStatusLine().getStatusCode();
                        context.getLogger().info("Status: " + status);
                        return response.getFirstHeader("transactionId").getValue();
                    }
    
                };
                responseBody = httpclient.execute(httppost, responseHandler);
            } 
            catch(IOException e){
                context.getLogger().info(e.getMessage().toString());
                return "FAILED";
            }
           
            return responseBody;
    }
}


