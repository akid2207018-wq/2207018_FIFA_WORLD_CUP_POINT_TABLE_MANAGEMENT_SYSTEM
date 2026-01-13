package com.fifaworldcup.service;

import com.fifaworldcup.database.DatabaseManager;
import com.fifaworldcup.model.KnockoutMatch;
import com.fifaworldcup.model.Team;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class KnockoutService {
    private DatabaseManager dbManager;
    private TeamService teamService;
    private StandingsService standingsService;

    public KnockoutService() {
        this.dbManager = DatabaseManager.getInstance();
        this.teamService = new TeamService();
        this.standingsService = new StandingsService();
    }

    /**
     * Generate Round of 16 bracket based on FIFA World Cup format:
     * Match 1: 1A vs 2B
     * Match 2: 1C vs 2D
     * Match 3: 1E vs 2F
     * Match 4: 1G vs 2H
     * Match 5: 1B vs 2A
     * Match 6: 1D vs 2C
     * Match 7: 1F vs 2E
     * Match 8: 1H vs 2G
     */
    public void generateRoundOf16Bracket() throws SQLException {
        
        
        clearKnockoutMatches();
        
        
        standingsService.determineAllGroupQualifiers();
        
        
        String[] groups = {"A", "B", "C", "D", "E", "F", "G", "H"};
        Team[] winners = new Team[8];
        Team[] runnersUp = new Team[8];
        
        for (int i = 0; i < groups.length; i++) {
            List<Team> standings = standingsService.getGroupStandings(groups[i]);
            if (standings.size() >= 1) winners[i] = standings.get(0);
            if (standings.size() >= 2) runnersUp[i] = standings.get(1);
        }
        
        
        createKnockoutMatch(1, "ROUND_16", winners[0], runnersUp[1], "L1");  // 1A vs 2B
        createKnockoutMatch(2, "ROUND_16", winners[2], runnersUp[3], "L2");  // 1C vs 2D
        createKnockoutMatch(3, "ROUND_16", winners[4], runnersUp[5], "L3");  // 1E vs 2F
        createKnockoutMatch(4, "ROUND_16", winners[6], runnersUp[7], "L4");  // 1G vs 2H
        
        
        createKnockoutMatch(5, "ROUND_16", winners[1], runnersUp[0], "R1");  // 1B vs 2A
        createKnockoutMatch(6, "ROUND_16", winners[3], runnersUp[2], "R2");  // 1D vs 2C
        createKnockoutMatch(7, "ROUND_16", winners[5], runnersUp[4], "R3");  // 1F vs 2E
        createKnockoutMatch(8, "ROUND_16", winners[7], runnersUp[6], "R4");  // 1H vs 2G
        
        
        createKnockoutMatch(9, "QUARTER", null, null, "QL1");   // Winner M1 vs Winner M2
        createKnockoutMatch(10, "QUARTER", null, null, "QL2");  // Winner M3 vs Winner M4
        createKnockoutMatch(11, "QUARTER", null, null, "QR1");  // Winner M5 vs Winner M6
        createKnockoutMatch(12, "QUARTER", null, null, "QR2");  // Winner M7 vs Winner M8
        
        
        createKnockoutMatch(13, "SEMI", null, null, "SL");      // Winner QF1 vs Winner QF2
        createKnockoutMatch(14, "SEMI", null, null, "SR");      // Winner QF3 vs Winner QF4
        
        
        createKnockoutMatch(15, "FINAL", null, null, "F");      // Winner SF1 vs Winner SF2
    }

    private void createKnockoutMatch(int matchNumber, String round, Team team1, Team team2, String bracketPosition) throws SQLException {
        KnockoutMatch match = new KnockoutMatch();
        match.setMatchNumber(matchNumber);
        match.setRound(round);
        match.setTeam1Id(team1 != null ? team1.getId() : 0);
        match.setTeam2Id(team2 != null ? team2.getId() : 0);
        match.setTeam1Name(team1 != null ? team1.getName() : "TBD");
        match.setTeam2Name(team2 != null ? team2.getName() : "TBD");
        match.setBracketPosition(bracketPosition);
        match.setCompleted(false);
        
        addKnockoutMatch(match);
    }

    public void addKnockoutMatch(KnockoutMatch match) throws SQLException {
        String sql = "INSERT INTO knockout_matches (match_number, round, team1_id, team2_id, " +
                     "team1_score, team2_score, winner_id, bracket_position, completed) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        Connection conn = dbManager.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, match.getMatchNumber());
            pstmt.setString(2, match.getRound());
            pstmt.setInt(3, match.getTeam1Id());
            pstmt.setInt(4, match.getTeam2Id());
            pstmt.setInt(5, match.getTeam1Score());
            pstmt.setInt(6, match.getTeam2Score());
            pstmt.setInt(7, match.getWinnerId());
            pstmt.setString(8, match.getBracketPosition());
            pstmt.setInt(9, match.isCompleted() ? 1 : 0);
            pstmt.executeUpdate();
            
            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                match.setId(rs.getInt(1));
            }
        } finally {
            dbManager.releaseConnection(conn);
        }
    }

    public void updateKnockoutMatchResult(int matchId, int team1Score, int team2Score, int winnerId) throws SQLException {
        // In knockout, there must be a winner (no draws allowed in final result)
        if (team1Score == team2Score && winnerId == 0) {
            throw new SQLException("Knockout matches must have a winner. Set winner for penalty shootout.");
        }
        
        String sql = "UPDATE knockout_matches SET team1_score = ?, team2_score = ?, winner_id = ?, completed = 1 WHERE id = ?";
        
        Connection conn = dbManager.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, team1Score);
            pstmt.setInt(2, team2Score);
            pstmt.setInt(3, winnerId);
            pstmt.setInt(4, matchId);
            pstmt.executeUpdate();
        } finally {
            dbManager.releaseConnection(conn);
        }
        
        // Advance winner to next round
        advanceWinnerToNextRound(matchId);
    }

    public List<KnockoutMatch> getKnockoutMatchesByRound(String round) throws SQLException {
        List<KnockoutMatch> matches = new ArrayList<>();
        String sql = "SELECT km.*, t1.name as team1_name, t2.name as team2_name, tw.name as winner_name " +
                     "FROM knockout_matches km " +
                     "LEFT JOIN teams t1 ON km.team1_id = t1.id " +
                     "LEFT JOIN teams t2 ON km.team2_id = t2.id " +
                     "LEFT JOIN teams tw ON km.winner_id = tw.id " +
                     "WHERE km.round = ? ORDER BY km.match_number";
        
        Connection conn = dbManager.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, round);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                matches.add(mapResultSetToKnockoutMatch(rs));
            }
        } finally {
            dbManager.releaseConnection(conn);
        }
        return matches;
    }

    public List<KnockoutMatch> getAllKnockoutMatches() throws SQLException {
        List<KnockoutMatch> matches = new ArrayList<>();
        String sql = "SELECT km.*, t1.name as team1_name, t2.name as team2_name, tw.name as winner_name " +
                     "FROM knockout_matches km " +
                     "LEFT JOIN teams t1 ON km.team1_id = t1.id " +
                     "LEFT JOIN teams t2 ON km.team2_id = t2.id " +
                     "LEFT JOIN teams tw ON km.winner_id = tw.id " +
                     "ORDER BY km.match_number";
        
        Connection conn = dbManager.getConnection();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                matches.add(mapResultSetToKnockoutMatch(rs));
            }
        } finally {
            dbManager.releaseConnection(conn);
        }
        return matches;
    }

    public KnockoutMatch getKnockoutMatchById(int id) throws SQLException {
        String sql = "SELECT km.*, t1.name as team1_name, t2.name as team2_name, tw.name as winner_name " +
                     "FROM knockout_matches km " +
                     "LEFT JOIN teams t1 ON km.team1_id = t1.id " +
                     "LEFT JOIN teams t2 ON km.team2_id = t2.id " +
                     "LEFT JOIN teams tw ON km.winner_id = tw.id " +
                     "WHERE km.id = ?";
        
        Connection conn = dbManager.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return mapResultSetToKnockoutMatch(rs);
            }
        } finally {
            dbManager.releaseConnection(conn);
        }
        return null;
    }
    
    /**
     * Get knockout match by two team IDs (in any order)
     */
    public KnockoutMatch getKnockoutMatchByTeams(int team1Id, int team2Id) throws SQLException {
        String sql = "SELECT km.*, t1.name as team1_name, t2.name as team2_name, tw.name as winner_name " +
                     "FROM knockout_matches km " +
                     "LEFT JOIN teams t1 ON km.team1_id = t1.id " +
                     "LEFT JOIN teams t2 ON km.team2_id = t2.id " +
                     "LEFT JOIN teams tw ON km.winner_id = tw.id " +
                     "WHERE (km.team1_id = ? AND km.team2_id = ?) OR (km.team1_id = ? AND km.team2_id = ?)";
        
        Connection conn = dbManager.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, team1Id);
            pstmt.setInt(2, team2Id);
            pstmt.setInt(3, team2Id);
            pstmt.setInt(4, team1Id);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return mapResultSetToKnockoutMatch(rs);
            }
        } finally {
            dbManager.releaseConnection(conn);
        }
        return null;
    }

    public void advanceWinnerToNextRound(int matchId) throws SQLException {
        KnockoutMatch match = getKnockoutMatchById(matchId);
        if (match == null || !match.isCompleted() || match.getWinnerId() == 0) {
            return;
        }

        int winnerId = match.getWinnerId();
        int matchNumber = match.getMatchNumber();
        
        // Determine which next round match to update based on bracket position
        int nextMatchNumber = 0;
        boolean isTeam1 = false;
        
        // Round of 16 -> Quarter Finals
        if (matchNumber >= 1 && matchNumber <= 8) {
            if (matchNumber == 1 || matchNumber == 2) {
                nextMatchNumber = 9; // QF1
                isTeam1 = (matchNumber == 1);
            } else if (matchNumber == 3 || matchNumber == 4) {
                nextMatchNumber = 10; // QF2
                isTeam1 = (matchNumber == 3);
            } else if (matchNumber == 5 || matchNumber == 6) {
                nextMatchNumber = 11; // QF3
                isTeam1 = (matchNumber == 5);
            } else if (matchNumber == 7 || matchNumber == 8) {
                nextMatchNumber = 12; // QF4
                isTeam1 = (matchNumber == 7);
            }
        }
        // Quarter Finals -> Semi Finals
        else if (matchNumber >= 9 && matchNumber <= 12) {
            if (matchNumber == 9 || matchNumber == 10) {
                nextMatchNumber = 13; // SF1
                isTeam1 = (matchNumber == 9);
            } else {
                nextMatchNumber = 14; // SF2
                isTeam1 = (matchNumber == 11);
            }
        }
        // Semi Finals -> Final
        else if (matchNumber == 13 || matchNumber == 14) {
            nextMatchNumber = 15; // Final
            isTeam1 = (matchNumber == 13);
        }
        
        if (nextMatchNumber > 0) {
            String sql = isTeam1 ? 
                "UPDATE knockout_matches SET team1_id = ? WHERE match_number = ?" :
                "UPDATE knockout_matches SET team2_id = ? WHERE match_number = ?";
            
            Connection conn = dbManager.getConnection();
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, winnerId);
                pstmt.setInt(2, nextMatchNumber);
                pstmt.executeUpdate();
            } finally {
                dbManager.releaseConnection(conn);
            }
        }
    }

    public void generateNextRound(String currentRound) throws SQLException {
        // This is handled automatically by advanceWinnerToNextRound
        // But can be used to regenerate if needed
        List<KnockoutMatch> matches = getKnockoutMatchesByRound(currentRound);
        for (KnockoutMatch match : matches) {
            if (match.isCompleted() && match.getWinnerId() > 0) {
                advanceWinnerToNextRound(match.getId());
            }
        }
    }

    public void clearKnockoutMatches() throws SQLException {
        String sql = "DELETE FROM knockout_matches";
        
        Connection conn = dbManager.getConnection();
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
        } finally {
            dbManager.releaseConnection(conn);
        }
    }

    public Team getChampion() throws SQLException {
        List<KnockoutMatch> finalMatches = getKnockoutMatchesByRound("FINAL");
        if (!finalMatches.isEmpty()) {
            KnockoutMatch finalMatch = finalMatches.get(0);
            if (finalMatch.isCompleted() && finalMatch.getWinnerId() > 0) {
                return teamService.getTeamById(finalMatch.getWinnerId());
            }
        }
        return null;
    }

    private KnockoutMatch mapResultSetToKnockoutMatch(ResultSet rs) throws SQLException {
        KnockoutMatch match = new KnockoutMatch();
        match.setId(rs.getInt("id"));
        match.setMatchNumber(rs.getInt("match_number"));
        match.setRound(rs.getString("round"));
        match.setTeam1Id(rs.getInt("team1_id"));
        match.setTeam2Id(rs.getInt("team2_id"));
        match.setTeam1Name(rs.getString("team1_name") != null ? rs.getString("team1_name") : "TBD");
        match.setTeam2Name(rs.getString("team2_name") != null ? rs.getString("team2_name") : "TBD");
        match.setTeam1Score(rs.getInt("team1_score"));
        match.setTeam2Score(rs.getInt("team2_score"));
        match.setWinnerId(rs.getInt("winner_id"));
        match.setWinnerName(rs.getString("winner_name"));
        match.setBracketPosition(rs.getString("bracket_position"));
        match.setCompleted(rs.getInt("completed") == 1);
        return match;
    }
}
