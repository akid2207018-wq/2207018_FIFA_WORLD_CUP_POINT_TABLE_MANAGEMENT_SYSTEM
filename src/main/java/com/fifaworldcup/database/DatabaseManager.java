package com.fifaworldcup.database;

import java.sql.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class DatabaseManager {
    private static DatabaseManager instance;
    private static final String DB_URL = "jdbc:sqlite:fifa_worldcup.db";
    private static final int POOL_SIZE = 5;
    private BlockingQueue<Connection> connectionPool;
    private boolean initialized = false;

    private DatabaseManager() {
        connectionPool = new ArrayBlockingQueue<>(POOL_SIZE);
        initializePool();
    }

    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    private void initializePool() {
        try {
            for (int i = 0; i < POOL_SIZE; i++) {
                Connection conn = DriverManager.getConnection(DB_URL);
                conn.setAutoCommit(true);
                connectionPool.offer(conn);
            }
            // Initialize tables using one connection
            Connection conn = getConnection();
            try {
                initializeTables(conn);
                initialized = true;
            } finally {
                releaseConnection(conn);
            }
        } catch (SQLException e) {
            System.err.println("Error initializing connection pool: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public Connection getConnection() throws SQLException {
        try {
            Connection conn = connectionPool.take();
            if (conn == null || conn.isClosed()) {
                conn = DriverManager.getConnection(DB_URL);
            }
            return conn;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SQLException("Interrupted while waiting for connection", e);
        }
    }

    public void releaseConnection(Connection conn) {
        if (conn != null) {
            try {
                if (!conn.isClosed()) {
                    connectionPool.offer(conn);
                }
            } catch (SQLException e) {
                System.err.println("Error releasing connection: " + e.getMessage());
            }
        }
    }

    private void initializeTables(Connection connection) {
        Statement stmt = null;
        try {
            stmt = connection.createStatement();
            
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS teams (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name TEXT NOT NULL UNIQUE, " +
                "code TEXT NOT NULL UNIQUE, " +
                "group_name TEXT, " +
                "played INTEGER DEFAULT 0, " +
                "won INTEGER DEFAULT 0, " +
                "drawn INTEGER DEFAULT 0, " +
                "lost INTEGER DEFAULT 0, " +
                "goals_for INTEGER DEFAULT 0, " +
                "goals_against INTEGER DEFAULT 0, " +
                "goal_difference INTEGER DEFAULT 0, " +
                "points INTEGER DEFAULT 0, " +
                "qualified INTEGER DEFAULT 0)"
            );

            stmt.execute(
                "CREATE TABLE IF NOT EXISTS matches (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "team1_id INTEGER, " +
                "team2_id INTEGER, " +
                "team1_score INTEGER DEFAULT 0, " +
                "team2_score INTEGER DEFAULT 0, " +
                "stage TEXT, " +
                "group_name TEXT, " +
                "match_date TEXT, " +
                "match_number INTEGER, " +
                "completed INTEGER DEFAULT 0, " +
                "FOREIGN KEY(team1_id) REFERENCES teams(id), " +
                "FOREIGN KEY(team2_id) REFERENCES teams(id))"
            );

            stmt.execute(
                "CREATE TABLE IF NOT EXISTS players (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name TEXT NOT NULL, " +
                "team_id INTEGER, " +
                "jersey_number INTEGER, " +
                "position TEXT, " +
                "goals INTEGER DEFAULT 0, " +
                "assists INTEGER DEFAULT 0, " +
                "yellow_cards INTEGER DEFAULT 0, " +
                "red_cards INTEGER DEFAULT 0, " +
                "matches_played INTEGER DEFAULT 0, " +
                "FOREIGN KEY(team_id) REFERENCES teams(id))"
            );

            stmt.execute(
                "CREATE TABLE IF NOT EXISTS knockout_matches (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "match_number INTEGER, " +
                "round TEXT, " +
                "team1_id INTEGER, " +
                "team2_id INTEGER, " +
                "team1_score INTEGER DEFAULT 0, " +
                "team2_score INTEGER DEFAULT 0, " +
                "winner_id INTEGER, " +
                "bracket_position TEXT, " +
                "completed INTEGER DEFAULT 0, " +
                "FOREIGN KEY(team1_id) REFERENCES teams(id), " +
                "FOREIGN KEY(team2_id) REFERENCES teams(id), " +
                "FOREIGN KEY(winner_id) REFERENCES teams(id))"
            );

            // Create indexes for better performance
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_teams_group ON teams(group_name)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_matches_group ON matches(group_name)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_matches_stage ON matches(stage)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_players_team ON players(team_id)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_knockout_round ON knockout_matches(round)");

        } catch (SQLException e) {
            System.err.println("Error initializing database tables: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException e) {
                    // Ignore close error
                }
            }
        }
    }

    public void closeAllConnections() {
        for (Connection conn : connectionPool) {
            try {
                if (conn != null && !conn.isClosed()) {
                    conn.close();
                }
            } catch (SQLException e) {
                System.err.println("Error closing connection: " + e.getMessage());
            }
        }
        connectionPool.clear();
    }

    public boolean isInitialized() {
        return initialized;
    }
}
