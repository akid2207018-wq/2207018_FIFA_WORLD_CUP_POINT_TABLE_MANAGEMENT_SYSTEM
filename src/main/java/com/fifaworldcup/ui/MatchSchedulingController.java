package com.fifaworldcup.ui;

import com.fifaworldcup.database.DatabaseManager;
import com.fifaworldcup.model.Match;
import com.fifaworldcup.model.Team;
import com.fifaworldcup.service.Fifa2022ApiService;
import com.fifaworldcup.service.MatchService;
import com.fifaworldcup.service.StandingsService;
import com.fifaworldcup.service.TeamService;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.concurrent.Task;

import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MatchSchedulingController {
    @FXML
    private TableView<Match> tblMatches;
    
    @FXML
    private TableColumn<Match, Integer> colMatchNumber;
    
    @FXML
    private TableColumn<Match, String> colTeam1;
    
    @FXML
    private TableColumn<Match, String> colTeam2;
    
    @FXML
    private TableColumn<Match, String> colGroup;
    
    @FXML
    private TableColumn<Match, String> colStage;
    
    @FXML
    private TableColumn<Match, String> colStatus;
    
    @FXML
    private TableColumn<Match, String> colMatchDate;
    
    @FXML
    private ComboBox<String> cmbFilterGroup;
    
    @FXML
    private Button btnImportFromAPI;
    
    @FXML
    private Button btnImportSchedule;
    
    @FXML
    private Button btnClearSchedule;
    
    @FXML
    private Button btnExportSchedule;
    
    @FXML
    private Button btnRefresh;
    
    @FXML
    private Button btnBack;
    
    @FXML
    private Label lblTotalMatches;
    
    @FXML
    private Label lblStatus;

    private MatchService matchService;
    private TeamService teamService;
    private Fifa2022ApiService apiService;
    private StandingsService standingsService;
    private ObservableList<Match> matchesList;
    private static final DateTimeFormatter DISPLAY_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public MatchSchedulingController() {
        this.matchService = new MatchService();
        this.teamService = new TeamService();
        this.apiService = new Fifa2022ApiService();
        this.standingsService = new StandingsService();
        this.matchesList = FXCollections.observableArrayList();
    }

    @FXML
    public void initialize() {
        if (cmbFilterGroup != null) {
            cmbFilterGroup.getItems().addAll("All", "A", "B", "C", "D", "E", "F", "G", "H");
            cmbFilterGroup.setValue("All");
        }
        
        // Setup table columns
        colMatchNumber.setCellValueFactory(new PropertyValueFactory<>("matchNumber"));
        colTeam1.setCellValueFactory(new PropertyValueFactory<>("team1Name"));
        colTeam2.setCellValueFactory(new PropertyValueFactory<>("team2Name"));
        colGroup.setCellValueFactory(new PropertyValueFactory<>("group"));
        colStage.setCellValueFactory(new PropertyValueFactory<>("stage"));
        colStatus.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().isCompleted() ? "Completed" : "Pending"));
        colMatchDate.setCellValueFactory(cellData -> {
            if (cellData.getValue().getMatchDate() != null) {
                return new SimpleStringProperty(cellData.getValue().getMatchDate().format(DISPLAY_DATE_FORMATTER));
            }
            return new SimpleStringProperty("-");
        });
        
        tblMatches.setItems(matchesList);
        loadMatches();
    }

    public void setMatchService(MatchService matchService) {
        this.matchService = matchService;
    }

    @FXML
    public void handleClearSchedule() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Clear");
        confirm.setHeaderText("Clear All Matches");
        confirm.setContentText("Are you sure you want to delete all matches? This will also reset team statistics.");
        
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try {
                matchService.clearAllMatches();
                loadMatches();
                lblStatus.setText("All matches cleared!");
                lblStatus.setStyle("-fx-text-fill: blue;");
            } catch (SQLException e) {
                showAlert("Database Error", "Failed to clear matches: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }

    @FXML
    public void handleExportSchedule() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Schedule to JSON");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON Files", "*.json"));
        fileChooser.setInitialFileName("match_schedule.json");
        
        Stage stage = (Stage) btnExportSchedule.getScene().getWindow();
        File file = fileChooser.showSaveDialog(stage);
        
        if (file != null) {
            try {
                matchService.exportMatchesToJson(file.getAbsolutePath());
                lblStatus.setText("Schedule exported to " + file.getName());
                lblStatus.setStyle("-fx-text-fill: green;");
            } catch (Exception e) {
                showAlert("Export Error", "Failed to export schedule: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }
    
    @FXML
    public void handleImportFromAPI() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Import from FIFA 2022 API");
        confirm.setHeaderText("Load FIFA World Cup 2022 Schedule");
        confirm.setContentText("This will import group stage match schedule from FIFA World Cup 2022.\nExisting schedule will be replaced. Continue?");
        
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }
        
        if (btnImportFromAPI != null) {
            btnImportFromAPI.setDisable(true);
        }
        lblStatus.setText("Loading matches from FIFA 2022 API...");
        lblStatus.setStyle("-fx-text-fill: blue;");
        
        // Run API call in background thread
        Task<Void> task = new Task<Void>() {
            private int importedCount = 0;
            private String errorMessage = null;
            
            @Override
            protected Void call() throws Exception {
                try {
                    // First, fetch and import teams if needed
                    updateMessage("Fetching teams from API...");
                    List<Map<String, Object>> apiTeams = apiService.fetchAllTeams();
                    
                    for (Map<String, Object> apiTeam : apiTeams) {
                        String teamName = (String) apiTeam.get("name");
                        String teamCode = (String) apiTeam.get("code");
                        String groupName = (String) apiTeam.get("group");
                        
                        try {
                            Team existingTeam = teamService.getTeamByName(teamName);
                            if (existingTeam == null) {
                                Team newTeam = new Team();
                                newTeam.setName(teamName);
                                newTeam.setCode(teamCode);
                                newTeam.setGroup(groupName);
                                teamService.addTeam(newTeam);
                                
                                Team addedTeam = teamService.getTeamByName(teamName);
                                if (addedTeam != null) {
                                    teamService.assignTeamToGroup(addedTeam.getId(), groupName);
                                }
                            } else if (existingTeam.getGroup() == null || existingTeam.getGroup().isEmpty()) {
                                teamService.assignTeamToGroup(existingTeam.getId(), groupName);
                            }
                        } catch (SQLException e) {
                            System.err.println("Error importing team " + teamName + ": " + e.getMessage());
                        }
                    }
                    
                    // Now fetch matches
                    updateMessage("Fetching match schedule...");
                    List<Map<String, Object>> apiMatches = apiService.fetchAllGroupStageMatches();
                    
                    if (apiMatches.isEmpty()) {
                        errorMessage = "No matches received from API";
                        return null;
                    }
                    
                    // Clear existing matches
                    matchService.clearAllMatches();
                    
                    // Import each match
                    int matchNum = 1;
                    for (Map<String, Object> apiMatch : apiMatches) {
                        try {
                            String team1Name = (String) apiMatch.get("team1Name");
                            String team2Name = (String) apiMatch.get("team2Name");
                            String group = (String) apiMatch.get("group");
                            
                            if ("TBD".equals(team1Name) || "TBD".equals(team2Name) || group == null) {
                                continue;
                            }
                            
                            Team team1 = teamService.getTeamByName(team1Name);
                            Team team2 = teamService.getTeamByName(team2Name);
                            
                            if (team1 == null || team2 == null) {
                                System.err.println("Teams not found: " + team1Name + " vs " + team2Name);
                                continue;
                            }
                            
                            Match newMatch = new Match();
                            newMatch.setMatchNumber(matchNum++);
                            newMatch.setTeam1Id(team1.getId());
                            newMatch.setTeam2Id(team2.getId());
                            newMatch.setTeam1Score(0);
                            newMatch.setTeam2Score(0);
                            newMatch.setStage("GROUP");
                            newMatch.setGroup(group);
                            newMatch.setCompleted(false);
                            
                            matchService.addMatch(newMatch);
                            importedCount++;
                            
                        } catch (SQLException e) {
                            System.err.println("Error importing match: " + e.getMessage());
                        }
                    }
                } catch (Exception e) {
                    errorMessage = e.getMessage();
                    e.printStackTrace();
                }
                return null;
            }
            
            @Override
            protected void succeeded() {
                if (btnImportFromAPI != null) {
                    btnImportFromAPI.setDisable(false);
                }
                
                if (errorMessage != null) {
                    lblStatus.setText("API Error: " + errorMessage);
                    lblStatus.setStyle("-fx-text-fill: red;");
                    showAlert("API Error", "Failed to load schedule: " + errorMessage, Alert.AlertType.ERROR);
                } else {
                    loadMatches();
                    lblStatus.setText("Successfully imported " + importedCount + " matches from FIFA 2022!");
                    lblStatus.setStyle("-fx-text-fill: green;");
                    showAlert("Success", "Imported " + importedCount + " group stage matches from FIFA World Cup 2022!", Alert.AlertType.INFORMATION);
                }
            }
            
            @Override
            protected void failed() {
                if (btnImportFromAPI != null) {
                    btnImportFromAPI.setDisable(false);
                }
                lblStatus.setText("Failed to load matches from API");
                lblStatus.setStyle("-fx-text-fill: red;");
                showAlert("Error", "Failed to connect to API: " + getException().getMessage(), Alert.AlertType.ERROR);
            }
        };
        
        new Thread(task).start();
    }

    @FXML
    public void handleImportSchedule() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Import Schedule from JSON");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON Files", "*.json"));
        
        // Set initial directory to the json folder if it exists
        File jsonDir = new File("json");
        if (jsonDir.exists() && jsonDir.isDirectory()) {
            fileChooser.setInitialDirectory(jsonDir);
        }
        
        Stage stage = (Stage) btnRefresh.getScene().getWindow();
        File file = fileChooser.showOpenDialog(stage);
        
        if (file != null) {
            // Direct JDBC import - bypasses all service layer try-with-resources issues
            Connection conn = null;
            Statement stmt = null;
            PreparedStatement pstmt = null;
            Reader reader = null;
            
            try {
                DatabaseManager dbManager = DatabaseManager.getInstance();
                conn = dbManager.getConnection();
                
                // Clear existing matches AND teams for clean import
                stmt = conn.createStatement();
                stmt.executeUpdate("DELETE FROM matches");
                stmt.executeUpdate("DELETE FROM teams");
                stmt.close();
                stmt = null;
                
                // Read JSON file - use JsonArray to avoid LocalDateTime issues
                reader = new FileReader(file);
                com.google.gson.JsonArray jsonArray = com.google.gson.JsonParser.parseReader(reader).getAsJsonArray();
                reader.close();
                reader = null;
                
                // First pass: collect all unique teams from the matches
                java.util.Map<Integer, String[]> teamsFromMatches = new java.util.HashMap<>();
                java.util.Map<Integer, String> teamGroups = new java.util.HashMap<>();
                
                for (com.google.gson.JsonElement element : jsonArray) {
                    com.google.gson.JsonObject obj = element.getAsJsonObject();
                    int team1Id = obj.get("team1Id").getAsInt();
                    int team2Id = obj.get("team2Id").getAsInt();
                    String team1Name = obj.get("team1Name").getAsString();
                    String team2Name = obj.get("team2Name").getAsString();
                    String group = obj.has("group") && !obj.get("group").isJsonNull() ? obj.get("group").getAsString() : null;
                    
                    if (!teamsFromMatches.containsKey(team1Id)) {
                        teamsFromMatches.put(team1Id, new String[]{team1Name, team1Name.substring(0, Math.min(3, team1Name.length())).toUpperCase()});
                        if (group != null) teamGroups.put(team1Id, group);
                    }
                    if (!teamsFromMatches.containsKey(team2Id)) {
                        teamsFromMatches.put(team2Id, new String[]{team2Name, team2Name.substring(0, Math.min(3, team2Name.length())).toUpperCase()});
                        if (group != null) teamGroups.put(team2Id, group);
                    }
                }
                
                // Insert teams with their IDs from JSON
                String teamSql = "INSERT INTO teams (id, name, code, group_name) VALUES (?, ?, ?, ?)";
                pstmt = conn.prepareStatement(teamSql);
                for (java.util.Map.Entry<Integer, String[]> entry : teamsFromMatches.entrySet()) {
                    pstmt.setInt(1, entry.getKey());
                    pstmt.setString(2, entry.getValue()[0]);
                    pstmt.setString(3, entry.getValue()[1]);
                    pstmt.setString(4, teamGroups.get(entry.getKey()));
                    pstmt.executeUpdate();
                }
                pstmt.close();
                pstmt = null;
                
                // Now insert matches with IDs from JSON
                reader = new FileReader(file);
                jsonArray = com.google.gson.JsonParser.parseReader(reader).getAsJsonArray();
                reader.close();
                reader = null;
                
                String sql = "INSERT INTO matches (id, team1_id, team2_id, team1_score, team2_score, stage, group_name, " +
                             "match_date, match_number, completed) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                pstmt = conn.prepareStatement(sql);
                
                int count = 0;
                for (com.google.gson.JsonElement element : jsonArray) {
                    com.google.gson.JsonObject obj = element.getAsJsonObject();
                    pstmt.setInt(1, obj.get("id").getAsInt());
                    pstmt.setInt(2, obj.get("team1Id").getAsInt());
                    pstmt.setInt(3, obj.get("team2Id").getAsInt());
                    pstmt.setInt(4, obj.get("team1Score").getAsInt());
                    pstmt.setInt(5, obj.get("team2Score").getAsInt());
                    pstmt.setString(6, obj.get("stage").getAsString());
                    pstmt.setString(7, obj.has("group") && !obj.get("group").isJsonNull() ? obj.get("group").getAsString() : null);
                    pstmt.setString(8, null);
                    pstmt.setInt(9, obj.get("matchNumber").getAsInt());
                    pstmt.setInt(10, obj.get("completed").getAsBoolean() ? 1 : 0);
                    pstmt.executeUpdate();
                    count++;
                }
                
                pstmt.close();
                pstmt = null;
                dbManager.releaseConnection(conn);
                conn = null;
                
                // Recalculate all standings based on imported match results
                try {
                    standingsService.recalculateAllStandings();
                } catch (SQLException e) {
                    System.err.println("Error recalculating standings: " + e.getMessage());
                }
                
                loadMatches();
                lblStatus.setText(count + " matches imported. Standings recalculated!");
                lblStatus.setStyle("-fx-text-fill: green;");
                
            } catch (Exception e) {
                showAlert("Import Error", "Failed to import schedule: " + e.getMessage(), Alert.AlertType.ERROR);
                lblStatus.setText("Error: " + e.getMessage());
                lblStatus.setStyle("-fx-text-fill: red;");
            } finally {
                if (reader != null) { try { reader.close(); } catch (Exception e) {} }
                if (stmt != null) { try { stmt.close(); } catch (Exception e) {} }
                if (pstmt != null) { try { pstmt.close(); } catch (Exception e) {} }
                if (conn != null) { try { DatabaseManager.getInstance().releaseConnection(conn); } catch (Exception e) {} }
            }
        }
    }

    @FXML
    public void handleRefresh() {
        loadMatches();
        lblStatus.setText("Schedule refreshed!");
        lblStatus.setStyle("-fx-text-fill: blue;");
    }

    @FXML
    public void handleBack(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/home.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene currentScene = stage.getScene();
            Scene scene = new Scene(root, currentScene.getWidth(), currentScene.getHeight());
            stage.setScene(scene);
            stage.setTitle("FIFA World Cup - Home");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void handleFilterChange() {
        loadMatches();
    }

    private void loadMatches() {
        try {
            matchesList.clear();
            
            String filter = cmbFilterGroup != null ? cmbFilterGroup.getValue() : "All";
            List<Match> matches;
            
            if (filter == null || "All".equals(filter)) {
                matches = matchService.getAllMatches();
            } else {
                matches = matchService.getMatchesByGroup(filter);
            }
            
            matchesList.addAll(matches);
            
            if (lblTotalMatches != null) {
                lblTotalMatches.setText(String.valueOf(matches.size()));
            }
            
        } catch (SQLException e) {
            showAlert("Database Error", "Failed to load matches: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
