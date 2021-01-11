package tccc.bib.tools;

import java.util.Optional;

import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;


/**
 * Azure Functions with HTTP Trigger.
 */
public class GetTransaction {

    static final String storageConnection = System.getenv("BLOBSTORAGE");
    static final String charsetName = Optional.ofNullable(System.getenv("CHARSET_NAME")).orElse("UTF-8");
    
    public static String pull(String key) throws Exception {

        String content = "";

        String _container = key.substring(0, key.indexOf("/"));

        CloudStorageAccount storageAccount = CloudStorageAccount.parse(storageConnection);
        CloudBlobClient blobClient = storageAccount.createCloudBlobClient();
        CloudBlobContainer container  = blobClient.getContainerReference(_container);
        if(!container.exists()){throw new Exception(String.format("Container (%s) does not exist!",_container));}
        CloudBlockBlob blob = container.getBlockBlobReference(key.replaceAll(String.format("%s/",_container),""));
        content = blob.downloadText(charsetName, null, null, null);
        
        return content;
    }
}


