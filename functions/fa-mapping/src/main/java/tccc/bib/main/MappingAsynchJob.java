package tccc.bib.main;

import java.io.IOException;
import java.util.*;
import tccc.bib.tools.*;

import tccc.bib.transform.factories.CreateFromHashMap;
import tccc.bib.transform.factories.CreateToHashMap;
import tccc.bib.transform.interfaces.TransformFromHashMap;
import tccc.bib.transform.interfaces.TransformToHashMap;
import com.microsoft.azure.functions.annotation.*;
import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.queue.CloudQueue;
import com.microsoft.azure.storage.queue.CloudQueueClient;
import com.microsoft.azure.storage.queue.CloudQueueMessage;
import com.microsoft.azure.functions.*;

import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import groovy.lang.Tuple3;

import org.apache.http.entity.StringEntity;

public class MappingAsynchJob {

	private static final CloseableHttpClient httpclient = HttpClients.createDefault();

	@FunctionName("RunMappingAsynchJob")
	@StorageAccount("BLOBSTORAGE")
	public HttpResponseMessage run(@HttpTrigger(name = "req", methods = { HttpMethod.POST }, authLevel = AuthorizationLevel.FUNCTION) HttpRequestMessage<Optional<String>> request,
			final ExecutionContext context) {

		context.getLogger().info(String.format("Logic App Run Id: %s", request.getHeaders().get("x-ms-workflow-run-id")));
		

		String payload = request.getBody().orElse("");

		if (payload.isEmpty()) {
			context.getLogger().severe(request.getBody().orElse("Payload was empty!"));
			return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR).body("Payload is missing!").build();
		}

		AbstractMap<String, String> _request = JsonToObject.from(payload);

		context.getLogger().info(String.format("Transaction Id: %s", _request.get("transaction")));

		Integer actionCount = Integer.parseInt(_request.get("actionCount"));
		Integer reprocessCount = Integer.parseInt(_request.get("reprocessCount"));

		context.getLogger().info(String.format("Reprocess: %d   --  Action: %d", reprocessCount, actionCount));

		// get values
		String attributes = _request.get("attributes");
		String transactionId = _request.get("transaction");
		String source = _request.get("source");
		String event = _request.get("event");

		// add values
		_request.put("run", request.getHeaders().get("x-ms-workflow-run-id"));
		_request.put("flow", request.getHeaders().get("x-ms-workflow-name"));
		_request.put("actionCount", Integer.toString(Integer.sum(actionCount, 1)));

		String queuename = "biib-mapping-batch";
		switch (request.getQueryParameters().get("type")) {
		case "reprocess":
			queuename = "biib-mapping-reprocess";
			break;
		case "critical":
			queuename = "biib-mapping-critical";
			break;
		}

		String storageConnectionString = 
				String.format("DefaultEndpointsProtocol=https;AccountName=%s;AccountKey=%s", System.getenv("storageAccountName"), System.getenv("storageAccountKey"));

		try {
			CloudStorageAccount storageAccount = CloudStorageAccount.parse(storageConnectionString);
			CloudQueueClient queueClient = storageAccount.createCloudQueueClient();
			CloudQueue queue = queueClient.getQueueReference(queuename);
			CloudQueueMessage message = new CloudQueueMessage(ObjectToJson.from(_request));
			queue.addMessage(message);
			context.getLogger().info(String.format("Message was placed into queue: %s",queuename));
		} catch (Exception e) {
			context.getLogger().severe(("There was a failure: " + e.getMessage()));

			PutLogs.log(transactionId, "Failed", "Mapping publish", source, attributes, event,
					request.getHeaders().get("x-ms-workflow-run-id"), request.getHeaders().get("x-ms-workflow-name"),
					String.format("Failed to put message into queue with error: %s", e.getMessage()), actionCount,
					reprocessCount, context);

			return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred!").build();
		}

		PutLogs.log(transactionId, "Succeeded", "Mapping publish", source, attributes, event,
				request.getHeaders().get("x-ms-workflow-run-id"), request.getHeaders().get("x-ms-workflow-name"), "OK",
				actionCount, reprocessCount, context);

