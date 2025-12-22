package com.fifaworldcup.database;

import java.sql.*;

public class DatabaseManager {
    private static DatabaseManager instance;
    private Connection connection;
    private static final String DB_URL = "jdbc:sqlite:fifa_worldcup.db";

    private DatabaseManager() {
    }

    public static DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    public Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection(DB_URL);
            initializeTables();
        }
        return connection;
    }

    private void initializeTables() {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS teams (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name TEXT NOT NULL UNIQUE, " +
                "code TEXT NOT NULL UNIQUE, " +
                "group_name TEXT, " +
                "flag_path TEXT, " +
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
                "home_team_id INTEGER, " +
                "away_team_id INTEGER, " +
                "home_score INTEGER DEFAULT 0, " +
                "away_score INTEGER DEFAULT 0, " +
                "stage TEXT, " +
                "group_name TEXT, " +
                "match_date TEXT, " +
                "match_number INTEGER, " +
                "completed INTEGER DEFAULT 0, " +
                "FOREIGN KEY(home_team_id) REFERENCES teams(id), " +
                "FOREIGN KEY(away_team_id) REFERENCES teams(id))"
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

        } catch (SQLException e) {
            System.err.println("Error initializing database tables: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            System.err.println("Error closing database connection: " + e.getMessage());
        }
    }
}
