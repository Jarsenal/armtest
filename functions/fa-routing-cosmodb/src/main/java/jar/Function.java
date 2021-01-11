package jar;

import java.util.*;

import com.microsoft.azure.functions.annotation.*;

import org.json.JSONArray;

import com.microsoft.azure.functions.*;
import com.microsoft.azure.documentdb.ConnectionMode;
import com.microsoft.azure.documentdb.ConnectionPolicy;
import com.microsoft.azure.documentdb.Database;
import com.microsoft.azure.documentdb.DocumentClient;
import com.microsoft.azure.documentdb.DocumentClientException;
import com.microsoft.azure.documentdb.DocumentCollection;
import com.microsoft.azure.documentdb.FeedResponse;
import com.microsoft.azure.documentdb.Offer;
import com.microsoft.azure.documentdb.PartitionKeyDefinition;
import com.microsoft.azure.documentdb.RequestOptions;
import com.microsoft.azure.documentdb.ResourceResponse;
import com.microsoft.azure.documentdb.RetryOptions;
import com.microsoft.azure.documentdb.ConsistencyLevel;
import com.microsoft.azure.documentdb.bulkexecutor.BulkImportResponse;
import com.microsoft.azure.documentdb.bulkexecutor.DocumentBulkExecutor;
import com.microsoft.azure.documentdb.bulkexecutor.DocumentBulkExecutor.Builder;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;



/**
 * Azure Functions with HTTP Trigger.
 */
public class Function {


    private DocumentClient client;

    public Function(){
        super();
        ConnectionPolicy connectionPolicy = new ConnectionPolicy();
        RetryOptions retryOptions = new RetryOptions();
        retryOptions.setMaxRetryAttemptsOnThrottledRequests(0);
        connectionPolicy.setRetryOptions(retryOptions);
        connectionPolicy.setConnectionMode(ConnectionMode.Gateway);
        connectionPolicy.setMaxPoolSize(10);

        client = new DocumentClient(
                System.getenv("CosmoDbHost"),
                System.getenv("CosmoDbKey"), 
                connectionPolicy,
                ConsistencyLevel.Session);

        // Set client's retry options high for initialization
        client.getConnectionPolicy().getRetryOptions().setMaxRetryWaitTimeInSeconds(120);
        client.getConnectionPolicy().getRetryOptions().setMaxRetryAttemptsOnThrottledRequests(100);
        
    }


    @FunctionName("RunCosmoDBRoutingJob")
    @StorageAccount("tcccbiibuiconfiguration_STORAGE")
    public HttpResponseMessage runCosmoDBRoutingJob(
             @HttpTrigger(
                name = "req", 
                methods = {HttpMethod.POST}, 
                authLevel = AuthorizationLevel.FUNCTION) 
                HttpRequestMessage<Optional<String>> request,
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
            final ExecutionContext context) throws Exception {
        
            
            int result = 0;

            Gson gson = new Gson();
            AbstractMap<String, Object> routeConfig = gson.fromJson(configFile.toString(), new TypeToken<LinkedHashMap<String, Object>>() {
            }.getType());

            
            
            DocumentCollection collection = null;
            
            
            try {
                collection = createEmptyCollectionIfNotExists(
                    client, (String)routeConfig.get("database"), 
                    (String)routeConfig.get("collection"), 
                    (String)routeConfig.get("partitionkey"), 400);

                // You can specify the maximum throughput (out of entire collection's throughput) that you wish the bulk import API to consume here
                int offerThroughput = getOfferThroughput(client, collection);

                Builder bulkExecutorBuilder = DocumentBulkExecutor
                    .builder()
                    .from(client, (String)routeConfig.get("database"), 
                    (String)routeConfig.get("collection"), 
                                collection.getPartitionKey(), offerThroughput);

                // Instantiate bulk executor
                try (DocumentBulkExecutor bulkExecutor = bulkExecutorBuilder.build()) {

                    // Set retries to 0 to pass control to bulk executor
                    client.getConnectionPolicy().getRetryOptions().setMaxRetryWaitTimeInSeconds(0);
                    client.getConnectionPolicy().getRetryOptions().setMaxRetryAttemptsOnThrottledRequests(0);

                    System.out.println("About to look over checkpoints");

                    Collection<String> documents = new LinkedList<>();

                    JSONArray jsonArray = new JSONArray(inputFile);
                    for(int index = 0; index < jsonArray.length(); index++){
                        documents.add(jsonArray.getJSONObject(index).toString());
                    }

                    // Execute bulk update API		
                    System.out.println("Calling update all!");		
                    BulkImportResponse bulkImportResponse = bulkExecutor.importAll(documents, true, true, null);
                    result = bulkImportResponse.getNumberOfDocumentsImported();
                    bulkExecutor.close();
                }
            
            } catch (DocumentClientException e) {
                e.printStackTrace();
            }

            //client.close();
       
        outputFile.setValue(String.format("{\"TotalImported\":\"%d\"}", result));

        return request.createResponseBuilder(HttpStatus.OK).body(
            "Done."
        ).build();
    }

