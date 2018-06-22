package com.avairebot.contracts.database.connections;

import com.avairebot.contracts.database.Database;
import com.avairebot.database.exceptions.DatabaseException;

public abstract class HostnameDatabase extends Database {

    /**
     * The database hostname that should be
     * used to connect to the database.
     */
    private String hostname;

    /**
     * The port that the database is listing on.
     */
    private int port;

    /**
     * The name of the database that should be used.
     */
    private String database;

    /**
     * The login username used to connect to the database.
     */
    private String username;

    /**
     * The login password used to connect to the database.
     */
    private String password;

    /**
     * Creates a new host name database instance.
     *
     * @param hostname The host name to connect to.
     * @param port     The port the database is listing on.
     * @param database The database name to use.
     * @param username The login username.
     * @param password The login password.
     */
    public HostnameDatabase(String hostname, int port, String database, String username, String password) {
        this.hostname = hostname;
        this.port = port;
        this.database = database;
        this.username = username;
        this.password = password;
    }

    /**
     * Returns the host name the database is listing on.
     *
     * @return the database host name
     */
    public String getHostname() {
        return hostname;
    }

    /**
     * Sets the host name the database is listing on.
     *
     * @param hostname the database host name
     */
    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    /**
     * Returns the port the database is listing on.
     *
     * @return the database port
     */
    public int getPort() {
        return port;
    }

    /**
     * Sets the port the database should be listing on,
     * the port has to be between 0 and 65535.
     *
     * @param port the database port
     */
    public void setPort(int port) {
        if ((port < 0) || (65535 < port)) {
            throw new DatabaseException("Port number cannot be below 0 or greater than 65535.");
        }

        this.port = port;
    }

    /**
     * Gets the name of the database that should be used.
     *
     * @return the database name
     */
    public String getDatabase() {
        return database;
    }

    /**
     * Sets the database name that should be used.
     *
     * @param database the database name
     */
    public void setDatabase(String database) {
        this.database = database;
    }

    /**
     * Returns the database username login.
     *
     * @return the database username
     */
    public String getUsername() {
        return username;
    }

    /**
     * Sets the database username login.
     *
     * @param username the database username
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * Returns the database password login.
     *
     * @return the database password
     */
    public String getPassword() {
        return password;
    }

    /**
     * Sets the database password login.
     *
     * @param password the database password
     */
    public void setPassword(String password) {
        this.password = password;
    }
}
