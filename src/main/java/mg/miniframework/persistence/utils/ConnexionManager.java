package mg.miniframework.persistence.utils;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Logger;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class ConnexionManager {
    private static final Logger logger = Logger.getLogger(ConnexionManager.class.getName());
    
    private static ConnexionManager instance;
    private HikariDataSource dataSource;

    private ConnexionManager(String url, String username, String password) {
        try {
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(url);
            config.setUsername(username);
            config.setPassword(password);
            
            config.setMaximumPoolSize(20);
            config.setMinimumIdle(5);
            config.setConnectionTimeout(30000); 
            config.setIdleTimeout(600000); 
            config.setMaxLifetime(1800000); 
            config.setAutoCommit(true); 
            config.setPoolName("GestionProductionPool");
            
            this.dataSource = new HikariDataSource(config);
            logger.info("Pool HikariCP initialisé - Max: 20, Min: 5, autoCommit=true");
            
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la création du pool HikariCP", e);
        }
    }

    public static ConnexionManager getInstance(String url, String username, String password) {
        if (instance == null) {
            synchronized (ConnexionManager.class) {
                if (instance == null) {
                    instance = new ConnexionManager(url, username, password);
                }
            }
        }
        return instance;
    }

    public static ConnexionManager getInstance() {
        if (instance == null) {
            throw new RuntimeException("ConnexionManager non initialisé. Appelez getInstance(url, username, password) d'abord.");
        }
        return instance;
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public boolean releaseConnection(Connection connection) {
        if (connection == null) {
            return false;
        }
        
        try {
            if (!connection.getAutoCommit()) {
                connection.rollback();
                logger.fine("Transaction rollback effectué");
            }
            connection.close();
            return true;
        } catch (SQLException e) {
            logger.warning("Erreur lors du relâchement de la connexion: " + e.getMessage());
            return false;
        }
    }

    public void shutdown() throws SQLException {
        logger.info("Arrêt du pool HikariCP");
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    public int getAvailableConnectionsCount() {
        return dataSource.getHikariPoolMXBean().getIdleConnections();
    }

    public int getUsedConnectionsCount() {
        return dataSource.getHikariPoolMXBean().getActiveConnections();
    }

    public int getTotalConnections() {
        return dataSource.getHikariPoolMXBean().getTotalConnections();
    }
}
