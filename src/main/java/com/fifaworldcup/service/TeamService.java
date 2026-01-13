package com.fifaworldcup.service;

import com.fifaworldcup.database.DatabaseManager;
import com.fifaworldcup.model.Team;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TeamService {
    private DatabaseManager dbManager;
    private Gson gson;

    public TeamService() {
        this.dbManager = DatabaseManager.getInstance();
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    // Validation methods
    public String validateTeam(Team team) {
        if (team.getName() == null || team.getName().trim().isEmpty()) {
            return "Team name is required";
        }
        if (team.getName().length() < 2 || team.getName().length() > 50) {
            return "Team name must be between 2 and 50 characters";
        }
        if (team.getCode() == null || team.getCode().trim().isEmpty()) {
            return "Team code is required";
        }
        if (!team.getCode().matches("^[A-Z]{3}$")) {
            return "Team code must be exactly 3 uppercase letters";
        }
        return null; // Valid
    }
    
    public boolean isTeamNameExists(String name, Integer excludeId) throws SQLException {
        String sql = excludeId == null ? 
            "SELECT COUNT(*) FROM teams WHERE LOWER(name) = LOWER(?)" :
            "SELECT COUNT(*) FROM teams WHERE LOWER(name) = LOWER(?) AND id != ?";
        
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = dbManager.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, name.trim());
            if (excludeId != null) {
                pstmt.setInt(2, excludeId);
            }
            rs = pstmt.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException e) {}
            if (pstmt != null) try { pstmt.close(); } catch (SQLException e) {}
            if (conn != null) dbManager.releaseConnection(conn);
        }
    }
    
    public boolean isTeamCodeExists(String code, Integer excludeId) throws SQLException {
        String sql = excludeId == null ? 
            "SELECT COUNT(*) FROM teams WHERE UPPER(code) = UPPER(?)" :
            "SELECT COUNT(*) FROM teams WHERE UPPER(code) = UPPER(?) AND id != ?";
        
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = dbManager.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, code.trim());
            if (excludeId != null) {
                pstmt.setInt(2, excludeId);
            }
            rs = pstmt.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException e) {}
            if (pstmt != null) try { pstmt.close(); } catch (SQLException e) {}
            if (conn != null) dbManager.releaseConnection(conn);
        }
    }

    public void addTeam(Team team) throws SQLException {
        String validation = validateTeam(team);
        if (validation != null) {
            throw new SQLException(validation);
        }
        
        // Check for duplicates
        if (isTeamNameExists(team.getName(), null)) {
            throw new SQLException("A team with the name '" + team.getName() + "' already exists.");
        }
        if (isTeamCodeExists(team.getCode(), null)) {
            throw new SQLException("A team with the code '" + team.getCode().toUpperCase() + "' already exists.");
        }

        Connection conn = dbManager.getConnection();
        try {
            // Find the smallest available ID (reuse deleted IDs)
            Integer nextId = findNextAvailableId(conn);
            
            String sql;
            PreparedStatement pstmt;
            
            if (nextId != null) {
                // Insert with specific ID to reuse deleted ID
                sql = "INSERT INTO teams (id, name, code, group_name, played, won, drawn, lost, " +
                      "goals_for, goals_against, goal_difference, points, qualified) " +
                      "VALUES (?, ?, ?, ?, 0, 0, 0, 0, 0, 0, 0, 0, 0)";
                pstmt = conn.prepareStatement(sql);
                pstmt.setInt(1, nextId);
                pstmt.setString(2, team.getName().trim());
                pstmt.setString(3, team.getCode().toUpperCase().trim());
                pstmt.setString(4, team.getGroup());
                pstmt.executeUpdate();
                team.setId(nextId);
                pstmt.close();
            } else {
                // No gaps, use auto-increment
                sql = "INSERT INTO teams (name, code, group_name, played, won, drawn, lost, " +
                      "goals_for, goals_against, goal_difference, points, qualified) " +
                      "VALUES (?, ?, ?, 0, 0, 0, 0, 0, 0, 0, 0, 0)";
                pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                pstmt.setString(1, team.getName().trim());
                pstmt.setString(2, team.getCode().toUpperCase().trim());
                pstmt.setString(3, team.getGroup());
                pstmt.executeUpdate();
                
                ResultSet rs = pstmt.getGeneratedKeys();
                if (rs.next()) {
                    team.setId(rs.getInt(1));
                }
                pstmt.close();
            }
        } finally {
            dbManager.releaseConnection(conn);
        }
    }
    
    /**
     * Finds the smallest available ID by looking for gaps in the sequence.
     * Returns null if there are no gaps (should use auto-increment).
     */
    private Integer findNextAvailableId(Connection  conn) throws SQLException {
         // Find the smallest missing ID
        String sql = "SELECT t1.id + 1 AS gap " +
                     "                FROM teams t1 " +
                     "LEFT JOIN teams t2 ON t1.id + 1 = t2.id " +
                     "WHERE t2.id IS NULL " +
                     "ORDER BY gap " +
                     "LIMIT 1";
        
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = conn.prepareStatement(sql);
            rs = pstmt.executeQuery();
            
            if (rs.next()) {
                int gap = rs.getInt("gap");
                rs.close();
                pstmt.close();
                
                // Check if this gap is less than the max ID (meaning it's a real gap)
                String maxSql = "SELECT MAX(id) as max_id FROM teams";
                PreparedStatement maxPstmt = null;
                ResultSet maxRs = null;
                try {
                    maxPstmt = conn.prepareStatement(maxSql);
                    maxRs = maxPstmt.executeQuery();
                    
                    if (maxRs.next()) {
                        int maxId = maxRs.getInt("max_id");
                        if (gap <= maxId) {
                            return gap; // Found a gap in the sequence
                        }
                    }
                } finally {
                    if (maxRs != null) try { maxRs.close(); } catch (SQLException e) {}
                    if (maxPstmt != null) try { maxPstmt.close(); } catch (SQLException e) {}
                }
            }
            
            // Check if table is empty, start from 1
            String countSql = "SELECT COUNT(*) as count FROM teams";
            PreparedStatement countPstmt = null;
            ResultSet countRs = null;
            try {
                countPstmt = conn.prepareStatement(countSql);
                countRs = countPstmt.executeQuery();
                if (countRs.next() && countRs.getInt("count") == 0) {
                    return 1; // Start from 1 for empty table
                }
            } finally {
                if (countRs != null) try { countRs.close(); } catch (SQLException e) {}
                if (countPstmt != null) try { countPstmt.close(); } catch (SQLException e) {}
            }
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException e) {}
            if (pstmt != null) try { pstmt.close(); } catch (SQLException e) {}
        }
        
        return null; // No gaps found, use auto-increment
    }

    public void updateTeam(Team team) throws SQLException {
        String validation = validateTeam(team);
        if (validation != null) {
            throw new SQLException(validation);
        }
        
        // Check for duplicates (excluding current team)
        if (isTeamNameExists(team.getName(), team.getId())) {
            throw new SQLException("A team with the name '" + team.getName() + "' already exists.");
        }
        if (isTeamCodeExists(team.getCode(), team.getId())) {
            throw new SQLException("A team with the code '" + team.getCode().toUpperCase() + "' already exists.");
        }

        String sql = "UPDATE teams SET name = ?, code = ?, group_name = ? WHERE id = ?";
        
        Connection conn = null;
        PreparedStatement pstmt = null;
        try {
            conn = dbManager.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, team.getName().trim());
            pstmt.setString(2, team.getCode().toUpperCase().trim());
            pstmt.setString(3, team.getGroup());
            pstmt.setInt(4, team.getId());
            pstmt.executeUpdate();
        } finally {
            if (pstmt != null) try { pstmt.close(); } catch (SQLException e) {}
            if (conn != null) dbManager.releaseConnection(conn);
        }
    }

    public void deleteTeam(int teamId) throws SQLException {
        Connection conn = null;
        PreparedStatement pstmt = null;
        try {
            conn = dbManager.getConnection();
            
            // First delete related matches
            String deleteMatches = "DELETE FROM matches WHERE team1_id = ? OR team2_id = ?";
            pstmt = conn.prepareStatement(deleteMatches);
            pstmt.setInt(1, teamId);
            pstmt.setInt(2, teamId);
            pstmt.executeUpdate();
            pstmt.close();
            
            // Delete knockout matches
            String deleteKnockout = "DELETE FROM knockout_matches WHERE team1_id = ? OR team2_id = ?";
            pstmt = conn.prepareStatement(deleteKnockout);
            pstmt.setInt(1, teamId);
            pstmt.setInt(2, teamId);
            pstmt.executeUpdate();
            pstmt.close();
            
            // Finally delete the team
            String sql = "DELETE FROM teams WHERE id = ?";
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, teamId);
            pstmt.executeUpdate();
            pstmt.close();
            pstmt = null;
            
            // Reset auto-increment counter if the deleted ID was the highest
            resetAutoIncrementIfNeeded(conn);
        } finally {
            if (pstmt != null) try { pstmt.close(); } catch (SQLException e) {}
            if (conn != null) dbManager.releaseConnection(conn);
        }
    }
    
    /**
     * Resets the SQLite auto-increment counter to the current maximum ID.
     * This allows reusing IDs when the last team(s) are deleted.
     */
    private void resetAutoIncrementIfNeeded(Connection conn) throws SQLException {
        // Get the current maximum ID
        String maxSql = "SELECT MAX(id) as max_id FROM teams";
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = conn.prepareStatement(maxSql);
            rs = pstmt.executeQuery();
            
            if (rs.next()) {
                int maxId = rs.getInt("max_id");
                rs.close();
                pstmt.close();
                
                if (maxId == 0) {
                    // Table is empty, delete the sequence entry
                    String deleteSql = "DELETE FROM sqlite_sequence WHERE name = 'teams'";
                    PreparedStatement delPstmt = conn.prepareStatement(deleteSql);
                    delPstmt.executeUpdate();
                    delPstmt.close();
                } else {
                    // Update the sequence to the current max ID
                    String updateSql = "UPDATE sqlite_sequence SET seq = ? WHERE name = 'teams'";
                    PreparedStatement updPstmt = conn.prepareStatement(updateSql);
                    updPstmt.setInt(1, maxId);
                    updPstmt.executeUpdate();
                    updPstmt.close();
                }
            }
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException e) {}
            if (pstmt != null) try { pstmt.close(); } catch (SQLException e) {}
        }
    }

    public Team getTeamById(int id) throws SQLException {
        String sql = "SELECT * FROM teams WHERE id = ?";
        
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = dbManager.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, id);
            rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return mapResultSetToTeam(rs);
            }
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException e) {}
            if (pstmt != null) try { pstmt.close(); } catch (SQLException e) {}
            if (conn != null) dbManager.releaseConnection(conn);
        }
        return null;
    }

    public Team getTeamByName(String name) throws SQLException {
        String sql = "SELECT * FROM teams WHERE LOWER(name) = LOWER(?)";
        
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = dbManager.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, name);
            rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return mapResultSetToTeam(rs);
            }
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException e) {}
            if (pstmt != null) try { pstmt.close(); } catch (SQLException e) {}
            if (conn != null) dbManager.releaseConnection(conn);
        }
        return null;
    }

    public List<Team> getAllTeams() throws SQLException {
        List<Team> teams = new ArrayList<>();
        String sql = "SELECT * FROM teams ORDER BY name";
        
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        try {
            conn = dbManager.getConnection();
            stmt = conn.createStatement();
            rs = stmt.executeQuery(sql);
            while (rs.next()) {
                teams.add(mapResultSetToTeam(rs));
            }
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException e) {}
            if (stmt != null) try { stmt.close(); } catch (SQLException e) {}
            if (conn != null) dbManager.releaseConnection(conn);
        }
        return teams;
    }

    public List<Team> getTeamsByGroup(String group) throws SQLException {
        List<Team> teams = new ArrayList<>();
        String sql = "SELECT * FROM teams WHERE group_name = ? ORDER BY points DESC, goal_difference DESC, goals_for DESC";
        
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = dbManager.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, group);
            rs = pstmt.executeQuery();
            while (rs.next()) {
                teams.add(mapResultSetToTeam(rs));
            }
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException e) {}
            if (pstmt != null) try { pstmt.close(); } catch (SQLException e) {}
            if (conn != null) dbManager.releaseConnection(conn);
        }
        return teams;
    }

    public List<Team> getTeamsWithoutGroup() throws SQLException {
        List<Team> teams = new ArrayList<>();
        String sql = "SELECT * FROM teams WHERE group_name IS NULL OR group_name = '' ORDER BY name";
        
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        try {
            conn = dbManager.getConnection();
            stmt = conn.createStatement();
            rs = stmt.executeQuery(sql);
            while (rs.next()) {
                teams.add(mapResultSetToTeam(rs));
            }
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException e) {}
            if (stmt != null) try { stmt.close(); } catch (SQLException e) {}
            if (conn != null) dbManager.releaseConnection(conn);
        }
        return teams;
    }

    public void assignTeamToGroup(int teamId, String group) throws SQLException {
        String sql = "UPDATE teams SET group_name = ? WHERE id = ?";
        
        Connection conn = null;
        PreparedStatement pstmt = null;
        try {
            conn = dbManager.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, group);
            pstmt.setInt(2, teamId);
            pstmt.executeUpdate();
        } finally {
            if (pstmt != null) try { pstmt.close(); } catch (SQLException e) {}
            if (conn != null) dbManager.releaseConnection(conn);
        }
    }

    public void clearAllGroups() throws SQLException {
        String sql = "UPDATE teams SET group_name = NULL";
        
        Connection conn = null;
        Statement stmt = null;
        try {
            conn = dbManager.getConnection();
            stmt = conn.createStatement();
            stmt.executeUpdate(sql);
        } finally {
            if (stmt != null) try { stmt.close(); } catch (SQLException e) {}
            if (conn != null) dbManager.releaseConnection(conn);
        }
    }

    public int getTeamCountInGroup(String group) throws SQLException {
        String sql = "SELECT COUNT(*) FROM teams WHERE group_name = ?";
        
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = dbManager.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, group);
            rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException e) {}
            if (pstmt != null) try { pstmt.close(); } catch (SQLException e) {}
            if (conn != null) dbManager.releaseConnection(conn);
        }
        return 0;
    }

    public void exportTeamsToJson(String filePath) throws IOException, SQLException {
        List<Team> teams = getAllTeams();
        try (Writer writer = new FileWriter(filePath)) {
            gson.toJson(teams, writer);
        }
    }

    public void importTeamsFromJson(String filePath) throws IOException, SQLException {
        try (Reader reader = new FileReader(filePath)) {
            Type teamListType = new TypeToken<ArrayList<Team>>(){}.getType();
            List<Team> teams = gson.fromJson(reader, teamListType);
            
            for (Team team : teams) {
                try {
                    // Check if team already exists
                    Team existing = getTeamByName(team.getName());
                    if (existing == null) {
                        addTeam(team);
                    }
                } catch (SQLException e) {
                    System.err.println("Error importing team " + team.getName() + ": " + e.getMessage());
                }
            }
        }
    }

    public void resetTeamStatistics(int teamId) throws SQLException {
        String sql = "UPDATE teams SET played = 0, won = 0, drawn = 0, lost = 0, " +
                     "goals_for = 0, goals_against = 0, goal_difference = 0, points = 0, qualified = 0 " +
                     "WHERE id = ?";
        
        Connection conn = null;
        PreparedStatement pstmt = null;
        try {
            conn = dbManager.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, teamId);
            pstmt.executeUpdate();
        } finally {
            if (pstmt != null) try { pstmt.close(); } catch (SQLException e) {}
            if (conn != null) dbManager.releaseConnection(conn);
        }
    }

    public void resetAllTeamStatistics() throws SQLException {
        String sql = "UPDATE teams SET played = 0, won = 0, drawn = 0, lost = 0, " +
                     "goals_for = 0, goals_against = 0, goal_difference = 0, points = 0, qualified = 0";
        
        Connection conn = null;
        Statement stmt = null;
        try {
            conn = dbManager.getConnection();
            stmt = conn.createStatement();
            stmt.executeUpdate(sql);
        } finally {
            if (stmt != null) try { stmt.close(); } catch (SQLException e) {}
            if (conn != null) dbManager.releaseConnection(conn);
        }
    }

    public void updateTeamStatistics(Team team) throws SQLException {
        String sql = "UPDATE teams SET played = ?, won = ?, drawn = ?, lost = ?, " +
                     "goals_for = ?, goals_against = ?, goal_difference = ?, points = ?, qualified = ? " +
                     "WHERE id = ?";
        
        Connection conn = null;
        PreparedStatement pstmt = null;
        try {
            conn = dbManager.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, team.getPlayed());
            pstmt.setInt(2, team.getWon());
            pstmt.setInt(3, team.getDrawn());
            pstmt.setInt(4, team.getLost());
            pstmt.setInt(5, team.getGoalsFor());
            pstmt.setInt(6, team.getGoalsAgainst());
            pstmt.setInt(7, team.getGoalDifference());
            pstmt.setInt(8, team.getPoints());
            pstmt.setInt(9, team.isQualified() ? 1 : 0);
            pstmt.setInt(10, team.getId());
            pstmt.executeUpdate();
        } finally {
            if (pstmt != null) try { pstmt.close(); } catch (SQLException e) {}
            if (conn != null) dbManager.releaseConnection(conn);
        }
    }

    public List<Team> searchTeams(String query) throws SQLException {
        List<Team> teams = new ArrayList<>();
        String sql = "SELECT * FROM teams WHERE LOWER(name) LIKE ? OR LOWER(code) LIKE ? ORDER BY name";
        
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = dbManager.getConnection();
            pstmt = conn.prepareStatement(sql);
            String searchPattern = "%" + query.toLowerCase() + "%";
            pstmt.setString(1, searchPattern);
            pstmt.setString(2, searchPattern);
            rs = pstmt.executeQuery();
            while (rs.next()) {
                teams.add(mapResultSetToTeam(rs));
            }
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException e) {}
            if (pstmt != null) try { pstmt.close(); } catch (SQLException e) {}
            if (conn != null) dbManager.releaseConnection(conn);
        }
        return teams;
    }

    private Team mapResultSetToTeam(ResultSet rs) throws SQLException {
        Team team = new Team();
        team.setId(rs.getInt("id"));
        team.setName(rs.getString("name"));
        team.setCode(rs.getString("code"));
        team.setGroup(rs.getString("group_name"));
        team.setPlayed(rs.getInt("played"));
        team.setWon(rs.getInt("won"));
        team.setDrawn(rs.getInt("drawn"));
        team.setLost(rs.getInt("lost"));
        team.setGoalsFor(rs.getInt("goals_for"));
        team.setGoalsAgainst(rs.getInt("goals_against"));
        team.setGoalDifference(rs.getInt("goal_difference"));
        team.setPoints(rs.getInt("points"));
        team.setQualified(rs.getInt("qualified") == 1);
        return team;
    }
}
