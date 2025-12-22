package com.fifaworldcup.service;

import com.fifaworldcup.database.DatabaseManager;
import com.fifaworldcup.model.Team;

import java.sql.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class StandingsService {
    private DatabaseManager dbManager;

    public StandingsService() {
        this.dbManager = DatabaseManager.getInstance();
    }

    public List<Team> getGroupStandings(String group) throws SQLException {
        return new ArrayList<>();
    }

    public void updateStandings(int matchId) throws SQLException {
    }

    public List<Team> getQualifiedTeams() throws SQLException {
        return new ArrayList<>();
    }

    public void determineGroupQualifiers(String group) throws SQLException {
    }

    public void determineAllGroupQualifiers() throws SQLException {
    }

    private List<Team> sortTeamsByFIFARules(List<Team> teams) {
        return teams;
    }
}
