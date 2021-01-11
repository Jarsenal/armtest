package tccc.bib.main;

import java.util.*;

import com.microsoft.azure.functions.annotation.*;

import tccc.bib.tools.WriteObject;

import com.microsoft.azure.functions.*;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.sql.Connection;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.sql.DriverManager;


public class LogIngest {

    @FunctionName("Logger-Ingest")
    public void run(
        @EventHubTrigger(
            name = "msg",
            eventHubName = "evh-logs",
            connection = "EventHub") String payload,
            final ExecutionContext context) {
        
        context.getLogger().info("Received new event logs");
        context.getLogger().info(payload);

        StringBuilder query = new StringBuilder();
        query.append("INSERT INTO [dbo].[Transactions]\n");
        query.append("(transactionId,status,action,blobKey,attributes,event,insertDateTime,runid,workflow,eventMessage,actionCount,reprocessCount)\n");
        query.append("VALUES\n");

        Gson gson = new Gson();
        LinkedList<String> lines = new LinkedList<>();
        
        AbstractList<?> records = gson.fromJson(payload, new TypeToken<LinkedList<LinkedHashMap<String, Object>>>() {}.getType());


        for(AbstractMap<String,Object> record : (AbstractList<AbstractMap<String,Object>>)records){
            
            if(getValue(record,"status").equals("Failed")){
                context.getLogger().info("We found an error!");
                sendAlert(record, context);  // see if you can make this run on another thread
            }
            
            String result = createInsertRow(record);
            if(result.isEmpty()) continue;
                lines.add(result);
        }

        // if no lines, let's not insert anything
        if(lines.size()==0)return;
        query.append(String.join(",\n",lines));
        query.append(";");

        // Create and execute a SELECT SQL statement.
        String selectSql = query.toString();
        context.getLogger().info(String.format("Insert Query: %s",selectSql));
        

        // sql
        //Connect to database
        boolean successful = false;
        int count = 0;

        String hostName = System.getenv("SQL_HOST");
        String dbName = System.getenv("SQL_DB");
        String user = System.getenv("SQL_USER");
        String password = System.getenv("SQL_PWD");
        
        String url = String.format("jdbc:sqlserver://%s:1433;database=%s;user=%s;password=%s;encrypt=true;"
            + "hostNameInCertificate=*.database.windows.net;loginTimeout=30;", hostName, dbName, user, password);
        

        while(!successful && count < 5){
        
            context.getLogger().info(String.format("Inserting into database, try count: %d", count + 1));
                
            count++;
            successful = true;

            try (Connection connection = DriverManager.getConnection(url)){
                Statement statement = connection.createStatement();
                statement.execute(selectSql);
                context.getLogger().info("Successfully made the call");
            }
            catch (Exception e) {
                e.printStackTrace();
                context.getLogger().info("Failed to make the request.");
                context.getLogger().info(e.getMessage());
                successful = false;
            }
        }

        if(!successful){
            context.getLogger().info("Failed to submit to the sql database");

            SimpleDateFormat formatdate = new SimpleDateFormat("yyyyMMdd");
            SimpleDateFormat formattime = new SimpleDateFormat("HHmmssSSS");
            Date date = new Date();
            String datetimedate = formatdate.format(date);
            String datetimetime = formattime.format(date);
            
            // will write query to blob storage so it can be manually entered
            AbstractMap<String, String> map = new LinkedHashMap<>();
            map.put("datetime",date.toString());
            map.put("message","Failed to submit to the sql database");
            map.put("query",selectSql);

            try{
                WriteObject.write(map, String.format("functionapp/logging/%s/%s-%s", datetimedate, datetimetime, UUID.randomUUID()), "failures");
            }
            catch (Exception e){
                e.printStackTrace();
                context.getLogger().info(e.getMessage());

            }
        }
    
        context.getLogger().info("Function End");
    }

    private void sendAlert(AbstractMap<String,Object> record, final ExecutionContext context){

        // get events
        //AbstractList<?> events = GetConfig.pull("events");
        String sourceevent = getValue(record,"data.event");
        String sourcetransaction = getValue(record,"data.transaction_id");

        // create json payload
        try{
            WriteObject.write(record, String.format("errors/%s/%s",sourceevent, sourcetransaction));
        }
        catch(Exception e){
            e.printStackTrace();
            context.getLogger().info("Failed to write out the error.");
            context.getLogger().info(e.getMessage());
        }

        return;
    }

    private String createInsertRow(AbstractMap<String,Object> record){
        
        if(getValue(record,"data.action").isEmpty()) return"";

        String result = String.format("('%s','%s', '%s', '%s', '%s', '%s', convert(datetime, '%s', 126), '%s','%s','%s',%s,%s)",
                  getValue(record,"transaction_id"),
                  getValue(record,"status"), 
                  getValue(record,"action"), 
                  getValue(record,"payload"),
                  getValue(record,"attributes"),
                  getValue(record,"event"), 
                  getValue(record,"eventTime").replaceAll("Z",""),
                  getValue(record,"runId"),
                  getValue(record,"workflowName"),
                  getValue(record,"message"),
                  getValue(record,"actionCount"),
                  getValue(record,"reprocessCount")); 

        return result;
    }

   private String getValue(AbstractMap<String,Object> record, String... chain){
       String result = null;
       int count = 0;
       AbstractMap<String,Object> pointer = record;

       while(Objects.isNull(result) && chain.length > count){
            
            for(String item : chain[count++].split("\\.")){
                if(pointer.get(item) instanceof AbstractMap){
                    pointer = (AbstractMap)pointer.get(item);
                }
                else if(pointer.get(item) instanceof Double){
                    result = Double.toString((Double)pointer.get(item));
                }
                else {
                    result = (String)pointer.get(item);
                }
            }
            
            pointer = record;
        }

       return Objects.isNull(result)?"":result;
   }
}
