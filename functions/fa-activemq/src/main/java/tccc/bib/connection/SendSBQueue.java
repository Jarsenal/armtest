package tccc.bib.connection;

import java.util.*;
import com.microsoft.azure.functions.*;
import java.io.IOException;

import com.microsoft.azure.servicebus.*;
import com.microsoft.azure.servicebus.primitives.ConnectionStringBuilder;

import static java.nio.charset.StandardCharsets.*;

import java.time.Duration;
import java.util.concurrent.*;

import java.util.UUID;

public class SendSBQueue {
    public static String sendPayload(String connectionString, String event, String queueName, String payload, ExecutionContext context) {
        
        Message message = new Message(payload);       
        message.setMessageId(UUID.randomUUID().toString());
        //message.setTimeToLive(Duration.ofMinutes(2));
        message.setSessionId(event);
        
        try {

        //Initialize an instance of QueueClient object to invoke Azure Service Bus APIs
        QueueClient sendClient = new QueueClient(new ConnectionStringBuilder(connectionString, queueName), ReceiveMode.PEEKLOCK);
        
        context.getLogger().info(String.format("Received message to send to SB Queue"));                       
        context.getLogger().info(String.format("Message sending: Id = " + message.getMessageId()));

            //send message asynchronously and close connection            
            sendClient.sendAsync(message).thenRunAsync(() -> {
                context.getLogger().info(String.format("Message acknowledged: Id = " + message.getMessageId()));
                sendClient.closeAsync();
             });

        } catch (Exception e) {

            context.getLogger().info(String.format("Failed to send message : Id = " + message.getMessageId()));
            context.getLogger().info(e.getMessage());
            e.printStackTrace();
            return "FAILED";
        }    

            return message.getMessageId();
    }

}