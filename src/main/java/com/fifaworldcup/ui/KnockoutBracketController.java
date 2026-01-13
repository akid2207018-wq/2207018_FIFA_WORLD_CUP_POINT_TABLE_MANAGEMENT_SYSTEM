package com.fifaworldcup.ui;

import com.fifaworldcup.model.KnockoutMatch;
import com.fifaworldcup.model.Team;
import com.fifaworldcup.database.DatabaseManager;
import com.fifaworldcup.service.KnockoutService;
import com.fifaworldcup.service.StandingsService;
import com.fifaworldcup.service.Fifa2022ApiService;
import com.fifaworldcup.service.TeamService;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.fxml.FXML;
import javafx.concurrent.Task;

import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.Map;

/**
 * Knockout Bracket Controller
 * 
 * API/DATA FLOW:
 * 1. GENERATE BRACKET:
 *    - Prerequisite: All group stage matches must be completed
 *    - Step 1: Determine qualified teams (top 2 from each group)
 *      * Calls: standingsService.getQualifiedTeams()
 *      * SQL: SELECT * FROM teams WHERE qualified = 1 ORDER BY group_name, points DESC
 *      * Qualification: Calls standingsService.markQualifiedTeams()
 *      * Logic: Top 2 teams per group based on: points > goal_difference > goals_for
 *    
 *    - Step 2: Create Round of 16 matchups
 *      * Calls: knockoutService.generateRoundOf16()
 *      * Pairing: Group winners vs Group runners-up from different groups
 *      * Example: Winner of A vs Runner-up of B, Winner of C vs Runner-up of D
 *      * SQL: INSERT INTO knockout_matches (round, team1_id, team2_id, bracket_position, match_number)
 *    
 *    - Step 3: Create subsequent rounds (Quarter, Semi, Final)
 *      * Creates placeholder matches with NULL teams
 *      * Winners advance automatically when match is updated
 * 
 * 2. UPDATE MATCH:
 *    - User clicks on match box
 *    - Enters scores for both teams
 *    - Calls: knockoutService.updateKnockoutMatch(matchId, team1Score, team2Score)
 *    - SQL: UPDATE knockout_matches SET team1_score = ?, team2_score = ?, winner_id = ?, completed = 1
 *    - Determines winner and advances to next round automatically
 * 
 * 3. LOAD BRACKET:
 *    - Calls: knockoutService.getKnockoutMatchesByRound(round)
 *    - SQL: SELECT km.*, t1.name as team1_name, t2.name as team2_name, w.name as winner_name
 *           FROM knockout_matches km
 *           LEFT JOIN teams t1 ON km.team1_id = t1.id
 *           LEFT JOIN teams t2 ON km.team2_id = t2.id
 *           LEFT JOIN teams w ON km.winner_id = w.id
 *           WHERE km.round = ?
 *    - Displays: Visual bracket with match results and advancement paths
 * 
 * 4. TEAM SOURCE:
 *    - Teams from: Top 2 teams from each of 8 groups (16 teams total)
 *    - Based on: Completed group stage matches and calculated standings
 *    - Progression: Round of 16 (16→8) → Quarter-Finals (8→4) → Semi-Finals (4→2) → Final (2→1)
 */

public class KnockoutBracketController {
    @FXML
    private ScrollPane scrollPane;
    
    @FXML
    private GridPane bracketGrid;
    
    @FXML
    private ComboBox<String> cmbSelectRound;
    
    @FXML
    private Button btnRefresh;
    
    @FXML
    private Button btnBack;
    
    @FXML
    private VBox vboxRound16;
    
    @FXML
    private VBox vboxQuarter;
    
    @FXML
    private VBox vboxSemi;
    
    @FXML
    private VBox vboxFinal;
    
    @FXML
    private Label lblChampion;
    
    @FXML
    private Label lblStatus;
    
    @FXML
    private Button btnImportFromApi;
    
    @FXML
    private Button btnImportFromJson;

    private KnockoutService knockoutService;
    private StandingsService standingsService;
    private TeamService teamService;
    private Fifa2022ApiService apiService;

