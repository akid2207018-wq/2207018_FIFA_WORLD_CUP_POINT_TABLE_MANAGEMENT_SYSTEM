package com.fifaworldcup.service;

import com.fifaworldcup.database.DatabaseManager;
import com.fifaworldcup.model.Player;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PlayerService {
    private DatabaseManager dbManager;
    private Gson gson;

    public PlayerService() {
        this.dbManager = DatabaseManager.getInstance();
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    // Validation
    public String validatePlayer(Player player) {
        if (player.getName() == null || player.getName().trim().isEmpty()) {
            return "Player name is required";
        }
        if (player.getName().length() < 2 || player.getName().length() > 100) {
            return "Player name must be between 2 and 100 characters";
        }
        if (player.getJerseyNumber() < 1 || player.getJerseyNumber() > 99) {
            return "Jersey number must be between 1 and 99";
        }
        if (player.getTeamId() <= 0) {
            return "Team selection is required";
        }
        if (player.getPosition() == null || player.getPosition().trim().isEmpty()) {
            return "Position is required";
        }
        return null; // Valid
    }

    public void addPlayer(Player player) throws SQLException {
        String validation = validatePlayer(player);
        if (validation != null) {
            throw new SQLException(validation);
        }

        String sql = "INSERT INTO players (name, team_id, jersey_number, position, goals, assists, " +
                     "yellow_cards, red_cards, matches_played) VALUES (?, ?, ?, ?, ?, ?, 0, 0, 0)";
        
        Connection conn = dbManager.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, player.getName().trim());
            pstmt.setInt(2, player.getTeamId());
            pstmt.setInt(3, player.getJerseyNumber());
            pstmt.setString(4, player.getPosition());
            pstmt.setInt(5, player.getGoals());
            pstmt.setInt(6, player.getAssists());
            pstmt.executeUpdate();
            
            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                player.setId(rs.getInt(1));
            }
        } finally {
            dbManager.releaseConnection(conn);
        }
    }

    public void updatePlayer(Player player) throws SQLException {
        String validation = validatePlayer(player);
        if (validation != null) {
            throw new SQLException(validation);
        }

        String sql = "UPDATE players SET name = ?, team_id = ?, jersey_number = ?, position = ?, " +
                     "goals = ?, assists = ?, yellow_cards = ?, red_cards = ?, matches_played = ? WHERE id = ?";
        
        Connection conn = dbManager.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, player.getName().trim());
            pstmt.setInt(2, player.getTeamId());
            pstmt.setInt(3, player.getJerseyNumber());
            pstmt.setString(4, player.getPosition());
            pstmt.setInt(5, player.getGoals());
            pstmt.setInt(6, player.getAssists());
            pstmt.setInt(7, player.getYellowCards());
            pstmt.setInt(8, player.getRedCards());
            pstmt.setInt(9, player.getMatchesPlayed());
            pstmt.setInt(10, player.getId());
            pstmt.executeUpdate();
        } finally {
            dbManager.releaseConnection(conn);
        }
    }

    public void deletePlayer(int playerId) throws SQLException {
        String sql = "DELETE FROM players WHERE id = ?";
        
        Connection conn = dbManager.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, playerId);
            pstmt.executeUpdate();
        } finally {
            dbManager.releaseConnection(conn);
        }
    }

    public Player getPlayerById(int id) throws SQLException {
        String sql = "SELECT p.*, t.name as team_name FROM players p " +
                     "LEFT JOIN teams t ON p.team_id = t.id WHERE p.id = ?";
        
        Connection conn = dbManager.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return mapResultSetToPlayer(rs);
            }
        } finally {
            dbManager.releaseConnection(conn);
        }
        return null;
    }

    public List<Player> getAllPlayers() throws SQLException {
        List<Player> players = new ArrayList<>();
        String sql = "SELECT p.*, t.name as team_name FROM players p " +
                     "LEFT JOIN teams t ON p.team_id = t.id ORDER BY p.name";
        
        Connection conn = dbManager.getConnection();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                players.add(mapResultSetToPlayer(rs));
            }
        } finally {
            dbManager.releaseConnection(conn);
        }
        return players;
    }

    public List<Player> getPlayersByTeam(int teamId) throws SQLException {
        List<Player> players = new ArrayList<>();
        String sql = "SELECT p.*, t.name as team_name FROM players p " +
                     "LEFT JOIN teams t ON p.team_id = t.id WHERE p.team_id = ? ORDER BY p.jersey_number";
        
        Connection conn = dbManager.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, teamId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                players.add(mapResultSetToPlayer(rs));
            }
        } finally {
            dbManager.releaseConnection(conn);
        }
        return players;
    }

    public List<Player> getTopScorers(int limit) throws SQLException {
        List<Player> players = new ArrayList<>();
        String sql = "SELECT p.*, t.name as team_name FROM players p " +
                     "LEFT JOIN teams t ON p.team_id = t.id " +
                     "WHERE p.goals > 0 " +
                     "ORDER BY p.goals DESC, p.assists DESC LIMIT ?";
        
        Connection conn = dbManager.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, limit);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                players.add(mapResultSetToPlayer(rs));
            }
        } finally {
            dbManager.releaseConnection(conn);
        }
        return players;
    }

    public List<Player> getTopAssists(int limit) throws SQLException {
        List<Player> players = new ArrayList<>();
        String sql = "SELECT p.*, t.name as team_name FROM players p " +
                     "LEFT JOIN teams t ON p.team_id = t.id " +
                     "WHERE p.assists > 0 " +
                     "ORDER BY p.assists DESC, p.goals DESC LIMIT ?";
        
        Connection conn = dbManager.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, limit);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                players.add(mapResultSetToPlayer(rs));
            }
        } finally {
            dbManager.releaseConnection(conn);
        }
        return players;
    }

    public void updatePlayerStats(int playerId, int goals, int assists) throws SQLException {
        if (goals < 0 || assists < 0) {
            throw new SQLException("Goals and assists cannot be negative");
        }

        String sql = "UPDATE players SET goals = ?, assists = ? WHERE id = ?";
        
        Connection conn = dbManager.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, goals);
            pstmt.setInt(2, assists);
            pstmt.setInt(3, playerId);
            pstmt.executeUpdate();
        } finally {
            dbManager.releaseConnection(conn);
        }
    }

    public void incrementPlayerGoals(int playerId) throws SQLException {
        String sql = "UPDATE players SET goals = goals + 1 WHERE id = ?";
        
        Connection conn = dbManager.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, playerId);
            pstmt.executeUpdate();
        } finally {
            dbManager.releaseConnection(conn);
        }
    }

    public void incrementPlayerAssists(int playerId) throws SQLException {
        String sql = "UPDATE players SET assists = assists + 1 WHERE id = ?";
        
        Connection conn = dbManager.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, playerId);
            pstmt.executeUpdate();
        } finally {
            dbManager.releaseConnection(conn);
        }
    }

    public void incrementMatchesPlayed(int playerId) throws SQLException {
        String sql = "UPDATE players SET matches_played = matches_played + 1 WHERE id = ?";
        
        Connection conn = dbManager.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, playerId);
            pstmt.executeUpdate();
        } finally {
            dbManager.releaseConnection(conn);
        }
    }

    public List<Player> searchPlayers(String query) throws SQLException {
        List<Player> players = new ArrayList<>();
        String sql = "SELECT p.*, t.name as team_name FROM players p " +
                     "LEFT JOIN teams t ON p.team_id = t.id " +
                     "WHERE LOWER(p.name) LIKE ? OR LOWER(t.name) LIKE ? " +
                     "ORDER BY p.name";
        
        Connection conn = dbManager.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            String searchPattern = "%" + query.toLowerCase() + "%";
            pstmt.setString(1, searchPattern);
            pstmt.setString(2, searchPattern);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                players.add(mapResultSetToPlayer(rs));
            }
        } finally {
            dbManager.releaseConnection(conn);
        }
        return players;
    }

    public void exportPlayersToJson(String filePath) throws IOException, SQLException {
        List<Player> players = getAllPlayers();
        try (Writer writer = new FileWriter(filePath)) {
            gson.toJson(players, writer);
        }
    }

    public void importPlayersFromJson(String filePath) throws IOException, SQLException {
        try (Reader reader = new FileReader(filePath)) {
            Type playerListType = new TypeToken<ArrayList<Player>>(){}.getType();
            List<Player> players = gson.fromJson(reader, playerListType);
            
            for (Player player : players) {
                addPlayer(player);
            }
        }
    }

    // Player statistics aggregation
    public int getTotalGoalsByTeam(int teamId) throws SQLException {
        String sql = "SELECT SUM(goals) FROM players WHERE team_id = ?";
        
        Connection conn = dbManager.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, teamId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } finally {
            dbManager.releaseConnection(conn);
        }
        return 0;
    }

    public int getTotalAssistsByTeam(int teamId) throws SQLException {
        String sql = "SELECT SUM(assists) FROM players WHERE team_id = ?";
        
        Connection conn = dbManager.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, teamId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } finally {
            dbManager.releaseConnection(conn);
        }
        return 0;
    }

    private Player mapResultSetToPlayer(ResultSet rs) throws SQLException {
        Player player = new Player();
        player.setId(rs.getInt("id"));
        player.setName(rs.getString("name"));
        player.setTeamId(rs.getInt("team_id"));
        player.setTeamName(rs.getString("team_name"));
        player.setJerseyNumber(rs.getInt("jersey_number"));
        player.setPosition(rs.getString("position"));
        player.setGoals(rs.getInt("goals"));
        player.setAssists(rs.getInt("assists"));
        player.setYellowCards(rs.getInt("yellow_cards"));
        player.setRedCards(rs.getInt("red_cards"));
        player.setMatchesPlayed(rs.getInt("matches_played"));
        return player;
    }
}
