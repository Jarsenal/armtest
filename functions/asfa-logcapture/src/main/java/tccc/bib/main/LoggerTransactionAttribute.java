package tccc.bib.main;

import java.util.*;
import com.microsoft.azure.functions.annotation.*;
import com.microsoft.azure.functions.*;
import com.google.gson.Gson;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.DriverManager;
import java.sql.PreparedStatement;



/**
 * Azure Functions with HTTP Trigger.
 */
public class LoggerTransactionAttribute {

    //static Connection connection = null;
   
    @FunctionName("Logger-Extract-Transaction-Attribute")
    public HttpResponseMessage run(@HttpTrigger(name = "req", methods = {
            HttpMethod.GET }, authLevel = AuthorizationLevel.FUNCTION) HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {
        context.getLogger().info("Java HTTP trigger processed a request.");

        AbstractList<AbstractMap<String,String>> output = new LinkedList<>();   
        
        
        String event = Optional.ofNullable(request.getQueryParameters().get("event")).orElse("");
        String attribute = Optional.ofNullable(request.getQueryParameters().get("attribute")).orElse("");
        Integer span = Integer.parseInt("-" + Optional.ofNullable(request.getQueryParameters().get("span")).orElse("24"));
        String start = Optional.ofNullable(request.getQueryParameters().get("start")).orElse("2100-12-30 12:00:00.000");
        Integer recordlimit = Integer.parseInt(Optional.ofNullable(request.getQueryParameters().get("recordlimit")).orElse("25"));



        if(attribute.isEmpty())
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                .header("Content-Type","application/json")
                .body("{\"message\":\"Please pass a value in the attribute\"}")
                .build();

        String hostName = System.getenv("SQL_HOST");
        String dbName = System.getenv("SQL_DB");
        String user = System.getenv("SQL_USER");
        String password = System.getenv("SQL_PWD");
        
        String url = String.format("jdbc:sqlserver://%s:1433;database=%s;user=%s;password=%s;encrypt=true;"
            + "hostNameInCertificate=*.database.windows.net;loginTimeout=30;", hostName, dbName, user, password);
            
        //Connect to database
        try (final Connection connection = DriverManager.getConnection(url)) {
            
            String selectSql =  "SELECT TOP (?) [transactionId],[action],[event],[blobKey],[attributes],[insertDateTime],[status],[runid],[workflow],[eventMessage] " +
                                "FROM [dbo].[Transactions] " + 
                                "WHERE ( '' = ? OR [event] = ?)  AND " + 
                                "([insertDateTime] > DATEADD(hour, ?, GETDATE())) AND" + 
                                "([insertDateTime] < ?) AND" + 
                                "([attributes] like ?) " + 
                                "order by insertDateTime desc";
            
            //Statement statement = connection.createStatement();
            PreparedStatement statement = connection.prepareStatement(selectSql);
            statement.setInt(1, recordlimit);
            statement.setString(2, event);
            statement.setString(3, event);
            statement.setInt(4, span);
            statement.setString(5, start);
            statement.setString(6, String.format("%%%s%%",attribute));
            
            
            ResultSet results = statement.executeQuery();
            while (results.next())
                {
                    AbstractMap<String,String> rec = new LinkedHashMap<>();
                    rec.put("status",results.getString("status"));
                    rec.put("event",results.getString("event"));
                    rec.put("transactionId",results.getString("transactionId"));
                    rec.put("action",results.getString("action"));
                    rec.put("blobKey",results.getString("blobKey"));
                    rec.put("attributes",results.getString("attributes"));
                    rec.put("insertDateTime",results.getString("insertDateTime"));
                    rec.put("runid",results.getString("runid"));
                    rec.put("workflow",results.getString("workflow"));
                    rec.put("message",results.getString("eventMessage"));
                    output.add(rec);
                }

            context.getLogger().info("Succefully made the call");
        }
        catch (Exception e) {
            e.printStackTrace();
            context.getLogger().info(e.getMessage());

            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                .header("Content-Type","application/json")
                .body("There was an error when extracing transactions, please try again.")
                .build();
        }


        return request.createResponseBuilder(HttpStatus.OK)
                .header("Content-Type","application/json")
                .body(new Gson().toJson(output))
                .build();
    }

   
    
}
