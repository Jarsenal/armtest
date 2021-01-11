package tccc.bib.main;

import java.util.*;
import java.util.logging.Logger;

import tccc.bib.tools.*;

import tccc.bib.transform.factories.CreateToHashMap;
import tccc.bib.transform.interfaces.TransformToHashMap;
import com.microsoft.azure.functions.annotation.*;
import com.microsoft.azure.functions.*;

import com.microsoft.azure.storage.blob.*;
import com.microsoft.azure.storage.CloudStorageAccount;


import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.util.stream.Collectors;




/**
 * Azure Functions with HTTP Trigger.
 */
public class ReceiveJob {
	
	static final String CONFIG_CONTAINER_NAME = "configuration";
	static final String EVENTS_CONFIG_BLOB_NAME = "events";

	static String storageConnection;
	static CloudBlobContainer configuration;
	
	static CloudBlockBlob eventsRef;
	static final long CACHE_CHECK_INTERVAL = Long.parseLong(Optional.ofNullable(System.getenv("CACHE_LIFE_SECONDS")).orElse("120")) * 1000;
	static long eventsLastCached = 0;
	static long eventsLastChecked = 0;
	
	static AbstractList<Object> eventsInCache;
	static final Gson gson = new Gson();
	static final AbstractMap<String, String> eventroute = new LinkedHashMap<>();
	
	static boolean isInitialized = false;

    private static void init() {
    	storageConnection = System.getenv("BLOBSTORAGE");
    	if (storageConnection == null)
    		throw new IllegalStateException("ERRONEOUS CLASS-LOADING, RUNRECEIVEJOB IS MISSING BLOBSTORAGE ENV VAR!");
    	
    	try {
    		configuration = CloudStorageAccount.parse(storageConnection).createCloudBlobClient().getContainerReference(CONFIG_CONTAINER_NAME);
    		eventsRef = configuration.getBlockBlobReference(EVENTS_CONFIG_BLOB_NAME);
    	} catch (Exception e) {
    		throw new IllegalStateException("ERRONEOUS CLASS-LOADING, RUNRECEIVEJOB CANNOT LOAD CONFIG PROPERLY!");
    	}
    	
    	isInitialized = true;
    }