    public KnockoutBracketController() {
        this.knockoutService = new KnockoutService();
        this.standingsService = new StandingsService();
        this.teamService = new TeamService();
        this.apiService = new Fifa2022ApiService();
    }

    @FXML
    public void initialize() {
        if (cmbSelectRound != null) {
            cmbSelectRound.getItems().addAll("All", "Round of 16", "Quarter-Finals", "Semi-Finals", "Final");
            cmbSelectRound.setValue("All");
        }
        // Auto-generate bracket if not exists and group stage is complete
        autoGenerateBracketIfReady();
        loadBracket();
    }
    
    /**
     * AUTO-GENERATE BRACKET:
     * Automatically generates the knockout bracket when all group stage matches are completed.
     * Teams are fetched from standings based on group stage results.
     * - Round of 16: Top 2 teams from each group (16 teams total)
     * - Pairing: 1A vs 2B, 1C vs 2D, 1E vs 2F, 1G vs 2H, 1B vs 2A, 1D vs 2C, 1F vs 2E, 1H vs 2G
     */
    private void autoGenerateBracketIfReady() {
        try {
            List<KnockoutMatch> existing = knockoutService.getAllKnockoutMatches();
            if (existing.isEmpty()) {
                // Try to generate bracket automatically
                knockoutService.generateRoundOf16Bracket();
                lblStatus.setText("Knockout bracket auto-generated from group standings!");
                lblStatus.setStyle("-fx-text-fill: green;");
            }
        } catch (SQLException e) {
            // Bracket generation may fail if group stage not complete - that's OK
            lblStatus.setText("Complete all group matches to generate knockout bracket.");
            lblStatus.setStyle("-fx-text-fill: #666;");
        }
    }

    public void setKnockoutService(KnockoutService knockoutService) {
        this.knockoutService = knockoutService;
    }

    @FXML
    public void handleRefresh() {
        loadBracket();
        lblStatus.setText("Bracket refreshed!");
        lblStatus.setStyle("-fx-text-fill: blue;");
    }
    
