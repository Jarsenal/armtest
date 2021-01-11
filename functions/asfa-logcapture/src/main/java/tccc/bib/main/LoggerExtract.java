package tccc.bib.main;

import java.util.*;
import com.microsoft.azure.functions.annotation.*;
import com.microsoft.azure.functions.*;
import com.google.gson.Gson;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.DriverManager;




/**
 * Azure Functions with HTTP Trigger.
 */
public class LoggerExtract {

    @FunctionName("Logger-Extract")
    public HttpResponseMessage run(@HttpTrigger(name = "req", methods = {
            HttpMethod.GET }, authLevel = AuthorizationLevel.FUNCTION) HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {
        context.getLogger().info("Java HTTP trigger processed a request.");

        AbstractList<AbstractMap<String,String>> output = new LinkedList<>();
        
        Integer timespan = Integer.parseInt("-" + Optional.ofNullable(request.getQueryParameters().get("span")).orElse("24"));
        String start = Optional.ofNullable(request.getQueryParameters().get("start")).orElse("2100-12-30 12:00:00.000");
        String event = Optional.ofNullable(request.getQueryParameters().get("event")).orElse("");
        String onlyfail = Optional.ofNullable(request.getQueryParameters().get("onlyfail")).orElse("no");
        String onlyIncomplete  = Optional.ofNullable(request.getQueryParameters().get("incomplete")).orElse("no");
        Integer recordlimit = Integer.parseInt(Optional.ofNullable(request.getQueryParameters().get("recordlimit")).orElse("50"));
        

        String hostName = System.getenv("SQL_HOST");
        String dbName = System.getenv("SQL_DB");
        String user = System.getenv("SQL_USER");
        String password = System.getenv("SQL_PWD");
        
        String url = String.format("jdbc:sqlserver://%s:1433;database=%s;user=%s;password=%s;encrypt=true;"
            + "hostNameInCertificate=*.database.windows.net;loginTimeout=30;", hostName, dbName, user, password);
            
            
        //Connect to database
        try (final Connection connection = DriverManager.getConnection(url)){

            CallableStatement proc = null;
            if(onlyfail.equals("yes")){
                proc = connection.prepareCall("{ call dbo.TRANSACTIONS_FAIL_LIST(?,?,?,?) }");
            }
            else if(onlyIncomplete.equals("yes")){
                proc = connection.prepareCall("{ call dbo.TRANSACTIONS_INCOMPLETE_LIST(?,?,?,?) }");
            }
            else{
                proc = connection.prepareCall("{ call dbo.TRANSACTIONS_LIST(?,?,?,?) }");
            }

            proc.setInt(1, timespan);
            proc.setString(2, start);
            proc.setString(3, event);
            proc.setInt(4, recordlimit);

            ResultSet results = proc.executeQuery();

            while (results.next())
                {
                    AbstractMap<String,String> rec = new LinkedHashMap<>();
                    rec.put("transactionId",results.getString("transactionId"));
                    rec.put("status",results.getString("status"));
                    rec.put("event",results.getString("event"));
                    rec.put("action",results.getString("action"));
                    rec.put("blobKey",results.getString("blobKey"));
                    rec.put("attributes",results.getString("attributes"));
                    rec.put("insertDateTime",results.getString("insertDateTime"));
                    rec.put("runid",results.getString("runid"));
                    rec.put("workflow",results.getString("workflow"));
                    output.add(rec);
                }

            context.getLogger().info("Succefully made the call");
        }
        catch (Exception e) {
            e.printStackTrace();
            context.getLogger().info(e.getMessage());

            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                .header("Content-Type","plain/text")
                .body(String.format("There was an error when extracting transactions (%s), please try again.",e.getMessage()))
                .build();
        }
        


        return request.createResponseBuilder(HttpStatus.OK)
                .header("Content-Type","application/json")
                .body(new Gson().toJson(output))
                .build();
    }

 
    
}
