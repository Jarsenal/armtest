package tccc.bib.main;

import java.util.*;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.activemq.ActiveMQConnectionFactory;

import com.microsoft.azure.functions.annotation.*;
import com.microsoft.azure.functions.*;

/**
 * Azure Functions with HTTP Trigger.
 */
public class ActiveQueueConsumer {
	
	@FunctionName("activemq-jms-queue-consumer")
    public HttpResponseMessage run(
            @HttpTrigger(name = "req", methods = {HttpMethod.GET}, authLevel = AuthorizationLevel.FUNCTION) HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {
    	
        context.getLogger().info("Azure HTTP trigger processed a request.");
        
        Integer timeout = Integer.parseInt(System.getenv("QUEUE_TIMEOUT"));

        // get queue
        String queuename = request.getQueryParameters().get("queuename");
        
        if(queuename == null || queuename == "") {
            request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                .body("Pass queue name")
                .build();
        }

        context.getLogger().info(String.format("Pulling message from queue: %s",queuename));
        

        ConnectionFactory connectionFactory = new ActiveMQConnectionFactory(System.getenv("ACTIVEMQ_HOST"));
        String responseBody = "";

        try {

            Connection connection = connectionFactory.createConnection(System.getenv("ACTIVEMQ_USER"),System.getenv("ACTIVEMQ_PASSWORD"));
            connection.start();

            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Destination destination = session.createQueue(queuename);
            MessageConsumer consumer = session.createConsumer(destination);
        
            context.getLogger().info(String.format("Retrieving message."));
        
            Message message = consumer.receive(timeout);
            
            if (message instanceof TextMessage) {
                TextMessage textMessage = (TextMessage) message;
                responseBody = textMessage.getText();
                context.getLogger().info(String.format("Received message"));
            }
            
            connection.close();
        }
        catch (JMSException e){
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(e.getMessage())
            .build();
        }

        return request.createResponseBuilder(HttpStatus.OK).body(responseBody).build();
    }
}
