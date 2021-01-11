package tccc.bib.main;

import java.util.*;
import com.microsoft.azure.functions.annotation.*;
import com.microsoft.azure.functions.*;
import com.google.gson.Gson;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.DriverManager;
import java.sql.PreparedStatement;



/**
 * Azure Functions with HTTP Trigger.
 */
public class LoggerEventSummary {

    @FunctionName("Event-Count-Extract")
    public HttpResponseMessage run(@HttpTrigger(name = "req", methods = {
            HttpMethod.GET }, authLevel = AuthorizationLevel.FUNCTION) HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {
        context.getLogger().info("Java HTTP trigger processed a request.");

        AbstractList<AbstractMap<String,String>> output = new LinkedList<>();   
        
        Integer timespan = Integer.parseInt("-" + Optional.ofNullable(request.getQueryParameters().get("span")).orElse("24"));
        String hostName = System.getenv("SQL_HOST");
        String dbName = System.getenv("SQL_DB");
        String user = System.getenv("SQL_USER");
        String password = System.getenv("SQL_PWD");
        
        String url = String.format("jdbc:sqlserver://%s:1433;database=%s;user=%s;password=%s;encrypt=true;"
            + "hostNameInCertificate=*.database.windows.net;loginTimeout=30;", hostName, dbName, user, password);
        
        //Connect to database
        try(final Connection connection = DriverManager.getConnection(url)) {
            
            CallableStatement proc = connection.prepareCall("{ call dbo.TRANSACTIONS_SUMMARY(?) }");
            proc.setInt(1, timespan.intValue());
            ResultSet results = proc.executeQuery();

            while (results.next())
                {
                    AbstractMap<String,String> rec = new LinkedHashMap<>();
                    rec.put("event",results.getString("event"));
                    rec.put("total",results.getString("total"));
                    rec.put("fails",results.getString("fails"));
                    rec.put("incomplete",results.getString("incomplete"));
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