    public DocumentCollection createEmptyCollectionIfNotExists(
            DocumentClient client, 
            String databaseId, 
            String collectionId,
            String partitionKeyDef, 
            int collectionThroughput) throws DocumentClientException {

		String databaseLink = String.format("/dbs/%s", databaseId);
		String collectionLink = String.format("/dbs/%s/colls/%s", databaseId, collectionId);

		ResourceResponse<Database> databaseResponse = null;
		Database readDatabase = null;

		while (readDatabase == null) {
			try {
				databaseResponse = client.readDatabase(databaseLink, null);
				readDatabase = databaseResponse.getResource();

				System.out.println("Database already exists...");
			} catch (DocumentClientException dce) {
				if (dce.getStatusCode() == 404) {
					System.out.println("Attempting to create database since non-existent...");

					Database databaseDefinition = new Database();
					databaseDefinition.setId(databaseId);

					client.createDatabase(databaseDefinition, null);

					databaseResponse = client.readDatabase(databaseLink, null);
					readDatabase = databaseResponse.getResource();
				} else {
					throw dce;
				}
			}
		}

		ResourceResponse<DocumentCollection> collectionResponse = null;
		DocumentCollection readCollection = null;

		while (readCollection == null) {
			try {
				collectionResponse = client.readCollection(collectionLink, null);
				readCollection = collectionResponse.getResource();

				System.out.println("Collection already exists...");
			} catch (DocumentClientException dce) {
				if (dce.getStatusCode() == 404) {
					System.out.println("Attempting to create collection since non-existent...");

                    System.out.println(String.format("Collection Id: %s", collectionId));

					DocumentCollection collectionDefinition = new DocumentCollection();
					collectionDefinition.setId(collectionId);

                    if(partitionKeyDef != null){
    					PartitionKeyDefinition partitionKeyDefinition = new PartitionKeyDefinition();
                        Collection<String> paths = new ArrayList<String>();
                        paths.add(partitionKeyDef);
                        partitionKeyDefinition.setPaths(paths);
                        collectionDefinition.setPartitionKey(partitionKeyDefinition);
                    }

					RequestOptions options = new RequestOptions();
					options.setOfferThroughput(400);

                    System.out.println(String.format("Now creating the collection with link: %s with partitionKey: %s", databaseLink, partitionKeyDef));

					// create a collection
					client.createCollection(databaseLink, collectionDefinition, options);

					collectionResponse = client.readCollection(collectionLink, null);
					readCollection = collectionResponse.getResource();
				} else {
					throw dce;
				}
			}
		}

		// Find offer associated with this collection
		Iterator<Offer> it = client.queryOffers(
				String.format("SELECT * FROM r where r.offerResourceId = '%s'", readCollection.getResourceId()), null)
				.getQueryIterator();
		Offer offer = it.next();

		// Update the offer
		System.out.println("Attempting to modify collection throughput...");

		offer.getContent().put("offerThroughput", collectionThroughput);
		client.replaceOffer(offer);

		return readCollection;
    }
    
    public int getOfferThroughput(DocumentClient client, DocumentCollection collection) {
		FeedResponse<Offer> offers = client.queryOffers(
				String.format("SELECT * FROM c where c.offerResourceId = '%s'", collection.getResourceId()), null);

		List<Offer> offerAsList = offers.getQueryIterable().toList();
		if (offerAsList.isEmpty()) {
			throw new IllegalStateException("Cannot find Collection's corresponding offer");
		}

		Offer offer = offerAsList.get(0);
		return offer.getContent().getInt("offerThroughput");
    }
}
