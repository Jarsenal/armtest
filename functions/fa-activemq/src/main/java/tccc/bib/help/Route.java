package tccc.bib.help;

import tccc.bib.objects.Request;
import tccc.bib.objects.Response;
import java.util.AbstractMap;
import javax.jms.MessageProducer;
import com.microsoft.azure.functions.*;
import org.apache.activemq.ActiveMQConnectionFactory;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.Session;
import javax.jms.TextMessage;

public class Route {
    
    public static Response send(Request request,final ExecutionContext context)
        throws Exception{
        
        AbstractMap<String, Object> routeConfig = request.getConfig();
        
        String messageBody = request.getPayload();
        String queuename = (String)routeConfig.get("queueName");
        
        if(queuename == null || queuename.isEmpty()) {
           throw new Exception("Queue name is not being passed!");
        }

        if (messageBody == null || messageBody.isEmpty()) {
           throw new Exception("Payload is empty!");
        }
        
        ConnectionFactory connectionFactory = 
            new ActiveMQConnectionFactory(System.getenv("ACTIVEMQ_HOST"));
        Connection connection = connectionFactory.createConnection(System.getenv("ACTIVEMQ_USER"),System.getenv("ACTIVEMQ_PASSWORD"));
        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        context.getLogger().info(String.format("Queue: %s", queuename));
        Destination destination = session.createQueue(queuename);
        MessageProducer producer = session.createProducer(destination);
        TextMessage message = session.createTextMessage(messageBody);
        producer.send(message);
        connection.close();
        
        return new Response("{\"message\":\"success\"}",request.getAttributes());
    }
}