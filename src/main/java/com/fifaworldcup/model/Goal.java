package com.fifaworldcup.model;

public class Goal {
    private String playerName;
    private int teamId;

    public Goal() {
    }

    public Goal(String playerName, int teamId) {
        this.playerName = playerName;
        this.teamId = teamId;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public int getTeamId() {
        return teamId;
    }

    public void setTeamId(int teamId) {
        this.teamId = teamId;
    }

    @Override
    public String toString() {
        return playerName;
    }
}
