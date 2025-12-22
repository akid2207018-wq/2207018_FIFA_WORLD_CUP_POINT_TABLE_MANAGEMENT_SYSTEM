package com.fifaworldcup.service;

import com.fifaworldcup.database.DatabaseManager;
import com.fifaworldcup.model.Team;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
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

    public void addTeam(Team team) throws SQLException {
    }

    public void updateTeam(Team team) throws SQLException {
    }

    public void deleteTeam(int teamId) throws SQLException {
    }

    public Team getTeamById(int id) throws SQLException {
        return null;
    }

    public List<Team> getAllTeams() throws SQLException {
        return new ArrayList<>();
    }

    public List<Team> getTeamsByGroup(String group) throws SQLException {
        return new ArrayList<>();
    }

    public void assignTeamToGroup(int teamId, String group) throws SQLException {
    }

    public void exportTeamsToJson(String filePath) throws IOException {
    }

    public void importTeamsFromJson(String filePath) throws IOException, SQLException {
    }

    public void resetTeamStatistics(int teamId) throws SQLException {
    }

    public void updateTeamStatistics(Team team) throws SQLException {
    }
}
