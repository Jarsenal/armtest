package tccc.bib.main;

import java.util.*;
import java.util.stream.Collectors;

import com.microsoft.azure.functions.annotation.*;

import org.joda.time.DateTime;

import tccc.bib.tools.JsonToObject;
import tccc.bib.tools.ObjectToJson;

import com.microsoft.azure.functions.*;

import com.microsoft.azure.eventgrid.EventGridClient;
import com.microsoft.azure.eventgrid.TopicCredentials;
import com.microsoft.azure.eventgrid.implementation.EventGridClientImpl;
import com.microsoft.azure.eventgrid.models.EventGridEvent;

import java.sql.Connection;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.DriverManager;



/**
 * Azure Functions with HTTP Trigger.
 */
public class Reprocess {

    
    @FunctionName("Reprocess-Job")
    public HttpResponseMessage run(@HttpTrigger(name = "req", methods = {
            HttpMethod.POST }, authLevel = AuthorizationLevel.FUNCTION) HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {
        context.getLogger().info("Java HTTP trigger processed a request.");

        AbstractList<AbstractMap<String,Object>> transactions = JsonToObject.from(request.getBody().orElse("[]"));

        TopicCredentials topicCredentials = new TopicCredentials(System.getenv("EVENT_KEY"));
        String eventGridEndpoint = System.getenv("EVENT_HOST");
            

        //Connect to eventgrid
        try {
            // Create an event grid client.
            EventGridClient client = new EventGridClientImpl(topicCredentials);

            List<EventGridEvent> eventsList = transactions.stream()
                .map(transaction -> createEvent(
                        (String)transaction.get("transaction"),
                        (String)transaction.get("event"),
                        ((Double)transaction.get("reprocessCount")).intValue()
                    ))
                .collect(Collectors.toList());

            client.publishEvents(eventGridEndpoint,eventsList);

            context.getLogger().info("Succefully made the call");
            
        }
        catch (Exception e) {
            e.printStackTrace();
            context.getLogger().info(e.getMessage());
        }


        return request.createResponseBuilder(HttpStatus.OK)
                .header("Content-Type","text/plain")
                .body("Successfully pushed transactions!")
                .build();
    }

    private EventGridEvent createEvent(String transaction, String event, Integer reprocessCount) {
        LinkedHashMap<String,Object> data = new LinkedHashMap<>();
        data.put("attributes","{}");
        data.put("event",event);
        data.put("transaction",transaction);
        data.put("reprocessCount", reprocessCount);
        data.put("actionCount",new Integer(0));
        data.put("key",String.format("transactions/received/%s/%s",event,transaction));
        return new EventGridEvent( transaction, event, data, "Reprocess", DateTime.now(), "2.0");
    }


    @FunctionName("Find-Source-Event")
    public HttpResponseMessage runFindSource(@HttpTrigger(name = "req", methods = {
            HttpMethod.POST }, authLevel = AuthorizationLevel.FUNCTION) HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {
        context.getLogger().info("Java HTTP trigger processed a request.");

        AbstractList<String> transactions = JsonToObject.from(request.getBody().orElse("[]"));
        AbstractList<AbstractMap<String,String>> output = new LinkedList<>();
        
        String hostName = System.getenv("SQL_HOST");
        String dbName = System.getenv("SQL_DB");
        String user = System.getenv("SQL_USER");
        String password = System.getenv("SQL_PWD");
        
        String url = String.format("jdbc:sqlserver://%s:1433;database=%s;user=%s;password=%s;encrypt=true;"
            + "hostNameInCertificate=*.database.windows.net;loginTimeout=30;", hostName, dbName, user, password);
        
       //Connect to database
       try (final Connection connection = DriverManager.getConnection(url)) {
 
                String selectSql = String.format(
                "SELECT DISTINCT [transactionId], [event], MAX([reprocessCount]) reprocessCount " +
                "FROM [dbo].[Transactions] " +
                "WHERE [transactionId] IN ('%s') " +
                "AND actionCount < 30 " + 
                "GROUP BY [transactionId], [event]", String.join("','",transactions) );

            context.getLogger().info(String.format("Query: %s", selectSql));
            
            Statement statement = connection.createStatement();
            ResultSet results = statement.executeQuery(selectSql);

            while (results.next())
            {
                AbstractMap<String,String> rec = new LinkedHashMap<>();
                rec.put("event",results.getString("event"));
                rec.put("transactionId",results.getString("transactionId"));
                rec.put("reprocessCount",results.getString("reprocessCount"));
                output.add(rec);
            }

            context.getLogger().info("Succefully made the call");
        }
        catch (Exception e) {
            e.printStackTrace();
            context.getLogger().info(e.getMessage());
        }

        return request.createResponseBuilder(HttpStatus.OK)
            .header("Content-Type","application/json")
            .body(ObjectToJson.from(output))
            .build();
    }

    
}
