package tccc.bib.connection;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.Session;
import javax.jms.TextMessage;
import org.apache.activemq.ActiveMQConnectionFactory;
import com.microsoft.azure.functions.*;
import java.util.LinkedHashMap;

public class PullMQ {

    static Session session = null; 
    static LinkedHashMap<String,MessageConsumer> consumers = new LinkedHashMap<>();
    
    public static String getPayload(String queue,String host, String user, String password, ExecutionContext context){
            
        MessageConsumer consumer = null;

        try {
            
            if(session == null){
                context.getLogger().info(String.format("Creating activemq connection."));
                ConnectionFactory connectionFactory = new ActiveMQConnectionFactory(host);
                Connection connection = connectionFactory.createConnection(user, password);
                connection.start();
                context.getLogger().info(String.format("Connection started."));
                session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            }
            if(consumers.containsKey(queue)){
                consumer = consumers.get(queue);
            }
            else{
                Destination destination = session.createQueue(queue);
                consumer = session.createConsumer(destination);
                consumers.put(queue, consumer);
            }
            
        }
        catch (Exception e){
            context.getLogger().info(String.format("Failed to create connection"));
            context.getLogger().info(e.getMessage());
            closeSession();
            e.printStackTrace();
        }

        String payload = "";
        if(consumer != null){
            try {
                context.getLogger().info(String.format("Pulling message from %s.", queue));
                Message message = consumer.receiveNoWait();
                if(message instanceof TextMessage){
                    context.getLogger().info(String.format("Got message from %s.", queue));
                    TextMessage textMessage = (TextMessage) message;
                    payload = textMessage.getText();
                } 
                else {
                    context.getLogger().info(String.format("No more messages from %s.", queue));
                } 
            }
            catch (JMSException e){
                closeSession();
                e.printStackTrace();
            }
        }
        else {
            closeSession();
            context.getLogger().info(String.format("Connection to queue: %s is not valid.", System.getenv("QUEUE_NAME")));
        }
    
        return payload;
    }

    private static void closeSession(){
        try{
            for(String key : consumers.keySet()){
                try{
                    consumers.get(key).close();
                }
                catch(Exception e){
                    e.printStackTrace();
                }
            }
            consumers.clear();
            if(session != null){
                session.close();
                session = null;
            }
            
        }
        catch(Exception e){
            consumers.clear();
            e.printStackTrace();
            session = null;
        }
    }
}


