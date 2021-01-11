package tccc.bib.tools;

import tccc.bib.interfaces.IRoute;
import tccc.bib.objects.Request;
import tccc.bib.objects.Response;
import java.util.AbstractMap;

import com.google.common.base.Optional;
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


public class RouteSimple implements IRoute {
    
    public Response send(Request request,final ExecutionContext context)
        throws Exception{
        
        context.getLogger().info("Inside Route function.");
        
        CloseableHttpClient httpclient = null;
        AbstractMap<String, Object> routeConfig = request.getConfig();
        Response response = null;

        String _connectionTimeout = Optional.fromNullable((String)routeConfig.get("connectionTimeout")).or("30000");
        String _requestTimeout = Optional.fromNullable((String)routeConfig.get("requestTimeout")).or("30000");
        String _socketTimeout = Optional.fromNullable((String)routeConfig.get("socketTimeout")).or("30000");
        String _mimeType = Optional.fromNullable((String)routeConfig.get("mimeType")).or("text/plain");
        String _charset = Optional.fromNullable((String)routeConfig.get("charset")).or("UTF-8");       

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
        
        ResponseHandler<Response> responseHandler = new ResponseHandler<Response>() {
            @Override
            public Response handleResponse(final HttpResponse response) 
                throws ClientProtocolException, IOException {
                Integer status = response.getStatusLine().getStatusCode();
                context.getLogger().info("Status: " + status);

                HttpEntity entity = response.getEntity();
                Response _response = new Response(
                    entity != null ? EntityUtils.toString(entity) : null,
                    request.getAttributes()
                    );

                _response.addAttribute("http_status",status.toString()) ;

                return _response;
            }
        };
        
        httppost.setEntity(new StringEntity(request.getPayload(), _mimeType, _charset));
        
        context.getLogger()
            .info(String.format("Making the request to the given url: %s",
             (String) routeConfig.get("url")));

        response = httpclient.execute(httppost, responseHandler);



        
        httpclient.close();

        return response;
    }
    
}