package tccc.bib.main;

import java.util.*;
import com.microsoft.azure.functions.annotation.*;

import tccc.bib.help.GetConfig;
import tccc.bib.connection.PullMQ;
import tccc.bib.connection.SendHTTP;
import tccc.bib.connection.SendSBQueue;

import com.microsoft.azure.functions.*;
import com.google.gson.Gson;

/**
 * Azure Functions with HTTP Trigger.
 */
public class ActiveQueueSchedule {

    static AbstractList<Object> eventlist = null;
    static final Gson gson = new Gson();
    static final AbstractMap<String, String> eventroute = new LinkedHashMap<>();
    
	@FunctionName("activemq-jms-queue-schedule-critical")
    public void runCritical(
        @TimerTrigger(name = "keepAliveTrigger", schedule = "0 0/1 * * * *") String timerInfo,
        ExecutionContext context
        ) {

        context.getLogger().info("Start critical job.");
        
        AbstractList<AbstractMap<String,String>> events = GetConfig.pull("mqevents");
        
        context.getLogger().info(String.format("Extracted events from storage with size of: %d", events.size()));

        events.stream()
            .filter(x -> x.get("type").equals("critical"))
            .forEach(x -> runJob(x.get("key"), String.format("%s&event=%s",System.getenv("URL_TARGET_QUEUE_CRITICAL"), x.get("event")), context));
        context.getLogger().info(String.format("End Function"));
    }
    
	@FunctionName("activemq-jms-queue-schedule-rapid")
    public void runRapid(
        @TimerTrigger(name = "keepAliveTrigger", schedule = "0/5 * * * * *") String timerInfo,
        ExecutionContext context
        ) {

        context.getLogger().info("Start rapid job.");
        
        AbstractList<AbstractMap<String,String>> events = GetConfig.pull("mqevents");
        
        context.getLogger().info(String.format("Extracted events from storage with size of: %d", events.size()));

        events.stream()
            .filter(x -> x.get("type").equals("rapid"))
            .forEach(x -> runJob(x.get("key"), String.format("%s&event=%s",System.getenv("URL_TARGET_STREAM"), x.get("event")), context));
        context.getLogger().info(String.format("End Function"));
    }

    @FunctionName("activemq-jms-queue-schedule-batch")
    public void runBatch(
        @TimerTrigger(name = "keepAliveTrigger", schedule = "0 0/15 * * * *") String timerInfo,
        ExecutionContext context
        ) {

        context.getLogger().info("Start batch job.");
        
        AbstractList<AbstractMap<String,String>> events = GetConfig.pull("mqevents");

        context.getLogger().info("Extracted events from storage.");

        
        events.stream()
            .filter((x) -> x.get("type").equals("batch"))
            .forEach(x -> runJob(x.get("key"),String.format("%s&event=%s",System.getenv("URL_TARGET_QUEUE"), x.get("event")), context));
        context.getLogger().info(String.format("End Function"));
    }

    @FunctionName("activemq-jms-queue-schedule-sequenced")
    public void runsequenced(
        @TimerTrigger(name = "keepAliveTrigger", schedule = "0 0/15 * * * *") String timerInfo,
        ExecutionContext context
        ) {

        context.getLogger().info("Start batch job.");
        
        AbstractList<AbstractMap<String,String>> events = GetConfig.pull("mqevents");

        context.getLogger().info("Extracted events from storage.");

        events.stream()
            .filter((x) -> x.get("type").equals("sequenced"))
            .forEach(x -> runJobSequenced(x.get("key"),System.getenv("SBQueueName"),x.get("event"),context));
        context.getLogger().info(String.format("End Function"));
    }

    private void runJob(String queue, String url, ExecutionContext context){
        
        context.getLogger().info(String.format("Start job with queue: %s to url: %s", queue, url));

        String payload ="";

        do{
            payload = PullMQ.getPayload( 
                    queue,
                    System.getenv("ACTIVEMQ_HOST"),
                    System.getenv("ACTIVEMQ_USER"), 
                    System.getenv("ACTIVEMQ_PASSWORD"),
                    context);
                if (!payload.isEmpty()) {        
                    String transactionId = SendHTTP.sendPayload(payload, url, context);                                        
                    context.getLogger().info("Sent message with transactionId: " + transactionId);
                }
        }
        while(!payload.isEmpty());
        context.getLogger().info(String.format("End Job"));
    }

    private void runJobSequenced(String queue, String sbQueue, String event, ExecutionContext context){
        
        context.getLogger().info(String.format("Start job with queue: %s to SB Queue: %s", queue, sbQueue));

        String payload ="";

        do{
            payload = PullMQ.getPayload( 
                    queue,
                    System.getenv("ACTIVEMQ_HOST"),
                    System.getenv("ACTIVEMQ_USER"), 
                    System.getenv("ACTIVEMQ_PASSWORD"),
                    context);
                if (!payload.isEmpty()) {        
                    String transactionId = SendSBQueue.sendPayload(System.getenv("ConnectionString"), event,sbQueue, payload, context);
                    context.getLogger().info("Sent message with transactionId: " + transactionId);
                }
        }
        while(!payload.isEmpty());
        context.getLogger().info(String.format("End Job"));
    }

    
}
