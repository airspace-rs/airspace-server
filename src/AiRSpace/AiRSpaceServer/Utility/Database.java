package AiRSpace.AiRSpaceServer.Utility;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Database
{
    // Use a static to store the connection
    private static Connection connection = null;

    public static Connection getInstance()
    {
        // If the connection hasn't been set, let's set it
        if (connection == null) {
            try {
                // Try to establish a connection using the settings in the settings file
                Class.forName("com.mysql.jdbc.Driver");
                connection = DriverManager.getConnection(
                        "jdbc:mysql://" + Setting.get("DBHostname") + ":" + Setting.get("DBPort") + "/" + Setting.get("DBDatabase"),
                        Setting.get("DBUsername"),
                        Setting.get("DBPassword")
                );
                // Set auto commit
                connection.setAutoCommit(true);
                // Set read-only
                connection.setReadOnly(false);
            } catch (ClassNotFoundException cnfe) {
                // The driver is included in this bundle, we shouldn't see this
                System.out.println("Can't find your MySQL driver.");
                System.exit(1);
            } catch (SQLException sqle) {
                // Any other MySQL exception will result in server termination.
                // TODO We should try to find a way to handle this gracefully
                System.out.println(sqle.getMessage());
                System.out.println(sqle.getSQLState());
                System.out.println(sqle.getErrorCode());
                System.exit(1); // Die!
            }
        }

        // Return the connection
        return connection;
    }
}