    /**
     * Import knockout matches from a JSON file
     */
    @FXML
    public void handleImportFromJson() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Import Knockout Matches from JSON");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON Files", "*.json"));
        
        // Set initial directory to json folder if exists
        File jsonDir = new File("json");
        if (jsonDir.exists() && jsonDir.isDirectory()) {
            fileChooser.setInitialDirectory(jsonDir);
        }
        
        Stage stage = (Stage) btnRefresh.getScene().getWindow();
        File file = fileChooser.showOpenDialog(stage);
        
        if (file != null) {
            Connection conn = null;
            Statement stmt = null;
            PreparedStatement pstmt = null;
            Reader reader = null;
            
            try {
                DatabaseManager dbManager = DatabaseManager.getInstance();
                conn = dbManager.getConnection();
                
                // Clear existing knockout matches
                stmt = conn.createStatement();
                stmt.executeUpdate("DELETE FROM knockout_matches");
                stmt.close();
                stmt = null;
                
                // Read JSON file
                reader = new FileReader(file);
                com.google.gson.JsonArray jsonArray = com.google.gson.JsonParser.parseReader(reader).getAsJsonArray();
                reader.close();
                reader = null;
                
                // Insert knockout matches from JSON
                String sql = "INSERT INTO knockout_matches (id, round, team1_id, team2_id, team1_score, team2_score, " +
                             "winner_id, completed, bracket_position, match_number) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                pstmt = conn.prepareStatement(sql);
                
                int count = 0;
                for (com.google.gson.JsonElement element : jsonArray) {
                    com.google.gson.JsonObject obj = element.getAsJsonObject();
                    
                    // Get stage/round from JSON
                    String stage_str = obj.has("stage") ? obj.get("stage").getAsString() : "ROUND_OF_16";
                    String round = convertStageToDbRound(stage_str);
                    
                    // Skip GROUP stage matches - only import knockout matches
                    if ("GROUP".equals(stage_str)) {
                        continue;
                    }
                    
                    int id = obj.get("id").getAsInt();
                    int team1Id = obj.get("team1Id").getAsInt();
                    int team2Id = obj.get("team2Id").getAsInt();
                    int team1Score = obj.get("team1Score").getAsInt();
                    int team2Score = obj.get("team2Score").getAsInt();
                    boolean completed = obj.get("completed").getAsBoolean();
                    int matchNumber = obj.has("matchNumber") ? obj.get("matchNumber").getAsInt() : id;
                    
                    // Determine winner
                    int winnerId = 0;
                    if (completed) {
                        if (team1Score > team2Score) {
                            winnerId = team1Id;
                        } else if (team2Score > team1Score) {
                            winnerId = team2Id;
                        } else {
                            // Penalty shootout - check penalty scores
                            if (obj.has("team1PenaltyScore") && obj.has("team2PenaltyScore")) {
                                int pen1 = obj.get("team1PenaltyScore").getAsInt();
                                int pen2 = obj.get("team2PenaltyScore").getAsInt();
                                winnerId = pen1 > pen2 ? team1Id : team2Id;
                            } else {
                                winnerId = team1Id; // Default
                            }
                        }
                    }
                    
                    pstmt.setInt(1, id);
                    pstmt.setString(2, round);
                    pstmt.setInt(3, team1Id);
                    pstmt.setInt(4, team2Id);
                    pstmt.setInt(5, team1Score);
                    pstmt.setInt(6, team2Score);
                    pstmt.setInt(7, winnerId);
                    pstmt.setInt(8, completed ? 1 : 0);
                    pstmt.setString(9, String.valueOf(count + 1)); // bracket position as string
                    pstmt.setInt(10, matchNumber);
                    pstmt.executeUpdate();
                    count++;
                }
                
                pstmt.close();
                pstmt = null;
                dbManager.releaseConnection(conn);
                conn = null;
                
                loadBracket();
                lblStatus.setText(count + " knockout matches imported from " + file.getName());
                lblStatus.setStyle("-fx-text-fill: green;");
                
            } catch (Exception e) {
                showAlert("Import Error", "Failed to import knockout matches: " + e.getMessage(), Alert.AlertType.ERROR);
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
    
    private String convertStageToDbRound(String stage) {
        if (stage == null) return "ROUND_16";
        switch (stage) {
            case "ROUND_OF_16": return "ROUND_16";
            case "QUARTER_FINAL": return "QUARTER";
            case "SEMI_FINAL": return "SEMI";
            case "THIRD_PLACE": return "THIRD_PLACE";
            case "FINAL": return "FINAL";
            default: return "ROUND_16";
        }
    }
    
    /**
     * Import knockout stage results from FIFA 2022 API
     * This will load Round of 16, Quarter-finals, Semi-finals, and Final results
     */
    @FXML
    public void handleImportFromApi() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Import from API");
        confirm.setHeaderText("Load FIFA 2022 Knockout Stage");
        confirm.setContentText("This will import knockout stage matches and results from FIFA World Cup 2022.\n" +
                             "This may take a moment. Continue?");
        
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }
        
        // Disable button during loading
        if (btnImportFromApi != null) {
            btnImportFromApi.setDisable(true);
        }
        lblStatus.setText("Loading knockout matches from FIFA 2022 API...");
        lblStatus.setStyle("-fx-text-fill: blue;");
        
        // Run API call in background thread
        Task<Void> task = new Task<Void>() {
            private int importedCount = 0;
            private int updatedCount = 0;
            private String errorMessage = null;
            
            @Override
            protected Void call() throws Exception {
                try {
                    // Fetch all knockout stage matches from API
                    updateMessage("Fetching knockout stage data from API...");
                    List<Map<String, Object>> apiMatches = apiService.fetchKnockoutStageMatches();
                    
                    if (apiMatches.isEmpty()) {
                        errorMessage = "No knockout matches received from API";
                        return null;
                    }
                    
                    updateMessage("Processing " + apiMatches.size() + " matches...");
                    
                    // Process each knockout match
                    for (Map<String, Object> apiMatch : apiMatches) {
                        try {
                            String team1Name = (String) apiMatch.get("team1Name");
                            String team2Name = (String) apiMatch.get("team2Name");
                            int team1Score = (Integer) apiMatch.get("team1Score");
                            int team2Score = (Integer) apiMatch.get("team2Score");
                            String stage = (String) apiMatch.get("stage");
                            boolean completed = (Boolean) apiMatch.get("completed");
                            
                            // Skip if teams are TBD
                            if ("TBD".equals(team1Name) || "TBD".equals(team2Name)) {
                                continue;
                            }
                            
                            // Get team IDs from database
                            Team team1 = teamService.getTeamByName(team1Name);
                            Team team2 = teamService.getTeamByName(team2Name);
                            
                            if (team1 == null || team2 == null) {
                                System.err.println("Teams not found: " + team1Name + " vs " + team2Name);
                                continue;
                            }
                            
                            // Convert stage to round format
                            String round = convertStageToRound(stage);
                            if (round == null) {
                                continue;
                            }
                            
                            // Check if knockout match exists
                            KnockoutMatch existingMatch = knockoutService.getKnockoutMatchByTeams(team1.getId(), team2.getId());
                            
                            int winnerId = 0;
                            if (completed) {
                                if (team1Score > team2Score) {
                                    winnerId = team1.getId();
                                } else if (team2Score > team1Score) {
                                    winnerId = team2.getId();
                                } else {
                                    // Tie - determine winner from next round or assume team1 won on penalties
                                    winnerId = team1.getId();
                                }
                            }
                            
                            if (existingMatch != null && completed) {
                                // Update existing knockout match result
                                knockoutService.updateKnockoutMatchResult(existingMatch.getId(), team1Score, team2Score, winnerId);
                                updatedCount++;
                            } else if (existingMatch == null) {
                                // Create new knockout match
                                KnockoutMatch newMatch = new KnockoutMatch();
                                newMatch.setTeam1Id(team1.getId());
                                newMatch.setTeam2Id(team2.getId());
                                newMatch.setTeam1Score(team1Score);
                                newMatch.setTeam2Score(team2Score);
                                newMatch.setRound(round);
                                newMatch.setCompleted(completed);
                                if (completed) {
                                    newMatch.setWinnerId(winnerId);
                                }
                                
                                knockoutService.addKnockoutMatch(newMatch);
                                importedCount++;
                            }
                            
                            updateProgress(importedCount + updatedCount, apiMatches.size());
                            
                        } catch (SQLException e) {
                            System.err.println("Error importing knockout match: " + e.getMessage());
                        }
                    }
                } catch (Exception e) {
                    errorMessage = e.getMessage();
                    e.printStackTrace();
                }
                return null;
            }
            
            private String convertStageToRound(String stage) {
                if (stage == null) return null;
                
                if (stage.contains("Round of 16")) {
                    return "ROUND_16";
                } else if (stage.contains("Quarter")) {
                    return "QUARTER";
                } else if (stage.contains("Semi")) {
                    return "SEMI";
                } else if (stage.contains("Final") && !stage.contains("Semi")) {
                    return "FINAL";
                }
                return null;
            }
            
            @Override
            protected void succeeded() {
                if (btnImportFromApi != null) {
                    btnImportFromApi.setDisable(false);
                }
                
                if (errorMessage != null) {
                    lblStatus.setText("API Error: " + errorMessage);
                    lblStatus.setStyle("-fx-text-fill: red;");
                    showAlert("API Error", "Failed to load knockout matches: " + errorMessage, Alert.AlertType.ERROR);
                } else {
                    loadBracket();
                    lblStatus.setText("Successfully imported/updated " + (importedCount + updatedCount) + " knockout matches!");
                    lblStatus.setStyle("-fx-text-fill: green;");
                    showAlert("Success", 
                        "Imported " + importedCount + " new matches and updated " + updatedCount + 
                        " existing matches from FIFA World Cup 2022 knockout stage!", 
                        Alert.AlertType.INFORMATION);
                }
            }
            
            @Override
            protected void failed() {
                if (btnImportFromApi != null) {
                    btnImportFromApi.setDisable(false);
                }
                lblStatus.setText("Failed to load knockout matches from API");
                lblStatus.setStyle("-fx-text-fill: red;");
                showAlert("Error", "Failed to connect to API: " + getException().getMessage(), Alert.AlertType.ERROR);
            }
        };
        
        new Thread(task).start();
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
    public void handleRoundChange() {
        loadBracket();
    }

    private void loadBracket() {
        try {
            String selectedRound = cmbSelectRound != null ? cmbSelectRound.getValue() : "All";
            
            // Clear existing content (keep headers)
            clearVBoxContent(vboxRound16);
            clearVBoxContent(vboxQuarter);
            clearVBoxContent(vboxSemi);
            clearVBoxContent(vboxFinal);
            
            if ("All".equals(selectedRound) || "Round of 16".equals(selectedRound)) {
                displayRound16();
            }
            if ("All".equals(selectedRound) || "Quarter-Finals".equals(selectedRound)) {
                displayQuarterFinals();
            }
            if ("All".equals(selectedRound) || "Semi-Finals".equals(selectedRound)) {
                displaySemiFinals();
            }
            if ("All".equals(selectedRound) || "Final".equals(selectedRound)) {
                displayFinal();
            }
            
            // Check for champion
            Team champion = knockoutService.getChampion();
            if (champion != null && lblChampion != null) {
                lblChampion.setText("🏆 CHAMPION: " + champion.getName() + " 🏆");
                lblChampion.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: gold;");
            }
            
        } catch (SQLException e) {
            showAlert("Error", "Failed to load bracket: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void clearVBoxContent(VBox vbox) {
        if (vbox != null && vbox.getChildren().size() > 1) {
            // Keep the first child (header label)
            Node header = vbox.getChildren().get(0);
            vbox.getChildren().clear();
            vbox.getChildren().add(header);
        }
    }

    private void displayRound16() throws SQLException {
        if (vboxRound16 == null) return;
        
        List<KnockoutMatch> matches = knockoutService.getKnockoutMatchesByRound("ROUND_16");
        for (KnockoutMatch match : matches) {
            vboxRound16.getChildren().add(createMatchCard(match));
        }
    }

    private void displayQuarterFinals() throws SQLException {
        if (vboxQuarter == null) return;
        
        List<KnockoutMatch> matches = knockoutService.getKnockoutMatchesByRound("QUARTER");
        for (KnockoutMatch match : matches) {
            vboxQuarter.getChildren().add(createMatchCard(match));
        }
    }

    private void displaySemiFinals() throws SQLException {
        if (vboxSemi == null) return;
        
        List<KnockoutMatch> matches = knockoutService.getKnockoutMatchesByRound("SEMI");
        for (KnockoutMatch match : matches) {
            vboxSemi.getChildren().add(createMatchCard(match));
        }
    }

    private void displayFinal() throws SQLException {
        if (vboxFinal == null) return;
        
        List<KnockoutMatch> matches = knockoutService.getKnockoutMatchesByRound("FINAL");
        for (KnockoutMatch match : matches) {
            vboxFinal.getChildren().add(createMatchCard(match));
        }
    }

    private VBox createMatchCard(KnockoutMatch match) {
        VBox card = new VBox(5);
        card.setStyle("-fx-background-color: white; -fx-border-color: #2a5298; -fx-border-radius: 5; " +
                     "-fx-background-radius: 5; -fx-padding: 10;");
        card.setPrefWidth(180);
        
        // Match number
        Label lblMatchNum = new Label("Match " + match.getMatchNumber());
        lblMatchNum.setStyle("-fx-font-size: 10px; -fx-text-fill: #666;");
        
        // Team 1
        HBox team1Box = new HBox(5);
        team1Box.setAlignment(Pos.CENTER_LEFT);
        Label lblTeam1 = new Label(match.getTeam1Name());
        lblTeam1.setStyle("-fx-font-weight: bold;");
        Label lblScore1 = new Label(match.isCompleted() ? String.valueOf(match.getTeam1Score()) : "-");
        team1Box.getChildren().addAll(lblTeam1, new Region(), lblScore1);
        HBox.setHgrow(team1Box.getChildren().get(1), Priority.ALWAYS);
        
        // VS label
        Label lblVs = new Label("vs");
        lblVs.setStyle("-fx-text-fill: #999;");
        lblVs.setAlignment(Pos.CENTER);
        
        // Team 2
        HBox team2Box = new HBox(5);
        team2Box.setAlignment(Pos.CENTER_LEFT);
        Label lblTeam2 = new Label(match.getTeam2Name());
        lblTeam2.setStyle("-fx-font-weight: bold;");
        Label lblScore2 = new Label(match.isCompleted() ? String.valueOf(match.getTeam2Score()) : "-");
        team2Box.getChildren().addAll(lblTeam2, new Region(), lblScore2);
        HBox.setHgrow(team2Box.getChildren().get(1), Priority.ALWAYS);
        
        // Highlight winner
        if (match.isCompleted() && match.getWinnerId() > 0) {
            if (match.getWinnerId() == match.getTeam1Id()) {
                lblTeam1.setStyle("-fx-font-weight: bold; -fx-text-fill: green;");
            } else {
                lblTeam2.setStyle("-fx-font-weight: bold; -fx-text-fill: green;");
            }
        }
        
        // Enter result button (if not completed and both teams assigned)
        if (!match.isCompleted() && match.getTeam1Id() > 0 && match.getTeam2Id() > 0) {
            Button btnEnterResult = new Button("Enter Result");
            btnEnterResult.setStyle("-fx-background-color: #2a5298; -fx-text-fill: white; -fx-font-size: 10px;");
            btnEnterResult.setOnAction(e -> showEnterResultDialog(match));
            card.getChildren().addAll(lblMatchNum, team1Box, lblVs, team2Box, btnEnterResult);
        } else {
            card.getChildren().addAll(lblMatchNum, team1Box, lblVs, team2Box);
        }
        
        // Status indicator
        if (match.isCompleted()) {
            Label lblComplete = new Label("✓ Completed");
            lblComplete.setStyle("-fx-text-fill: green; -fx-font-size: 10px;");
            card.getChildren().add(lblComplete);
        }
        
        return card;
    }

    private void showEnterResultDialog(KnockoutMatch match) {
        Dialog<int[]> dialog = new Dialog<>();
        dialog.setTitle("Enter Match Result");
        dialog.setHeaderText(match.getTeam1Name() + " vs " + match.getTeam2Name());
        
        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
        
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        
        TextField txtScore1 = new TextField("0");
        TextField txtScore2 = new TextField("0");
        
        grid.add(new Label(match.getTeam1Name() + " Score:"), 0, 0);
        grid.add(txtScore1, 1, 0);
        grid.add(new Label(match.getTeam2Name() + " Score:"), 0, 1);
        grid.add(txtScore2, 1, 1);
        
        // Winner selection for tie scenario
        ComboBox<String> cmbWinner = new ComboBox<>();
        cmbWinner.getItems().addAll(match.getTeam1Name(), match.getTeam2Name());
        cmbWinner.setPromptText("Select winner (if penalties)");
        grid.add(new Label("Winner (if tie):"), 0, 2);
        grid.add(cmbWinner, 1, 2);
        
        dialog.getDialogPane().setContent(grid);
        
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                try {
                    return new int[]{
                        Integer.parseInt(txtScore1.getText()),
                        Integer.parseInt(txtScore2.getText()),
                        cmbWinner.getSelectionModel().getSelectedIndex()
                    };
                } catch (NumberFormatException e) {
                    return null;
                }
            }
            return null;
        });
        
        Optional<int[]> result = dialog.showAndWait();
        result.ifPresent(scores -> {
            try {
                int score1 = scores[0];
                int score2 = scores[1];
                int winnerIdx = scores[2];
                
                int winnerId;
                if (score1 > score2) {
                    winnerId = match.getTeam1Id();
                } else if (score2 > score1) {
                    winnerId = match.getTeam2Id();
                } else {
                    // Tie - use selected winner (penalties)
                    winnerId = winnerIdx == 0 ? match.getTeam1Id() : match.getTeam2Id();
                }
                
                knockoutService.updateKnockoutMatchResult(match.getId(), score1, score2, winnerId);
                loadBracket();
                lblStatus.setText("Result saved!");
                lblStatus.setStyle("-fx-text-fill: green;");
                
            } catch (SQLException e) {
                showAlert("Error", "Failed to save result: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        });
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
