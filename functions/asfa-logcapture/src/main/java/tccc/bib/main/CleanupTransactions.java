package tccc.bib.main;

import java.sql.*;
import java.time.*;
import java.util.logging.Level;

import com.microsoft.azure.functions.annotation.*;
import com.microsoft.azure.functions.*;
import com.microsoft.rest.LogLevel;

/**
 * Azure Functions with Timer trigger.
 */
public class CleanupTransactions {
    /**
     * This function will be invoked periodically according to the specified schedule.
     */
    @FunctionName("Cleanup-Transactions")
    public void run(
        @TimerTrigger(name = "timerInfo", schedule = "0 0 17 * * *") String timerInfo,
        final ExecutionContext context
    ) {
        context.getLogger().info("Timer trigger Cleanup-Transactions function triggered at: " + LocalDateTime.now());

        CallableStatement cstmt = null;
        try {
            String hostName = System.getenv("SQL_HOST");
            String dbName = System.getenv("SQL_DB");
            String user = System.getenv("SQL_USER");
            String password = System.getenv("SQL_PWD");
            int past_days = -30;

            String CLEANUPTRANSACTIONS_DAYS = System.getenv("CLEANUPTRANSACTIONS_DAYS");

            if(CLEANUPTRANSACTIONS_DAYS != null){
                context.getLogger().info("Application Setting for CLEANUPTRANSACTIONS_DAYS variable is " + CLEANUPTRANSACTIONS_DAYS + ".");
                past_days = Integer.parseInt(CLEANUPTRANSACTIONS_DAYS);
            }
            else{
                context.getLogger().info("Application Setting for CLEANUPTRANSACTIONS_DAYS variable is not defined. Using default of -30 days.");
            }

            String url = String.format("jdbc:sqlserver://%s:1433;database=%s;user=%s;password=%s;encrypt=true;"
                    + "hostNameInCertificate=*.database.windows.net;loginTimeout=30;", hostName, dbName, user, password);

            String cleanUpStoredProcedure = "{call dbo.TRANSACTIONS_CLEANUP(?)}";

            final Connection connection = DriverManager.getConnection(url);

            cstmt = connection.prepareCall(cleanUpStoredProcedure);

            cstmt.setInt("past_days", past_days);

            cstmt.execute();

            context.getLogger().info("Timer trigger Cleanup-Transactions function finished at: " + LocalDateTime.now());
        }
        catch (Exception e) {
            e.printStackTrace();
            context.getLogger().info(e.getMessage());
        }
        finally {
            try {
                if (cstmt != null) {
                    cstmt.close();
                    context.getLogger().info("Closed Callable Statement.");
                }

            }
            catch(SQLException sqlExp) {
                //context.getLogger().info("No action Needed.");
            }
        }
    }
}
