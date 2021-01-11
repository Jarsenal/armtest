package tccc.bib.tools;

import java.util.AbstractMap;
import java.util.LinkedHashMap;
import java.util.Optional;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;


/**
 * Azure Functions with HTTP Trigger.
 */
public class GetConfig {

    static final String storageConnection = System.getenv("BLOBSTORAGE");
    static final Long cacheLife = Long.parseLong(Optional.ofNullable(System.getenv("CACHE_LIFE_SECONDS")).orElse("600"));

    static final AbstractMap<String, Object> configurations = new LinkedHashMap<>();
    static final AbstractMap<String, Long> cacheLives = new LinkedHashMap<>();
    static final Gson gson = new Gson();


    public static <T> T pull(String config ){

        // start limiting on how long we keep the items in memory
        Long currenttime = (System.currentTimeMillis()/1000);
        Long life = currenttime - Optional.ofNullable(cacheLives.get(config)).orElse(0L);
        
        if(configurations.containsKey(config) && life < cacheLife){
            return (T)configurations.get(config);
        }

        cacheLives.put(config,currenttime);
        
        String configFile = "";
        try {
            CloudStorageAccount storageAccount = CloudStorageAccount.parse(storageConnection);
            CloudBlobClient blobClient = storageAccount.createCloudBlobClient();
            CloudBlobContainer container  = blobClient.getContainerReference("configuration");
            if(!container.exists()){return null;}
            CloudBlockBlob blob = container.getBlockBlobReference(config);
            configFile = blob.downloadText();
            configurations.put(config,
            gson.fromJson(configFile.toString(), 
                new TypeToken<T>() {}.getType()));
        }
        catch(Exception e){
            return null;
        }

        return (T)configurations.get(config);
    }
}


