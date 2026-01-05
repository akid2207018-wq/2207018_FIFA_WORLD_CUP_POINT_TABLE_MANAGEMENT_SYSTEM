package com.fifaworldcup.service;

import com.fifaworldcup.database.DatabaseManager;
import com.fifaworldcup.model.Match;
import com.fifaworldcup.model.Team;

import java.sql.*;
import java.util.*;

public class StandingsService {
    private DatabaseManager dbManager;
    private TeamService teamService;
    private MatchService matchService;

    public StandingsService() {
        this.dbManager = DatabaseManager.getInstance();
        this.teamService = new TeamService();
        this.matchService = new MatchService();
    }

    public List<Team> getGroupStandings(String group) throws SQLException {
        List<Team> teams = teamService.getTeamsByGroup(group);
        return sortTeamsByFIFARules(teams, group);
    }

    // Update standings after a match result is entered
    public void updateStandings(int matchId) throws SQLException {
        Match match = matchService.getMatchById(matchId);
        if (match == null || !match.isCompleted()) {
            return;
        }

        // Recalculate statistics for both teams from all their matches
        recalculateTeamStats(match.getTeam1Id());
        recalculateTeamStats(match.getTeam2Id());
    }

    // Recalculate all team statistics from matches
    public void recalculateTeamStats(int teamId) throws SQLException {
        Team team = teamService.getTeamById(teamId);
        if (team == null) return;

        // Reset statistics
        team.setPlayed(0);
        team.setWon(0);
        team.setDrawn(0);
        team.setLost(0);
        team.setGoalsFor(0);
        team.setGoalsAgainst(0);
        team.setGoalDifference(0);
        team.setPoints(0);

        // Get all completed matches for this team
        List<Match> matches = matchService.getMatchesByTeam(teamId);
        
        for (Match match : matches) {
            if (!match.isCompleted()) continue;

            team.setPlayed(team.getPlayed() + 1);

            int goalsFor, goalsAgainst;
            if (match.getTeam1Id() == teamId) {
                goalsFor = match.getTeam1Score();
                goalsAgainst = match.getTeam2Score();
            } else {
                goalsFor = match.getTeam2Score();
                goalsAgainst = match.getTeam1Score();
            }

            team.setGoalsFor(team.getGoalsFor() + goalsFor);
            team.setGoalsAgainst(team.getGoalsAgainst() + goalsAgainst);

            if (goalsFor > goalsAgainst) {
                team.setWon(team.getWon() + 1);
                team.setPoints(team.getPoints() + 3); // Win = 3 points
            } else if (goalsFor == goalsAgainst) {
                team.setDrawn(team.getDrawn() + 1);
                team.setPoints(team.getPoints() + 1); // Draw = 1 point
            } else {
                team.setLost(team.getLost() + 1);
                // Loss = 0 points
            }
        }

        team.setGoalDifference(team.getGoalsFor() - team.getGoalsAgainst());
        teamService.updateTeamStatistics(team);
    }

    // Recalculate all teams in a group
    public void recalculateGroupStandings(String group) throws SQLException {
        List<Team> teams = teamService.getTeamsByGroup(group);
        for (Team team : teams) {
            recalculateTeamStats(team.getId());
        }
    }

    // Recalculate all standings
    public void recalculateAllStandings() throws SQLException {
        String[] groups = {"A", "B", "C", "D", "E", "F", "G", "H"};
        for (String group : groups) {
            recalculateGroupStandings(group);
        }
    }

    public List<Team> getQualifiedTeams() throws SQLException {
        List<Team> qualified = new ArrayList<>();
        String sql = "SELECT * FROM teams WHERE qualified = 1 ORDER BY group_name, points DESC";
        
        Connection conn = dbManager.getConnection();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                qualified.add(mapResultSetToTeam(rs));
            }
        } finally {
            dbManager.releaseConnection(conn);
        }
        return qualified;
    }

    // Determine top 2 teams from a group (qualified for knockout stage)
    public void determineGroupQualifiers(String group) throws SQLException {
        // First reset qualified status for all teams in group
        String resetSql = "UPDATE teams SET qualified = 0 WHERE group_name = ?";
        Connection conn = dbManager.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(resetSql)) {
            pstmt.setString(1, group);
            pstmt.executeUpdate();
        } finally {
            dbManager.releaseConnection(conn);
        }

        // Get sorted standings
        List<Team> standings = getGroupStandings(group);
        
        // Mark top 2 as qualified
        for (int i = 0; i < Math.min(2, standings.size()); i++) {
            Team team = standings.get(i);
            team.setQualified(true);
            teamService.updateTeamStatistics(team);
        }
    }

    public void determineAllGroupQualifiers() throws SQLException {
        String[] groups = {"A", "B", "C", "D", "E", "F", "G", "H"};
        for (String group : groups) {
            determineGroupQualifiers(group);
        }
    }

    /**
     * FIFA World Cup tiebreaker rules:
     * 1. Points (3 for win, 1 for draw, 0 for loss)
     * 2. Goal difference
     * 3. Goals scored
     * 4. Head-to-head points (if only 2 teams are tied)
     * 5. Head-to-head goal difference
     * 6. Head-to-head goals scored
     * 7. Fair play points (yellow/red cards) - simplified here
     * 8. Drawing of lots - not implemented
     */
    private List<Team> sortTeamsByFIFARules(List<Team> teams, String group) throws SQLException {
        teams.sort((t1, t2) -> {
            // 1. Points
            if (t1.getPoints() != t2.getPoints()) {
                return Integer.compare(t2.getPoints(), t1.getPoints());
            }
            
            // 2. Goal difference
            if (t1.getGoalDifference() != t2.getGoalDifference()) {
                return Integer.compare(t2.getGoalDifference(), t1.getGoalDifference());
            }
            
            // 3. Goals scored
            if (t1.getGoalsFor() != t2.getGoalsFor()) {
                return Integer.compare(t2.getGoalsFor(), t1.getGoalsFor());
            }
            
            // 4-6. Head-to-head (simplified - check direct match result)
            try {
                int h2hResult = getHeadToHeadResult(t1.getId(), t2.getId());
                if (h2hResult != 0) {
                    return h2hResult;
                }
            } catch (SQLException e) {
                // Ignore and continue with alphabetical
            }
            
            // 7. Alphabetical (fallback)
            return t1.getName().compareTo(t2.getName());
        });
        
        return teams;
    }

    // Returns positive if team1 won head-to-head, negative if team2 won, 0 if draw/not played
    private int getHeadToHeadResult(int team1Id, int team2Id) throws SQLException {
        String sql = "SELECT team1_id, team2_id, team1_score, team2_score FROM matches " +
                     "WHERE completed = 1 AND stage = 'GROUP' AND " +
                     "((team1_id = ? AND team2_id = ?) OR (team1_id = ? AND team2_id = ?))";
        
        Connection conn = dbManager.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, team1Id);
            pstmt.setInt(2, team2Id);
            pstmt.setInt(3, team2Id);
            pstmt.setInt(4, team1Id);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                int matchTeam1Id = rs.getInt("team1_id");
                int score1 = rs.getInt("team1_score");
                int score2 = rs.getInt("team2_score");
                
                int team1Goals, team2Goals;
                if (matchTeam1Id == team1Id) {
                    team1Goals = score1;
                    team2Goals = score2;
                } else {
                    team1Goals = score2;
                    team2Goals = score1;
                }
                
                return Integer.compare(team1Goals, team2Goals);
            }
        } finally {
            dbManager.releaseConnection(conn);
        }
        return 0;
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
