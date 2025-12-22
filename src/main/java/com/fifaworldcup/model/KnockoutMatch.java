package com.fifaworldcup.model;

public class KnockoutMatch {
    private int id;
    private int matchNumber;
    private String round;
    private int team1Id;
    private int team2Id;
    private String team1Name;
    private String team2Name;
    private int team1Score;
    private int team2Score;
    private int winnerId;
    private String winnerName;
    private boolean completed;
    private String bracketPosition;

    public KnockoutMatch() {
    }

    public KnockoutMatch(int id, String round, int matchNumber, String bracketPosition) {
        this.id = id;
        this.round = round;
        this.matchNumber = matchNumber;
        this.bracketPosition = bracketPosition;
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

    public int getMatchNumber() {
        return matchNumber;
    }

    public void setMatchNumber(int matchNumber) {
        this.matchNumber = matchNumber;
    }

    public String getRound() {
        return round;
    }

    public void setRound(String round) {
        this.round = round;
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

    public int getWinnerId() {
        return winnerId;
    }

    public void setWinnerId(int winnerId) {
        this.winnerId = winnerId;
    }

    public String getWinnerName() {
        return winnerName;
    }

    public void setWinnerName(String winnerName) {
        this.winnerName = winnerName;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public String getBracketPosition() {
        return bracketPosition;
    }

    public void setBracketPosition(String bracketPosition) {
        this.bracketPosition = bracketPosition;
    }

    @Override
    public String toString() {
        return round + " - " + (team1Name != null ? team1Name : "TBD") + " vs " + (team2Name != null ? team2Name : "TBD");
    }
}
