package com.fifaworldcup.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * FIFA 2022 Data Service - Local JSON Database
 * 
 * This service provides real FIFA World Cup 2022 data from local JSON files:
 * - json/fifa2022_teams.json - All 32 teams with their groups
 * - json/fifa2022_matches.json - Complete match schedule with results
 * 
 * Data includes:
 * - All 32 teams with their groups
 * - Complete match schedule with results  
 * - Group stage matches (48 matches)
 * - Knockout stage matches: Round of 16, Quarter-finals, Semi-finals, Final (16 matches)
 */
public class Fifa2022ApiService {
    private static final String TEAMS_JSON_PATH = "json/fifa2022_teams.json";
    private static final String MATCHES_JSON_PATH = "json/fifa2022_matches.json";
    
    private List<Map<String, Object>> cachedTeams = null;
    private List<Map<String, Object>> cachedMatches = null;
    
    public Fifa2022ApiService() {
        System.out.println("FIFA 2022 Data Service initialized - using local JSON database");
    }
    
    /**
     * Load JSON file from resources or file system
     */
    private String loadJsonFile(String filePath) throws IOException {
        // Try loading from classpath first (for packaged JAR)
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(filePath);
        if (inputStream != null) {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                return reader.lines().collect(Collectors.joining("\n"));
            }
        }
        
        // Try loading from file system (for development)
        File file = new File(filePath);
        if (file.exists()) {
            return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
        }
        
