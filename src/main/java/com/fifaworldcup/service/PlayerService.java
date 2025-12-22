package com.fifaworldcup.service;

import com.fifaworldcup.database.DatabaseManager;
import com.fifaworldcup.model.Player;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PlayerService {
    private DatabaseManager dbManager;

    public PlayerService() {
        this.dbManager = DatabaseManager.getInstance();
    }

    public void addPlayer(Player player) throws SQLException {
    }

    public void updatePlayer(Player player) throws SQLException {
    }

    public void deletePlayer(int playerId) throws SQLException {
    }

    public Player getPlayerById(int id) throws SQLException {
        return null;
    }

    public List<Player> getAllPlayers() throws SQLException {
        return new ArrayList<>();
    }

    public List<Player> getPlayersByTeam(int teamId) throws SQLException {
        return new ArrayList<>();
    }

    public List<Player> getTopScorers(int limit) throws SQLException {
        return new ArrayList<>();
    }

    public List<Player> getTopAssists(int limit) throws SQLException {
        return new ArrayList<>();
    }

    public void updatePlayerStats(int playerId, int goals, int assists) throws SQLException {
    }
}