    @FunctionName("RunReceiveJob")
    @StorageAccount("BLOBSTORAGE")
    public HttpResponseMessage runReceiveJob(
        @HttpTrigger(name = "req", methods = {HttpMethod.POST}, authLevel = AuthorizationLevel.FUNCTION) HttpRequestMessage<Optional<String>> request,
        @BlobInput(name = "file", path = "{payload}") String payload,
        final ExecutionContext context) {

    	final String sourceevent = request.getQueryParameters().get("event");
        final String key = request.getQueryParameters().get("payload");
        final String attributes = request.getHeaders().get("attributes");
        final String transactionId = request.getHeaders().get("tid");
        Integer actionCount = Integer.parseInt(request.getHeaders().get("actioncount"));
        Integer reprocessCount = Integer.parseInt(request.getHeaders().get("reprocesscount"));

        String startevent = sourceevent;
        String endevent = sourceevent;
        Object _request = null;
        AbstractMap<String, Object> eventitem = null;

    	context.getLogger().info(String.format("Logic App Run Id: %s",request.getHeaders().get("x-ms-workflow-run-id")));
        context.getLogger().info(String.format("Transaction Id: %s", transactionId));
        context.getLogger().info(String.format("Reprocess: %s   --  Action: %s", reprocessCount, actionCount));
        
        synchronized(ReceiveJob.class) {
	    	if(!isInitialized) {
	    		init();
	    		loadEventsInCache(context.getLogger());
	    		context.getLogger().info(String.format("Receive Job was initialized. Events last modified %s ; eventsLastChecked %s ; eventsLastCached %s",
	    				eventsRef.getProperties().getLastModified(),
	    				new Date(eventsLastChecked),
	    				new Date(eventsLastCached)));
	    	}
        }
    	
		AbstractMap<String, String> inboundAttributes = null;
		try {
			if (Objects.isNull(attributes)) {
				inboundAttributes = new LinkedHashMap<String, String>();
			} else {
				inboundAttributes = (AbstractMap<String, String>) CreateToHashMap.getTransform("JSON").transform(attributes, null, context);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
        
        loadEventsInCache(context.getLogger());
        do{
				startevent = endevent;
	
				List<Object> events = eventsInCache.stream()
						.filter(n -> ((String) ((AbstractMap) n).get("name")).equals(sourceevent))
						.collect(Collectors.toList());

                if(events.size() == 0) {
                        break;
                }  

                eventitem = (AbstractMap<String, Object>)events.get(0);

                if(((AbstractList)eventitem.get("changes")).size() > 0 
                || ((AbstractList)eventitem.get("duplicates")).size() > 0){
                if(_request == null){
                        try {
                                TransformToHashMap toMap = CreateToHashMap.getTransform((String)eventitem.get("type"));
                                // will need to grab properties from mapping config at some point 
                                // might be difficult since we are not sure what mapping to do.  so may need to have in events page??
                                AbstractMap<String, Object> properties = new LinkedHashMap<>();

                                if (Objects.isNull(toMap)){ 

                                        PutLogs.log(transactionId, "Failed", "Receive process", key, gson.toJson(inboundAttributes), sourceevent, request.getHeaders().get("x-ms-workflow-run-id"), request.getHeaders().get("x-ms-workflow-name"), String.format("THIS SOURCE TYPE HAS NOT BEEN DEVELOPED YET: " + (String)eventitem.get("type")),
                                        actionCount, reprocessCount, context);

                                        return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                                                .body(String.format("THIS SOURCE TYPE HAS NOT BEEN DEVELOPED YET: " + (String)eventitem.get("type")))
                                                .build();
                                }
                                
                                context.getLogger().info("Transforming payload to object.");
                                context.getLogger().info(payload);
                                _request = toMap.transform(payload, properties, context);
                        }
                        catch(Exception e){

                                e.printStackTrace();

                                PutLogs.log(transactionId, "Failed", "Receive process", key, gson.toJson(inboundAttributes), sourceevent, request.getHeaders().get("x-ms-workflow-run-id"), request.getHeaders().get("x-ms-workflow-name"), String.format("THERE WAS A FAILURE IN TRANSFORMING PAYLOAD OF TYPE: " + (String)eventitem.get("type")),
                                actionCount, reprocessCount, context);

                                return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                                .body(String.format("THERE WAS A FAILURE IN TRANSFORMING PAYLOAD OF TYPE: " + (String)eventitem.get("type")))
                                .build();
                        }
                }
        }

                if(((AbstractList)eventitem.get("changes")).size() == 0){
                        break;
                }
        
                String event = startevent;
                try {
                    
                    for(AbstractMap<String, Object> item : ((AbstractList<AbstractMap<String, Object>>)eventitem.get("changes"))){
                        context.getLogger().info("Checking Case");
                        if(WhenThisCheck.check("AND", null, (AbstractList<AbstractMap<String, Object>>)item.get("when"), _request, null, "", _request, _request, null, new IndexTracker(), context)){
                             event = (String)item.get("name");   
                             context.getLogger().info(String.format("Found match: %s", event));
                                endevent = event;
                                continue;
                        }
                    }
                } catch (Exception e) {
                    //e.printStackTrace();
                    
                    PutLogs.log(transactionId, "Failed", "Receive process", key, gson.toJson(inboundAttributes), sourceevent, request.getHeaders().get("x-ms-workflow-run-id"), request.getHeaders().get("x-ms-workflow-name"), e.getMessage(),
                    actionCount, reprocessCount, context);

                    return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                            .body(e.getMessage())
                            .build();
                }
        }while(startevent != endevent);

        AbstractList<AbstractMap<String,Object>> events = new LinkedList<>();
        AbstractMap<String,Object> _temp = new LinkedHashMap<>();
        _temp.put("name",endevent);
        _temp.put("when",new LinkedList<Object>());
        events.add(_temp);
        
        if(!Objects.isNull(eventitem.get("duplicates")))
                events.addAll((AbstractList<AbstractMap<String,Object>>)eventitem.get("duplicates"));
        
        final Object testpayload = _request;
        final AbstractMap<String,String> iattributes = inboundAttributes;       
        
        PutLogs.log(transactionId, "Succeeded", "Receive process", key, gson.toJson(inboundAttributes), sourceevent,
                request.getHeaders().get("x-ms-workflow-run-id"), request.getHeaders().get("x-ms-workflow-name"), "OK",
                actionCount, reprocessCount, context);

        return request.createResponseBuilder(HttpStatus.OK)
        .header("Content-Type", "application/json")
        .body(gson.toJson(events.stream()
                .map(x -> (getAttributesRouting((String)x.get("name"),
                                (AbstractList<AbstractMap<String, Object>>)x.get("when"),
                                iattributes,
                                testpayload,
                                context)))
                .filter(x -> (!Objects.isNull(x)))
                .collect(Collectors.toList())))
        .build();

    }

	private static void loadEventsInCache(Logger logger) {
		if (eventsLastChecked + CACHE_CHECK_INTERVAL > System.currentTimeMillis())
			return;

		eventsLastChecked = System.currentTimeMillis();

		try {
			eventsRef.downloadAttributes();
			if (eventsRef.getProperties().getLastModified().getTime() < eventsLastCached)
				return;

			String eventsText = eventsRef.downloadText();
			eventsInCache = gson.fromJson(eventsText, new TypeToken<LinkedList<Object>>() {}.getType());
			eventsLastCached = eventsLastChecked;
			logger.info("Events cache was updated.");
		} catch (NullPointerException npe) {
			throw new IllegalStateException("ERRONEOUS BLOB CONTENT LOADING, MOST PROBABLY RECEIVEJOB WAS NOT LOADED PROPERLY!");
		} catch (Exception e) {
			throw new IllegalStateException("ERRONEOUS BLOB CONTENT LOADING, RECEIVEJOB CANNOT LOAD EVENTS CONFIGURATION PROPERLY!");
		}
	}

    private AbstractMap<String, Object> getAttributesRouting(String event, AbstractList<AbstractMap<String, Object>> when, AbstractMap<String,String> inboundAttributes, Object payload,final ExecutionContext context){
		String route = "unknown";

		AbstractMap<String, Object> routing = new LinkedHashMap<>();

		if (eventroute.containsKey(event)) {
			route = eventroute.get(event);
		} else {
			try {
				CloudBlockBlob blob = configuration.getBlockBlobReference(String.format("routing/%s", event));
				String configFile = blob.downloadText();
				AbstractMap<String, Object> config = gson.fromJson(configFile, new TypeToken<LinkedHashMap<String, Object>>() {}.getType());
				route = (String) config.get("type");
				eventroute.put(event, route);
			} catch (Exception e) {
				context.getLogger().info(e.getMessage());
				e.printStackTrace();
			}
		}

        routing.put("route",route);

        // get follows
        List<Object> events = eventsInCache.stream()
                        .filter(n -> ((String)((AbstractMap)n).get("name")).equals(event))
                        .collect(Collectors.toList());
        
        if(events.size()>0){
                routing.put("follows",((AbstractMap<String,Object>)events.get(0))
                                        .get("follows"));
        }
        else{
                routing.put("follows",new LinkedList<String>());
        }
                

        // can do test here to see if we want this payload
        try {
                if(when.size() > 0 && !(WhenThisCheck.check("AND", null, when, payload, null, "", payload, payload, null, new IndexTracker(), context))){
                        context.getLogger().info(String.format("This event %s is being filtered out.", event));
                        return null;
                }
        }
        catch(Exception e)
        {
                e.printStackTrace();
                return null;
        }
        // can grab attributes here from the given payload

        AbstractMap<String, Object> result = new LinkedHashMap<>();

        result.put("event",event);
        result.put("attributes",inboundAttributes);
        result.put("routing",routing);

        return result;
        
    }
}