        throw new IOException("Could not find JSON file: " + filePath);
    }
    
    /**
     * Fetch all 32 teams from FIFA World Cup 2022
     */
    public List<Map<String, Object>> fetchAllTeams() throws Exception {
        if (cachedTeams != null) {
            System.out.println("Returning cached teams data (" + cachedTeams.size() + " teams)");
            return new ArrayList<>(cachedTeams);
        }
        
        System.out.println("Loading teams from: " + TEAMS_JSON_PATH);
        String jsonContent = loadJsonFile(TEAMS_JSON_PATH);
        
        JsonObject root = JsonParser.parseString(jsonContent).getAsJsonObject();
        JsonArray teamsArray = root.getAsJsonArray("data");
        
        List<Map<String, Object>> teams = new ArrayList<>();
        for (JsonElement element : teamsArray) {
            JsonObject teamObj = element.getAsJsonObject();
            Map<String, Object> team = new HashMap<>();
            
            team.put("id", teamObj.get("id").getAsInt());
            team.put("name", teamObj.get("name").getAsString());
            team.put("code", teamObj.get("code").getAsString());
            team.put("group", teamObj.get("group").getAsString());
            if (teamObj.has("flag")) {
                team.put("flag", teamObj.get("flag").getAsString());
            }
            
            teams.add(team);
        }
        
        cachedTeams = teams;
        System.out.println("✓ Loaded " + teams.size() + " teams from local database");
        return new ArrayList<>(teams);
    }
    
    /**
     * Load all matches from JSON file
     */
    private List<Map<String, Object>> loadAllMatches() throws Exception {
        if (cachedMatches != null) {
            return cachedMatches;
        }
        
        System.out.println("Loading matches from: " + MATCHES_JSON_PATH);
        String jsonContent = loadJsonFile(MATCHES_JSON_PATH);
        
        JsonArray matchesArray = JsonParser.parseString(jsonContent).getAsJsonArray();
        
        List<Map<String, Object>> matches = new ArrayList<>();
        for (JsonElement element : matchesArray) {
            JsonObject matchObj = element.getAsJsonObject();
            Map<String, Object> match = new HashMap<>();
            
            match.put("id", matchObj.get("id").getAsInt());
            match.put("matchNumber", matchObj.get("matchNumber").getAsInt());
            match.put("stage", matchObj.get("stage").getAsString());
            match.put("group", matchObj.has("group") && !matchObj.get("group").isJsonNull() 
                    ? matchObj.get("group").getAsString() : null);
            match.put("team1Id", matchObj.get("team1Id").getAsInt());
            match.put("team2Id", matchObj.get("team2Id").getAsInt());
            match.put("team1Name", matchObj.get("team1Name").getAsString());
            match.put("team2Name", matchObj.get("team2Name").getAsString());
            match.put("team1Score", matchObj.get("team1Score").getAsInt());
            match.put("team2Score", matchObj.get("team2Score").getAsInt());
            match.put("completed", matchObj.get("completed").getAsBoolean());
            
            // Penalty scores for knockout matches
            if (matchObj.has("team1PenaltyScore")) {
                match.put("team1PenaltyScore", matchObj.get("team1PenaltyScore").getAsInt());
            }
            if (matchObj.has("team2PenaltyScore")) {
                match.put("team2PenaltyScore", matchObj.get("team2PenaltyScore").getAsInt());
            }
            
            matches.add(match);
        }
        
        cachedMatches = matches;
        System.out.println("✓ Loaded " + matches.size() + " matches from local database");
        return cachedMatches;
    }
    
    /**
     * Fetch schedule by specific date
     * Note: New JSON format doesn't include date field
     */
    public List<Map<String, Object>> fetchScheduleByDate(String date) throws Exception {
        List<Map<String, Object>> allMatches = loadAllMatches();
        System.out.println("Note: Date filtering not available in new JSON format");
        return allMatches;
    }
    
    /**
     * Fetch all group stage matches (48 matches)
     */
    public List<Map<String, Object>> fetchAllGroupStageMatches() throws Exception {
        System.out.println("Loading group stage matches...");
        List<Map<String, Object>> allMatches = loadAllMatches();
        
        List<Map<String, Object>> groupMatches = allMatches.stream()
                .filter(m -> "GROUP".equals(m.get("stage")))
                .collect(Collectors.toList());
        
        System.out.println("✓ Found " + groupMatches.size() + " group stage matches");
        return groupMatches;
    }
    
    /**
     * Fetch all knockout stage matches (Round of 16 to Final)
     */
    public List<Map<String, Object>> fetchKnockoutStageMatches() throws Exception {
        System.out.println("Loading knockout stage matches...");
        List<Map<String, Object>> allMatches = loadAllMatches();
        
        List<String> knockoutStages = Arrays.asList(
            "ROUND_OF_16", 
            "QUARTER_FINAL", 
            "SEMI_FINAL", 
            "THIRD_PLACE",
            "FINAL"
        );
        
        List<Map<String, Object>> knockoutMatches = allMatches.stream()
                .filter(m -> knockoutStages.contains(m.get("stage")))
                .collect(Collectors.toList());
        
        System.out.println("✓ Found " + knockoutMatches.size() + " knockout stage matches");
        return knockoutMatches;
    }
    
    /**
     * Get team code from team name
     */
    public String getTeamCode(String teamName) {
        Map<String, String> teamCodes = new HashMap<>();
        teamCodes.put("Qatar", "QAT");
        teamCodes.put("Ecuador", "ECU");
        teamCodes.put("Senegal", "SEN");
        teamCodes.put("Netherlands", "NED");
        teamCodes.put("England", "ENG");
        teamCodes.put("Iran", "IRN");
        teamCodes.put("USA", "USA");
        teamCodes.put("Wales", "WAL");
        teamCodes.put("Argentina", "ARG");
        teamCodes.put("Saudi Arabia", "KSA");
        teamCodes.put("Mexico", "MEX");
        teamCodes.put("Poland", "POL");
        teamCodes.put("France", "FRA");
        teamCodes.put("Australia", "AUS");
        teamCodes.put("Denmark", "DEN");
        teamCodes.put("Tunisia", "TUN");
        teamCodes.put("Spain", "ESP");
        teamCodes.put("Costa Rica", "CRC");
        teamCodes.put("Germany", "GER");
        teamCodes.put("Japan", "JPN");
        teamCodes.put("Belgium", "BEL");
        teamCodes.put("Canada", "CAN");
        teamCodes.put("Morocco", "MAR");
        teamCodes.put("Croatia", "CRO");
        teamCodes.put("Brazil", "BRA");
        teamCodes.put("Serbia", "SRB");
        teamCodes.put("Switzerland", "SUI");
        teamCodes.put("Cameroon", "CMR");
        teamCodes.put("Portugal", "POR");
        teamCodes.put("Ghana", "GHA");
        teamCodes.put("Uruguay", "URU");
        teamCodes.put("South Korea", "KOR");
        
        return teamCodes.getOrDefault(teamName, teamName.substring(0, Math.min(3, teamName.length())).toUpperCase());
    }
    
    /**
     * Clear cached data (useful for testing or refresh)
     */
    public void clearCache() {
        cachedTeams = null;
        cachedMatches = null;
        System.out.println("Cache cleared");
    }
    
    // ==================== Test Methods ====================
    
    /**
     * Test connection to data source
     */
    public void testConnection() throws Exception {
        System.out.println("\n=== Testing FIFA 2022 Data Service ===");
        System.out.println("Data source: Local JSON files");
        System.out.println("Teams file: " + TEAMS_JSON_PATH);
        System.out.println("Matches file: " + MATCHES_JSON_PATH);
        
        try {
            List<Map<String, Object>> teams = fetchAllTeams();
            System.out.println("✓ Successfully loaded " + teams.size() + " teams");
            
            List<Map<String, Object>> matches = loadAllMatches();
            System.out.println("✓ Successfully loaded " + matches.size() + " matches");
            
            System.out.println("\n=== Connection test PASSED ===\n");
        } catch (Exception e) {
            System.out.println("\n=== Connection test FAILED ===");
            System.out.println("Error: " + e.getMessage());
            throw e;
        }
    }
}
