package tccc.bib.main;

import java.util.*;
import com.microsoft.azure.functions.annotation.*;
import com.microsoft.azure.functions.*;
import com.google.gson.Gson;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.DriverManager;



/**
 * Azure Functions with HTTP Trigger.
 */
public class LoggerTransaction {

    //static Connection connection = null;
   
    @FunctionName("Logger-Extract-Transaction")
    public HttpResponseMessage run(@HttpTrigger(name = "req", methods = {
            HttpMethod.GET }, authLevel = AuthorizationLevel.FUNCTION) HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {
        context.getLogger().info("Java HTTP trigger processed a request.");

        AbstractList<AbstractMap<String,String>> output = new LinkedList<>();   
        
        
        //String event = Optional.ofNullable(request.getQueryParameters().get("event")).orElse("");
        String transaction = Optional.ofNullable(request.getQueryParameters().get("transaction")).orElse("");
            
        //Connect to database
        try {

            String hostName = System.getenv("SQL_HOST");
            String dbName = System.getenv("SQL_DB");
            String user = System.getenv("SQL_USER");
            String password = System.getenv("SQL_PWD");
            
            String url = String.format("jdbc:sqlserver://%s:1433;database=%s;user=%s;password=%s;encrypt=true;"
                + "hostNameInCertificate=*.database.windows.net;loginTimeout=30;", hostName, dbName, user, password);
            final Connection connection = DriverManager.getConnection(url);     
        

            String selectSql = String.format("SELECT [action],[event],[blobKey],[attributes],[insertDateTime],[status],[runid],[workflow],[eventMessage] FROM [dbo].[Transactions] WHERE [transactionId] = '%s' order by insertDateTime desc", transaction);
            
            Statement statement = connection.createStatement();
            ResultSet results = statement.executeQuery(selectSql);
            while (results.next())
                {
                    AbstractMap<String,String> rec = new LinkedHashMap<>();
                    rec.put("status",results.getString("status"));
                    rec.put("event",results.getString("event"));
                    rec.put("transactionId",transaction);
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
