package tccc.bib.tools;

import java.util.*;

import com.microsoft.azure.storage.blob.*;
import com.microsoft.azure.storage.CloudStorageAccount;


import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;


/**
 * Azure Functions with HTTP Trigger.
 */
public class GetConfig {

    static final String storageConnection = System.getenv("BLOBSTORAGE");
    static final AbstractMap<String, Object> configurations = new LinkedHashMap<>();
    static final Gson gson = new Gson();


    public static <T> T pull(String config){

        
        if(configurations.containsKey(config)){
            return (T)configurations.get(config);
        }

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


