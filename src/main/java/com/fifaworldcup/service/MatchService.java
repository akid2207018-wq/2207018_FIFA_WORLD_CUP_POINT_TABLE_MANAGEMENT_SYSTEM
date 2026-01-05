package com.fifaworldcup.service;

import com.fifaworldcup.database.DatabaseManager;
import com.fifaworldcup.model.Match;
import com.fifaworldcup.model.Team;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class MatchService {
    private DatabaseManager dbManager;
    private Gson gson;
    private TeamService teamService;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public MatchService() {
        this.dbManager = DatabaseManager.getInstance();
        this.teamService = new TeamService();
        
        // Build Gson with LocalDateTime support
        GsonBuilder builder = new GsonBuilder();
        builder.setPrettyPrinting();
        builder.registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter());
        this.gson = builder.create();
    }
    
    // Combined adapter for LocalDateTime serialization/deserialization
    private static class LocalDateTimeAdapter implements JsonSerializer<LocalDateTime>, JsonDeserializer<LocalDateTime> {
        @Override
        public JsonElement serialize(LocalDateTime src, Type typeOfSrc, JsonSerializationContext context) {
            if (src == null) return JsonNull.INSTANCE;
            return new JsonPrimitive(src.format(DATE_FORMATTER));
        }

        @Override
        public LocalDateTime deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            if (json == null || json.isJsonNull()) return null;
            return LocalDateTime.parse(json.getAsString(), DATE_FORMATTER);
        }
    }

    public void addMatch(Match match) throws SQLException {
        String sql = "INSERT INTO matches (team1_id, team2_id, team1_score, team2_score, stage, group_name, " +
                     "match_date, match_number, completed) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = dbManager.getConnection();
            pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            pstmt.setInt(1, match.getTeam1Id());
            pstmt.setInt(2, match.getTeam2Id());
            pstmt.setInt(3, match.getTeam1Score());
            pstmt.setInt(4, match.getTeam2Score());
            pstmt.setString(5, match.getStage());
            pstmt.setString(6, match.getGroup());
            pstmt.setString(7, match.getMatchDate() != null ? match.getMatchDate().format(DATE_FORMATTER) : null);
            pstmt.setInt(8, match.getMatchNumber());
            pstmt.setInt(9, match.isCompleted() ? 1 : 0);
            pstmt.executeUpdate();
            
            rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                match.setId(rs.getInt(1));
            }
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException e) {}
            if (pstmt != null) try { pstmt.close(); } catch (SQLException e) {}
            if (conn != null) dbManager.releaseConnection(conn);
        }
    }

    public void updateMatchResult(int matchId, int team1Score, int team2Score) throws SQLException {
        // Validate scores
        if (team1Score < 0 || team2Score < 0) {
            throw new SQLException("Scores cannot be negative");
        }
        if (team1Score > 20 || team2Score > 20) {
            throw new SQLException("Scores seem unrealistic (max 20)");
        }

        String sql = "UPDATE matches SET team1_score = ?, team2_score = ?, completed = 1 WHERE id = ?";
        
        Connection conn = null;
        PreparedStatement pstmt = null;
        try {
            conn = dbManager.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, team1Score);
            pstmt.setInt(2, team2Score);
            pstmt.setInt(3, matchId);
            pstmt.executeUpdate();
        } finally {
            if (pstmt != null) try { pstmt.close(); } catch (SQLException e) {}
            if (conn != null) dbManager.releaseConnection(conn);
        }
    }

    public void clearMatchResult(int matchId) throws SQLException {
        String sql = "UPDATE matches SET team1_score = 0, team2_score = 0, completed = 0 WHERE id = ?";
        
        Connection conn = null;
        PreparedStatement pstmt = null;
        try {
            conn = dbManager.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, matchId);
            pstmt.executeUpdate();
        } finally {
            if (pstmt != null) try { pstmt.close(); } catch (SQLException e) {}
            if (conn != null) dbManager.releaseConnection(conn);
        }
    }

    public Match getMatchById(int id) throws SQLException {
        String sql = "SELECT m.*, t1.name as team1_name, t2.name as team2_name " +
                     "FROM matches m " +
                     "LEFT JOIN teams t1 ON m.team1_id = t1.id " +
                     "LEFT JOIN teams t2 ON m.team2_id = t2.id " +
                     "WHERE m.id = ?";
        
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = dbManager.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, id);
            rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return mapResultSetToMatch(rs);
            }
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException e) {}
            if (pstmt != null) try { pstmt.close(); } catch (SQLException e) {}
            if (conn != null) dbManager.releaseConnection(conn);
        }
        return null;
    }

    public List<Match> getAllMatches() throws SQLException {
        List<Match> matches = new ArrayList<>();
        String sql = "SELECT m.*, t1.name as team1_name, t2.name as team2_name " +
                     "FROM matches m " +
                     "LEFT JOIN teams t1 ON m.team1_id = t1.id " +
                     "LEFT JOIN teams t2 ON m.team2_id = t2.id " +
                     "ORDER BY m.match_number";
        
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        try {
            conn = dbManager.getConnection();
            stmt = conn.createStatement();
            rs = stmt.executeQuery(sql);
            while (rs.next()) {
                matches.add(mapResultSetToMatch(rs));
            }
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException e) {}
            if (stmt != null) try { stmt.close(); } catch (SQLException e) {}
            if (conn != null) dbManager.releaseConnection(conn);
        }
        return matches;
    }

    public List<Match> getMatchesByGroup(String group) throws SQLException {
        List<Match> matches = new ArrayList<>();
        String sql = "SELECT m.*, t1.name as team1_name, t2.name as team2_name " +
                     "FROM matches m " +
                     "LEFT JOIN teams t1 ON m.team1_id = t1.id " +
                     "LEFT JOIN teams t2 ON m.team2_id = t2.id " +
                     "WHERE m.group_name = ? " +
                     "ORDER BY m.match_number";
        
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = dbManager.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, group);
            rs = pstmt.executeQuery();
            while (rs.next()) {
                matches.add(mapResultSetToMatch(rs));
            }
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException e) {}
            if (pstmt != null) try { pstmt.close(); } catch (SQLException e) {}
            if (conn != null) dbManager.releaseConnection(conn);
        }
        return matches;
    }

    public List<Match> getMatchesByStage(String stage) throws SQLException {
        List<Match> matches = new ArrayList<>();
        String sql = "SELECT m.*, t1.name as team1_name, t2.name as team2_name " +
                     "FROM matches m " +
                     "LEFT JOIN teams t1 ON m.team1_id = t1.id " +
                     "LEFT JOIN teams t2 ON m.team2_id = t2.id " +
                     "WHERE m.stage = ? " +
                     "ORDER BY m.match_number";
        
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = dbManager.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, stage);
            rs = pstmt.executeQuery();
            while (rs.next()) {
                matches.add(mapResultSetToMatch(rs));
            }
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException e) {}
            if (pstmt != null) try { pstmt.close(); } catch (SQLException e) {}
            if (conn != null) dbManager.releaseConnection(conn);
        }
        return matches;
    }

    public List<Match> getMatchesByTeam(int teamId) throws SQLException {
        List<Match> matches = new ArrayList<>();
        String sql = "SELECT m.*, t1.name as team1_name, t2.name as team2_name " +
                     "FROM matches m " +
                     "LEFT JOIN teams t1 ON m.team1_id = t1.id " +
                     "LEFT JOIN teams t2 ON m.team2_id = t2.id " +
                     "WHERE m.team1_id = ? OR m.team2_id = ? " +
                     "ORDER BY m.match_number";
        
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = dbManager.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, teamId);
            pstmt.setInt(2, teamId);
            rs = pstmt.executeQuery();
            while (rs.next()) {
                matches.add(mapResultSetToMatch(rs));
            }
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException e) {}
            if (pstmt != null) try { pstmt.close(); } catch (SQLException e) {}
            if (conn != null) dbManager.releaseConnection(conn);
        }
        return matches;
    }

    public List<Match> getCompletedMatches() throws SQLException {
        List<Match> matches = new ArrayList<>();
        String sql = "SELECT m.*, t1.name as team1_name, t2.name as team2_name " +
                     "FROM matches m " +
                     "LEFT JOIN teams t1 ON m.team1_id = t1.id " +
                     "LEFT JOIN teams t2 ON m.team2_id = t2.id " +
                     "WHERE m.completed = 1 " +
                     "ORDER BY m.match_number";
        
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        try {
            conn = dbManager.getConnection();
            stmt = conn.createStatement();
            rs = stmt.executeQuery(sql);
            while (rs.next()) {
                matches.add(mapResultSetToMatch(rs));
            }
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException e) {}
            if (stmt != null) try { stmt.close(); } catch (SQLException e) {}
            if (conn != null) dbManager.releaseConnection(conn);
        }
        return matches;
    }

    public List<Match> getPendingMatches() throws SQLException {
        List<Match> matches = new ArrayList<>();
        String sql = "SELECT m.*, t1.name as team1_name, t2.name as team2_name " +
                     "FROM matches m " +
                     "LEFT JOIN teams t1 ON m.team1_id = t1.id " +
                     "LEFT JOIN teams t2 ON m.team2_id = t2.id " +
                     "WHERE m.completed = 0 " +
                     "ORDER BY m.match_number";
        
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        try {
            conn = dbManager.getConnection();
            stmt = conn.createStatement();
            rs = stmt.executeQuery(sql);
            while (rs.next()) {
                matches.add(mapResultSetToMatch(rs));
            }
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException e) {}
            if (stmt != null) try { stmt.close(); } catch (SQLException e) {}
            if (conn != null) dbManager.releaseConnection(conn);
        }
        return matches;
    }

    // Match scheduling algorithm - generates round-robin matches for each group
    public void generateGroupStageMatches() throws SQLException {
        String[] groups = {"A", "B", "C", "D", "E", "F", "G", "H"};
        int matchNumber = 1;
        
        for (String group : groups) {
            List<Team> teams = teamService.getTeamsByGroup(group);
            
            if (teams.size() < 2) continue;
            
            // Round-robin algorithm: each team plays every other team once
            for (int i = 0; i < teams.size(); i++) {
                for (int j = i + 1; j < teams.size(); j++) {
                    Match match = new Match();
                    match.setTeam1Id(teams.get(i).getId());
                    match.setTeam2Id(teams.get(j).getId());
                    match.setStage("GROUP");
                    match.setGroup(group);
                    match.setMatchNumber(matchNumber++);
                    match.setCompleted(false);
                    match.setMatchDate(null); // No date assigned
                    
                    addMatch(match);
                }
            }
        }
    }

    public void clearAllMatches() throws SQLException {
        Connection conn = null;
        Statement stmt = null;
        
        try {
            conn = dbManager.getConnection();
            stmt = conn.createStatement();
            
            // Delete all matches
            stmt.executeUpdate("DELETE FROM matches");
            
            // Reset team statistics
            stmt.executeUpdate("UPDATE teams SET played = 0, won = 0, drawn = 0, lost = 0, " +
                             "goals_for = 0, goals_against = 0, goal_difference = 0, points = 0, qualified = 0");
        } finally {
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException e) {
                    // Ignore close errors
                }
            }
            if (conn != null) {
                dbManager.releaseConnection(conn);
            }
        }
    }

    public void deleteMatch(int matchId) throws SQLException {
        String sql = "DELETE FROM matches WHERE id = ?";
        
        Connection conn = null;
        PreparedStatement pstmt = null;
        try {
            conn = dbManager.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, matchId);
            pstmt.executeUpdate();
        } finally {
            if (pstmt != null) try { pstmt.close(); } catch (SQLException e) {}
            if (conn != null) dbManager.releaseConnection(conn);
        }
    }

    public void exportMatchesToJson(String filePath) throws IOException, SQLException {
        List<Match> matches = getAllMatches();
        try (Writer writer = new FileWriter(filePath)) {
            gson.toJson(matches, writer);
        }
    }

    public void importMatchesFromJson(String filePath) throws IOException, SQLException {
        try (Reader reader = new FileReader(filePath)) {
            Type matchListType = new TypeToken<ArrayList<Match>>(){}.getType();
            List<Match> matches = gson.fromJson(reader, matchListType);
            
            for (Match match : matches) {
                addMatch(match);
            }
        }
    }

    public int getTotalMatchCount() throws SQLException {
        String sql = "SELECT COUNT(*) FROM matches";
        
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        try {
            conn = dbManager.getConnection();
            stmt = conn.createStatement();
            rs = stmt.executeQuery(sql);
            if (rs.next()) {
                return rs.getInt(1);
            }
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException e) {}
            if (stmt != null) try { stmt.close(); } catch (SQLException e) {}
            if (conn != null) dbManager.releaseConnection(conn);
        }
        return 0;
    }

    public int getCompletedMatchCount() throws SQLException {
        String sql = "SELECT COUNT(*) FROM matches WHERE completed = 1";
        
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        try {
            conn = dbManager.getConnection();
            stmt = conn.createStatement();
            rs = stmt.executeQuery(sql);
            if (rs.next()) {
                return rs.getInt(1);
            }
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException e) {}
            if (stmt != null) try { stmt.close(); } catch (SQLException e) {}
            if (conn != null) dbManager.releaseConnection(conn);
        }
        return 0;
    }

    private Match mapResultSetToMatch(ResultSet rs) throws SQLException {
        Match match = new Match();
        match.setId(rs.getInt("id"));
        match.setTeam1Id(rs.getInt("team1_id"));
        match.setTeam2Id(rs.getInt("team2_id"));
        match.setTeam1Name(rs.getString("team1_name"));
        match.setTeam2Name(rs.getString("team2_name"));
        match.setTeam1Score(rs.getInt("team1_score"));
        match.setTeam2Score(rs.getInt("team2_score"));
        match.setStage(rs.getString("stage"));
        match.setGroup(rs.getString("group_name"));
        match.setMatchNumber(rs.getInt("match_number"));
        match.setCompleted(rs.getInt("completed") == 1);
        
        String dateStr = rs.getString("match_date");
        if (dateStr != null && !dateStr.isEmpty()) {
            try {
                match.setMatchDate(LocalDateTime.parse(dateStr, DATE_FORMATTER));
            } catch (Exception e) {
                // Ignore date parse errors
            }
        }
        
        return match;
    }
}
