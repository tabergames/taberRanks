package taber.ranks;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Created by Sven on 28/08/2014. All self coded.
 */
public class Database {

    private static CadiaRanks instance = CadiaRanks.getInstance();
    private static Connection conn;

    public static void connect() {
        try {
            if ((conn != null) && !conn.isClosed()) {
                return;
            }
            String ip = instance.getConfig().getString("database.ip");
            String port = instance.getConfig().getString("database.port");
            String name = instance.getConfig().getString("database.name");
            String username = instance.getConfig().getString(
                    "database.username");
            String password = instance.getConfig().getString(
                    "database.password");
            conn = DriverManager.getConnection("jdbc:mysql://" + ip + ":"
                            + port + "/" + name + "?autoReconnect=true", username,
                    password
            );
            instance.getLogger().info("Connected to the database.");
        } catch (Exception e) {
            instance.getLogger().info("DATABASE FAILED: " + e.getMessage());
            e.printStackTrace();
            instance.getPluginLoader().disablePlugin(instance);
        }
    }

    public static void disconnect() {
        try {
            conn.close();
        } catch (SQLException e) {
            instance.getLogger().info("DATABASE FAILED: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static Connection getConnection() {
        return conn;
    }

}
