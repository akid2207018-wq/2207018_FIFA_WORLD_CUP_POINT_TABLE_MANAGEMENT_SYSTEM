package com.fifaworldcup.service;

import com.fifaworldcup.database.DatabaseManager;
import com.fifaworldcup.model.KnockoutMatch;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class KnockoutService {
    private DatabaseManager dbManager;

    public KnockoutService() {
        this.dbManager = DatabaseManager.getInstance();
    }

    public void generateRoundOf16Bracket() throws SQLException {
    }

    public void addKnockoutMatch(KnockoutMatch match) throws SQLException {
    }

    public void updateKnockoutMatchResult(int matchId, int team1Score, int team2Score, int winnerId) throws SQLException {
    }

    public List<KnockoutMatch> getKnockoutMatchesByRound(String round) throws SQLException {
        return new ArrayList<>();
    }

    public KnockoutMatch getKnockoutMatchById(int id) throws SQLException {
        return null;
    }

    public void advanceWinnerToNextRound(int matchId) throws SQLException {
    }

    public void generateNextRound(String currentRound) throws SQLException {
    }
}
