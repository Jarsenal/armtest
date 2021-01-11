package tccc.bib.tools;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLContextBuilder;
import org.apache.http.conn.ssl.TrustAllStrategy;
import org.apache.http.params.HttpParams;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.client.config.RequestConfig;

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.AbstractMap;
import java.util.Optional;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.HttpClientBuilder;

public class CreateHttpClient {
    static CloseableHttpClient create(AbstractMap<String, Object> routeConfig)
            throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException {
        
        
        String _connectionTimeout = Optional.ofNullable((String)routeConfig.get("connectionTimeout")).orElse("30000");
        String _requestTimeout = Optional.ofNullable((String)routeConfig.get("requestTimeout")).orElse("30000");
        String _socketTimeout = Optional.ofNullable((String)routeConfig.get("socketTimeout")).orElse("30000");

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
            return HttpClientBuilder
                                .create()
                                .setDefaultCredentialsProvider(credentialsProvider)
                                .setDefaultRequestConfig(config)
                                .build();
        } 
        else {
            return HttpClients.custom()
            .setSSLContext(
                new SSLContextBuilder()
                    .loadTrustMaterial(null, TrustAllStrategy.INSTANCE)
                    .build())
            .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
            .setDefaultRequestConfig(config)
            .build();
        }
    }
}