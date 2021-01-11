package tccc.bib.help;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.Executors;

import org.joda.time.DateTime;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.eventhubs.EventData;
import com.microsoft.azure.eventhubs.EventHubClient;
import com.microsoft.azure.eventhubs.EventHubException;


/**
 * Azure Functions with HTTP Trigger.
 */
public class PutLogs {

    static EventHubClient ehClient;
    static Gson gson;

    static {
        gson = new GsonBuilder().create();
        try {
            ehClient = EventHubClient.createSync(System.getenv("eventhub"), Executors.newScheduledThreadPool(4));
        } catch (IllegalArgumentException | EventHubException | IOException e) {
            e.printStackTrace();
            ehClient = null;
        }
    }

    public static void log(String transactionId, 
                        String status,
                        String action,
                        String payload,
                        String attributes,
                        String event,
                        String runId,
                        String workflowName,
                        String message,
                        Integer actionCount,
                        Integer reprocessCount,
                        final ExecutionContext context) {

        if(Objects.isNull(ehClient)){
            context.getLogger().severe("Connection to Eventhub failed!");
            return;
        }

        String messageId = UUID.randomUUID().toString();
        String eventTime = DateTime.now().toString("yyyy-MM-dd'T'HH:mm:ss.SSS");

        LinkedHashMap<String,Object> data = new LinkedHashMap<>();
        data.put("runId",runId);
        data.put("workflowName",workflowName);
        data.put("status", status);
        data.put("transaction_id",transactionId);
        data.put("action",action);
        data.put("payload",payload);
        data.put("attributes",attributes);
        data.put("event",event);
        data.put("message",message);
        data.put("actionCount", actionCount);
        data.put("reprocessCount", reprocessCount);
        data.put("eventTime",eventTime);
        
        byte[] payloadBytes = gson.toJson(data).getBytes(Charset.defaultCharset());
        EventData sendEvent = EventData.create(payloadBytes);

        Boolean successful = false;
        Integer count = 0;

        while(!successful && count < 5){
            try {
                ehClient.sendSync(sendEvent);
                successful=true;
            }
            catch (EventHubException e){
                count++;
                context.getLogger().severe(e.getMessage());
            }
        }


        context.getLogger().info(String.format("Log Pushed: {"+
                "\"submit\":\"%s\", " +
                "\"eventTime\":\"%s\" " + 
                "\"mid\":\"%s\", " +
                "\"tid\":\"%s\", " +
                "\"status\":\"%s\", " + 
                "\"action\":\"%s\", " + 
                "\"payload\":\"%s\", " + 
                "\"attributes\":\"%s\", " + 
                "\"event\":\"%s\", " + 
                "\"runid\":\"%s\", " + 
                "\"workflow\":\"%s\", " + 
                "\"message\":\"%s\" " + 
                "\"actionCount\":%d " + 
                "\"reprocessCount\":%d " + 
                "}",
                (successful?"success":"failure"), eventTime, messageId,
                transactionId, status, action, payload, 
                attributes, event, runId,workflowName, message,
                actionCount,reprocessCount));
    }
}

