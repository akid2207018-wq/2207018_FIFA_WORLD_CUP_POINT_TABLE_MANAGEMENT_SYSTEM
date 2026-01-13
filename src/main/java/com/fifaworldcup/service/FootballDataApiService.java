package com.fifaworldcup.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Football-Data.org API Client
 * 
 * API Documentation: https://www.football-data.org/documentation/quickstart
 * Free Tier: 10 requests/minute, no credit card required
 * 
 * Endpoints Used:
 * - GET /v4/competitions/2000/teams - FIFA World Cup teams
 * - GET /v4/competitions/2000/matches - FIFA World Cup matches
 * 
 * Competition ID 2000 = FIFA World Cup
 */
public class FootballDataApiService {
    private static final String API_BASE_URL = "https://api.football-data.org/v4";
    private static final String COMPETITION_ID = "2000"; // FIFA World Cup
    private String apiToken;
    
    public FootballDataApiService() {
        // Get API token from environment variable or use default
        this.apiToken = System.getenv("FOOTBALL_DATA_API_TOKEN");
        if (this.apiToken == null || this.apiToken.isEmpty()) {
            // Free tier token - replace with your own from football-data.org
            this.apiToken = "d134c084ec4a4d81ae59ce456465bc49";
        }
    }
    
    public FootballDataApiService(String apiToken) {
        this.apiToken = apiToken;
    }
    
    /**
     * Fetch teams from FIFA World Cup competition
     * Returns map of team data with: id, name, shortName, tla (3-letter code)
     */
    public List<Map<String, Object>> fetchTeams() throws Exception {
        String endpoint = API_BASE_URL + "/competitions/" + COMPETITION_ID + "/teams";
        JsonObject response = makeApiRequest(endpoint);
        
        List<Map<String, Object>> teams = new ArrayList<>();
        JsonArray teamsArray = response.getAsJsonArray("teams");
        
        for (JsonElement element : teamsArray) {
            JsonObject team = element.getAsJsonObject();
            Map<String, Object> teamData = new HashMap<>();
            teamData.put("id", team.get("id").getAsInt());
            teamData.put("name", team.get("name").getAsString());
            teamData.put("shortName", team.get("shortName").getAsString());
            teamData.put("tla", team.get("tla").getAsString()); // 3-letter code
            teamData.put("crest", team.has("crest") ? team.get("crest").getAsString() : null);
            teams.add(teamData);
        }
        
        return teams;
    }
    
    /**
     * Fetch matches from FIFA World Cup competition
     * Returns list of match data with: id, homeTeam, awayTeam, score, status, group
     */
    public List<Map<String, Object>> fetchMatches() throws Exception {
        String endpoint = API_BASE_URL + "/competitions/" + COMPETITION_ID + "/matches";
        JsonObject response = makeApiRequest(endpoint);
        
        List<Map<String, Object>> matches = new ArrayList<>();
        JsonArray matchesArray = response.getAsJsonArray("matches");
        
        int matchNumber = 1;
        for (JsonElement element : matchesArray) {
            JsonObject match = element.getAsJsonObject();
            
            // Only get group stage matches
            if (!match.get("stage").getAsString().equals("GROUP_STAGE")) {
                continue;
            }
            
            Map<String, Object> matchData = new HashMap<>();
            matchData.put("matchNumber", matchNumber++);
            matchData.put("id", match.get("id").getAsInt());
            
            // Home team
            JsonObject homeTeam = match.getAsJsonObject("homeTeam");
            matchData.put("homeTeamId", homeTeam.get("id").getAsInt());
            matchData.put("homeTeamName", homeTeam.get("name").getAsString());
            matchData.put("homeTeamTla", homeTeam.get("tla").getAsString());
            
            // Away team
            JsonObject awayTeam = match.getAsJsonObject("awayTeam");
            matchData.put("awayTeamId", awayTeam.get("id").getAsInt());
            matchData.put("awayTeamName", awayTeam.get("name").getAsString());
            matchData.put("awayTeamTla", awayTeam.get("tla").getAsString());
            
            // Score
            JsonObject score = match.getAsJsonObject("score");
            JsonObject fullTime = score.getAsJsonObject("fullTime");
            matchData.put("homeScore", fullTime.has("home") && !fullTime.get("home").isJsonNull() 
                ? fullTime.get("home").getAsInt() : 0);
            matchData.put("awayScore", fullTime.has("away") && !fullTime.get("away").isJsonNull() 
                ? fullTime.get("away").getAsInt() : 0);
            
            // Status and group
            matchData.put("status", match.get("status").getAsString());
            matchData.put("completed", match.get("status").getAsString().equals("FINISHED"));
            
            // Extract group from match (e.g., "GROUP_A" -> "A")
            if (match.has("group") && !match.get("group").isJsonNull()) {
                String group = match.get("group").getAsString();
                matchData.put("group", group.replace("GROUP_", ""));
            } else {
                matchData.put("group", "A"); // Default
            }
            
            matchData.put("stage", "GROUP");
            matchData.put("matchDate", match.has("utcDate") ? match.get("utcDate").getAsString() : null);
            
            matches.add(matchData);
        }
        
        return matches;
    }
    
    /**
     * Make HTTP GET request to API endpoint
     */
    private JsonObject makeApiRequest(String endpoint) throws Exception {
        URL url = new URL(endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        
        // Set headers
        conn.setRequestMethod("GET");
        conn.setRequestProperty("X-Auth-Token", apiToken);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);
        
        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            throw new RuntimeException("API request failed: HTTP " + responseCode + 
                " - " + conn.getResponseMessage() + 
                "\nEndpoint: " + endpoint +
                "\nCheck your API token at: https://www.football-data.org/client/register");
        }
        
        // Read response
        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = in.readLine()) != null) {
            response.append(line);
        }
        in.close();
        conn.disconnect();
        
        return JsonParser.parseString(response.toString()).getAsJsonObject();
    }
    
    /**
     * Test API connection
     */
    public boolean testConnection() {
        try {
            String endpoint = API_BASE_URL + "/competitions/" + COMPETITION_ID;
            makeApiRequest(endpoint);
            return true;
        } catch (Exception e) {
            System.err.println("API connection test failed: " + e.getMessage());
            return false;
        }
    }
}
