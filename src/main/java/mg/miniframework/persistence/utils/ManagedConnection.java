package mg.miniframework.persistence.utils;

import java.sql.Connection;

/**
 * Wrapper pour une connexion managée qui implémente AutoCloseable
 * Permet l'utilisation avec try-with-resources
 */
public class ManagedConnection implements AutoCloseable {
    private Connection connection;
    private ConnexionManager manager;
    private boolean closed = false;

    public ManagedConnection(Connection connection, ConnexionManager manager) {
        this.connection = connection;
        this.manager = manager;
    }

    public Connection getConnection() {
        return connection;
    }

    @Override
    public void close() {
        if (!closed && connection != null && manager != null) {
            manager.releaseConnection(connection);
            closed = true;
        }
    }

    public boolean isClosed() {
        return closed;
    }
}
