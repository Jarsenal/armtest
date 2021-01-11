package tccc.bib.tools;

import tccc.bib.interfaces.IRoute;
import tccc.bib.objects.Request;
import tccc.bib.objects.Response;

import java.util.AbstractList;
import java.util.AbstractMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Optional;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.microsoft.azure.functions.*;
import java.io.IOException;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLContextBuilder;
import org.apache.http.conn.ssl.TrustAllStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;
import org.apache.http.entity.StringEntity;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.HttpClientBuilder;


public class RouteSplit implements IRoute {
    
    Gson gson = new Gson();
    
    public Response send(Request request, final ExecutionContext context)
        throws Exception{
        
        context.getLogger().info("Inside Route function.");
        
        CloseableHttpClient httpclient = null;
        AbstractMap<String, Object> routeConfig = request.getConfig();
        Response response = null;

        String _connectionTimeout = Optional.ofNullable((String)routeConfig.get("connectionTimeout")).orElse("30000");
        String _requestTimeout = Optional.ofNullable((String)routeConfig.get("requestTimeout")).orElse("30000");
        String _socketTimeout = Optional.ofNullable((String)routeConfig.get("socketTimeout")).orElse("30000");
        String _mimeType = Optional.ofNullable((String)routeConfig.get("mimeType")).orElse("text/plain");
        String _charset = Optional.ofNullable((String)routeConfig.get("charset")).orElse("UTF-8");       


        String _splitfield = Optional.ofNullable((String)routeConfig.get("split.field")).orElse("");
        Integer _splitsize = Integer.parseInt(Optional.ofNullable((String)routeConfig.get("split.batchsize")).orElse("1"));
        String _splittype = Optional.ofNullable((String)routeConfig.get("split.type")).orElse("JSON");

        /*
        / We will assume JSON at this point.  This is a quick fix to handle larger loads, we will
        / want to work out a better way to split the payload at the receive end
        */
        RequestConfig config = RequestConfig.custom()
            .setConnectTimeout(Integer.parseInt(_connectionTimeout))
            .setConnectionRequestTimeout(Integer.parseInt(_requestTimeout))
            .setSocketTimeout(Integer.parseInt(_socketTimeout)).build();
        

        // TODO: need to rework this
        //////////////////////////////
        if (routeConfig.get("username") != null && routeConfig.get("password") != null) {
            CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(
                    (String) routeConfig.get("username"), (String) routeConfig.get("password")));
            httpclient = HttpClientBuilder
                                .create()
                                .setDefaultCredentialsProvider(credentialsProvider)
                                .setDefaultRequestConfig(config)
                                .build();
        } 
        else {
            httpclient = HttpClients.custom()
            .setSSLContext(
                new SSLContextBuilder()
                    .loadTrustMaterial(null, TrustAllStrategy.INSTANCE)
                    .build())
            .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
            .setDefaultRequestConfig(config)
            .build();
        }

        
        String queryParams = null;
        for (String key : routeConfig.keySet()) {
            if (key.contains("query.")) {
                if (queryParams == null)
                    queryParams = "?";
                else
                    queryParams += "&";
                queryParams += String.format("%s=%s", key.replace("query.", ""), (String) routeConfig.get(key));
            }
        }

        HttpPost httppost = new HttpPost((String) routeConfig.get("url") + ((queryParams == null) ? "" : queryParams));

        for (String key : routeConfig.keySet()) {
            if (key.contains("header.")) {
                context.getLogger().info(String.format("Adding header to call: %s", key.replace("header.", "")));
                httppost.setHeader(key.replace("header.", ""), (String) routeConfig.get(key));
            }
        }
        
        ResponseHandler<String> responseHandler = (res) -> {
                Integer status = res.getStatusLine().getStatusCode();
                context.getLogger().info("Status: " + status);

                HttpEntity entity = res.getEntity();
                
                return entity != null ? EntityUtils.toString(entity) : "";
            };
        
        StringBuilder resultBuilder = new StringBuilder();
            
        for(String payload : splitPayload(request.getPayload(), _splitfield, _splitsize, _splittype)){
            context.getLogger().info(String.format("Making the request to the given url: %s",(String) routeConfig.get("url")));
             httppost.setEntity(new StringEntity(payload, _mimeType, _charset));
             resultBuilder.append(httpclient.execute(httppost, responseHandler));
             resultBuilder.append("\r\n");
        }
        
        httpclient.close();

        response = new Response( resultBuilder.toString(), request.getAttributes());
        response.addAttribute("http_status","200") ;


        return response;
    }

    private AbstractList<String> splitPayload(String payload,String splitfield,Integer splitsize, String splittype){
        LinkedList<String> list = new LinkedList<>();
        String[] tokens = splitfield.split("\\.");
        Object root = null;
        AbstractList<Object> target = null;
        
        if(splitfield.isEmpty()){
            target = gson.fromJson(payload, new TypeToken<LinkedList<LinkedHashMap<String, Object>>>() {}.getType());

            int count = 0;
            AbstractList<Object> batch = new LinkedList<>();
        
            for(Object item : (AbstractList<Object>)root){
                count++;
                batch.add(item);
                if(count >= splitsize){
                    String _batch = gson.toJson(batch);
                    list.add(_batch);
                    
                    // reset
                    count = 0;
                    batch.clear();
                }
            }
            
            if(batch.size() > 0){
                String _batch = gson.toJson(batch);
                list.add(_batch);
            }
        }
        else {

            root = gson.fromJson(payload, new TypeToken<LinkedHashMap<String, Object>>() {}.getType());
            
            Object temp = root;

            AbstractMap<String, Object> pretemp = (AbstractMap<String, Object>)temp;

            String key = "";

            for(String token : tokens){
                pretemp = (AbstractMap<String, Object>)temp;
                temp = ((AbstractMap)temp).get(token);
                key = token;
            }
            pretemp.remove(key);
            int count = 0;
            AbstractList<Object> batch = new LinkedList<>();
            for(Object item : (AbstractList<Object>)temp){
                count++;
                batch.add(item);
                if(count >= splitsize){
                    pretemp.put(key, batch);
                    String _batch = gson.toJson(root);
                    list.add(_batch);
                    pretemp.remove(key);
                    
                    // reset
                    count = 0;
                    batch.clear();
                }
            }

            if(batch.size() > 0){
                pretemp.put(key, batch);
                    String _batch = gson.toJson(root);
                    list.add(_batch);
                    pretemp.remove(key);
            }
        }

       
            

        return list;
    }
}