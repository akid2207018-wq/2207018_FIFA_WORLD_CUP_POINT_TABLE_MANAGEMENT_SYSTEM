package com.fifaworldcup.model;

import java.time.LocalDateTime;

public class Match {
    private int id;
    private int team1Id;
    private int team2Id;
    private String team1Name;
    private String team2Name;
    private int team1Score;
    private int team2Score;
    private String stage;
    private String group;
    private LocalDateTime matchDate;
    private boolean completed;
    private int matchNumber;

    public Match() {
    }

    public Match(int id, int team1Id, int team2Id, String stage, String group) {
        this.id = id;
        this.team1Id = team1Id;
        this.team2Id = team2Id;
        this.stage = stage;
        this.group = group;
        this.team1Score = 0;
        this.team2Score = 0;
        this.completed = false;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getTeam1Id() {
        return team1Id;
    }

    public void setTeam1Id(int team1Id) {
        this.team1Id = team1Id;
    }

    public int getTeam2Id() {
        return team2Id;
    }

    public void setTeam2Id(int team2Id) {
        this.team2Id = team2Id;
    }

    public String getTeam1Name() {
        return team1Name;
    }

    public void setTeam1Name(String team1Name) {
        this.team1Name = team1Name;
    }

    public String getTeam2Name() {
        return team2Name;
    }

    public void setTeam2Name(String team2Name) {
        this.team2Name = team2Name;
    }

    public int getTeam1Score() {
        return team1Score;
    }

    public void setTeam1Score(int team1Score) {
        this.team1Score = team1Score;
    }

    public int getTeam2Score() {
        return team2Score;
    }

    public void setTeam2Score(int team2Score) {
        this.team2Score = team2Score;
    }

    public String getStage() {
        return stage;
    }

    public void setStage(String stage) {
        this.stage = stage;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public LocalDateTime getMatchDate() {
        return matchDate;
    }

    public void setMatchDate(LocalDateTime matchDate) {
        this.matchDate = matchDate;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public int getMatchNumber() {
        return matchNumber;
    }

    public void setMatchNumber(int matchNumber) {
        this.matchNumber = matchNumber;
    }

    public String getStatusText() {
        return completed ? "Completed" : "Pending";
    }

    @Override
    public String toString() {
        return team1Name + " vs " + team2Name;
    }
}