		return request.createResponseBuilder(HttpStatus.OK).header("attributes", "{}").body("OK").build();

	}

	@FunctionName("AsyncQueuedJobCritical")
	public void runAsyncQueuedJobCritical(@QueueTrigger(name = "msg", queueName = "biib-mapping-critical", connection = "BLOBSTORAGE") String message,
			final ExecutionContext context) {
		
		AbstractMap<String, String> request = JsonToObject.from(message);
		
		context.getLogger().info(message);
		context.getLogger().info(String.format("Logic App Run Id: %s", request.get("run")));
		context.getLogger().info(String.format("Transaction Id: %s", request.get("transaction")));

		Integer actionCount = Integer.parseInt(request.get("actionCount"));
		Integer reprocessCount = Integer.parseInt(request.get("reprocessCount"));

		context.getLogger().info(String.format("Reprocess: %d   --  Action: %d", reprocessCount, actionCount));

		Tuple3<String, String, String> result = runMappingJob(request.get("event"), request.get("source"), request.get("target"), request.get("attributes"), context);
		
		Boolean success = false;
		Integer count = 0;
		while (!success && count < 3) {
			count++;
			try {
				PutLogs.log(request.get("transaction"), result.getFirst(), "Mapping job", request.get("target"),
						result.getThird(), request.get("event"), request.get("run"), request.get("flow"),
						result.getSecond(), actionCount, reprocessCount, context);

				sendCallBack(result.getFirst(), result.getSecond(), result.getThird(), request.get("callback"), context);
				success = true;

			} catch (IOException e) {
				context.getLogger().warning(String.format("Failed to send message count %d back to callback: %s", count, request.get("callback")));
				context.getLogger().warning(String.format("Callback Error: %s", e.getMessage()));
			}
		}
	}

	@FunctionName("AsyncQueuedJobBatch")
	public void runAsyncQueuedJobBatch(@QueueTrigger(name = "msg", queueName = "biib-mapping-batch", connection = "BLOBSTORAGE") String message,
			final ExecutionContext context) {
		
		AbstractMap<String, String> request = JsonToObject.from(message);

		context.getLogger().info(message);
		context.getLogger().info(String.format("Logic App Run Id: %s", request.get("run")));
		context.getLogger().info(String.format("Transaction Id: %s", request.get("transaction")));

		Integer actionCount = Integer.parseInt(request.get("actionCount"));
		Integer reprocessCount = Integer.parseInt(request.get("reprocessCount"));

		context.getLogger().info(String.format("Reprocess: %d   --  Action: %d", reprocessCount, actionCount));

		Tuple3<String, String, String> result = runMappingJob(request.get("event"), request.get("source"), request.get("target"), request.get("attributes"), context);

		Boolean success = false;
		Integer count = 0;
		while (!success && count < 3) {
			count++;
			try {
				PutLogs.log(request.get("transaction"), result.getFirst(), "Mapping job", request.get("target"),
						result.getThird(), request.get("event"), request.get("run"), request.get("flow"),
						result.getSecond(), actionCount, reprocessCount, context);

				sendCallBack(result.getFirst(), result.getSecond(), result.getThird(), request.get("callback"), context);
				success = true;

			} catch (IOException e) {
				context.getLogger().warning(String.format("Failed to send message count %d back to callback: %s", count, request.get("callback")));
				context.getLogger().warning(String.format("Callback Error: %s", e.getMessage()));
			}
		}
	}

	@FunctionName("AsyncQueuedJobReprocess")
	public void runAsyncQueuedJobReprocess(@QueueTrigger(name = "msg", queueName = "biib-mapping-reprocess", connection = "BLOBSTORAGE") String message,
			final ExecutionContext context) {

		AbstractMap<String, String> request = JsonToObject.from(message);

		context.getLogger().info(message);
		context.getLogger().info(String.format("Logic App Run Id: %s", request.get("run")));
		context.getLogger().info(String.format("Transaction Id: %s", request.get("transaction")));

		Integer actionCount = Integer.parseInt(request.get("actionCount"));
		Integer reprocessCount = Integer.parseInt(request.get("reprocessCount"));

		context.getLogger().info(String.format("Reprocess: %d   --  Action: %d", reprocessCount, actionCount));

		Tuple3<String, String, String> result = runMappingJob(request.get("event"), request.get("source"), request.get("target"), request.get("attributes"), context);

		Boolean success = false;
		Integer count = 0;
		while (!success && count < 3) {
			count++;
			try {
				PutLogs.log(request.get("transaction"), result.getFirst(), "Mapping job", request.get("target"),
						result.getThird(), request.get("event"), request.get("run"), request.get("flow"),
						result.getSecond(), actionCount, reprocessCount, context);

				sendCallBack(result.getFirst(), result.getSecond(), result.getThird(), request.get("callback"), context);
				success = true;

			} catch (IOException e) {
				context.getLogger().warning(String.format("Failed to send message count %d back to callback: %s", count, request.get("callback")));
				context.getLogger().warning(String.format("Callback Error: %s", e.getMessage()));
			}
		}
	}

	private Tuple3<String, String, String> runMappingJob(String event, String inputKey, String outputKey, String attributes, final ExecutionContext context) {

		context.getLogger().info("Extracting mapping config for event: " + event);
		
		AbstractMap<String, Object> fullMappingConfig = GetConfig.pull("mapping/" + event);
		AbstractMap<String, Object> attributesMappingCongig = JsonToObject.from((String) fullMappingConfig.get("mappingProperty"));
		AbstractMap<String, Object> payloadMappingConfig = JsonToObject.from((String) fullMappingConfig.get("mapping"));
		String sourceType = (String) fullMappingConfig.get("type");
		String targetType = (String) fullMappingConfig.get("response");
		Boolean test = Optional.ofNullable((Boolean) fullMappingConfig.get("test")).orElse(false);

		AbstractMap<String, Object> properties = new LinkedHashMap<>();
		properties.put("sourceFF", JsonToObject.from((String) fullMappingConfig.get("sourceFF")));
		properties.put("targetFF", JsonToObject.from((String) fullMappingConfig.get("targetFF")));
		properties.put("root", (String) payloadMappingConfig.get("name"));
		properties.put("sourceHeader", MapReader.read(payloadMappingConfig, "source.header"));
		properties.put("sourceQuotes", MapReader.read(payloadMappingConfig, "source.quotes"));
		properties.put("sourceDelimiter", MapReader.read(payloadMappingConfig, "source.delimiter"));
		properties.put("targetHeader", MapReader.read(payloadMappingConfig, "target.header"));
		properties.put("targetQuotes", MapReader.read(payloadMappingConfig, "target.quotes"));
		properties.put("targetDelimiter", MapReader.read(payloadMappingConfig, "target.delimiter"));

		attributes = Optional.ofNullable(attributes).orElse("{}");
		String denormalizedPayload = "";
		String mappingStatus = "Succeeded";
		String mappingMessage = "OK";
		String denormalizedAttributes = attributes;

		try {
			TransformToHashMap sourceTransform = CreateToHashMap.getTransform(sourceType);
			if (sourceTransform == null)
				throw new Exception(String.format("THIS SOURCE TYPE HAS NOT BEEN DEVELOPED YET: " + sourceType));
			
			TransformFromHashMap targetTransform = CreateFromHashMap.getTransform(targetType);
			if (targetTransform == null)
				throw new Exception(String.format("THIS TARGET TYPE HAS NOT BEEN DEVELOPED YET: " + sourceType));

			String inboundMessage = GetTransaction.pull(inputKey);
			if (inboundMessage == null)
				return new Tuple3<String, String, String>("Failed", "Payload is missing!", attributes);



			context.getLogger().info("Start mapping service. Transforming from " + sourceType + " to " + targetType);

			Object normalizedPayload = sourceTransform.transform(inboundMessage, properties, context);
			AbstractMap<String, Object> normalizedAttributes = (AbstractMap<String, Object>) CreateToHashMap.getTransform("JSON").transform(attributes, null, context);
			
			Object mappedPayload = Mapping.run(payloadMappingConfig, normalizedPayload, normalizedAttributes, test, context);
			AbstractMap<String, Object> mappedAttributes = (AbstractMap<String, Object>) Mapping.run(attributesMappingCongig, normalizedPayload, normalizedAttributes, test, context);

			// carry inbound attributes with outbound
			if (normalizedAttributes != null)
				for (String k : normalizedAttributes.keySet())
					if (!mappedAttributes.containsKey(k))
						mappedAttributes.put(k, normalizedAttributes.get(k));

			denormalizedAttributes = CreateFromHashMap.getTransform("JSON").transform(mappedAttributes, null, context);
			denormalizedPayload = targetTransform.transform(mappedPayload, properties, context);

		} catch (Exception e) {
			e.printStackTrace();

			StringBuilder builder = new StringBuilder();
			for (StackTraceElement element : e.getStackTrace()) {
				builder.append(element.toString());
				builder.append("\n");
			}

			denormalizedPayload = builder.toString();

			mappingStatus = "Failed";
			mappingMessage = e.getMessage();
		}

		try {
			PutTransaction.push(outputKey, denormalizedPayload);
		} catch (Exception e) {
			e.printStackTrace();
		}

		context.getLogger().info("End mapping job.");

		return new Tuple3<String, String, String>(mappingStatus, mappingMessage, denormalizedAttributes);

	}

	private void sendCallBack(String status, String message, String attributes, String uri, final ExecutionContext context) throws ClientProtocolException, IOException {
		
		HttpPost httppost = new HttpPost(uri);
		httppost.setHeader("attributes", attributes);
		httppost.setHeader("statusoverride", status.toString());
		httppost.setEntity(new StringEntity(message));

		ResponseHandler<String> responseHandler = (response) -> {
			context.getLogger().info("Status: " + status);
			HttpEntity entity = response.getEntity();
			return EntityUtils.toString(entity);
		};

		context.getLogger().info(String.format("Making the request to the given url: %s", uri));
		String response = httpclient.execute(httppost, responseHandler);
		context.getLogger().info(String.format("Result: %s", response));
	}

}