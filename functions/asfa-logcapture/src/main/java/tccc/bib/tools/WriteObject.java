package tccc.bib.tools;

import java.util.*;

import com.microsoft.azure.storage.blob.*;
import com.microsoft.azure.storage.CloudStorageAccount;


import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;


/**
 * Azure Functions with HTTP Trigger.
 */
public class WriteObject {

    static final String storageConnection = System.getenv("BLOBSTORAGE");
    static final AbstractMap<String, Object> configurations = new LinkedHashMap<>();
    static final Gson gson = new Gson();


    public static <T> void write(T object, String location)
    throws Exception{
        String payload = gson.toJson(object);
        CloudStorageAccount storageAccount = CloudStorageAccount.parse(storageConnection);
        CloudBlobClient blobClient = storageAccount.createCloudBlobClient();
        CloudBlobContainer container  = blobClient.getContainerReference("transactions");
        if(!container.exists()){return;}
        CloudBlockBlob blob = container.getBlockBlobReference(location);
        blob.uploadText(payload);
    }

    public static <T> void write(T object, String location, String container)
    throws Exception{
        String payload = gson.toJson(object);
        CloudStorageAccount storageAccount = CloudStorageAccount.parse(storageConnection);
        CloudBlobClient blobClient = storageAccount.createCloudBlobClient();
        CloudBlobContainer _container  = blobClient.getContainerReference(container);
        if(!_container.exists()){return;}
        CloudBlockBlob blob = _container.getBlockBlobReference(location);
        blob.uploadText(payload);
    }
}


