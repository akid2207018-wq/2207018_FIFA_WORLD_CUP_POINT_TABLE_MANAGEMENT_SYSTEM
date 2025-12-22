package com.fifaworldcup.service;

import com.fifaworldcup.database.DatabaseManager;
import com.fifaworldcup.model.Match;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class MatchService {
    private DatabaseManager dbManager;
    private Gson gson;

    public MatchService() {
        this.dbManager = DatabaseManager.getInstance();
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    public void addMatch(Match match) throws SQLException {
    }

    public void updateMatchResult(int matchId, int homeScore, int awayScore) throws SQLException {
    }

    public Match getMatchById(int id) throws SQLException {
        return null;
    }

    public List<Match> getAllMatches() throws SQLException {
        return new ArrayList<>();
    }

    public List<Match> getMatchesByGroup(String group) throws SQLException {
        return new ArrayList<>();
    }

    public List<Match> getMatchesByStage(String stage) throws SQLException {
        return new ArrayList<>();
    }

    public List<Match> getMatchesByTeam(int teamId) throws SQLException {
        return new ArrayList<>();
    }

    public void generateGroupStageMatches() throws SQLException {
    }

    public void exportMatchesToJson(String filePath) throws IOException {
    }

    public void importMatchesFromJson(String filePath) throws IOException, SQLException {
    }

    public void deleteMatch(int matchId) throws SQLException {
    }
}
