package com.fifaworldcup.ui;

import com.fifaworldcup.database.DatabaseManager;
import com.fifaworldcup.model.Match;
import com.fifaworldcup.service.MatchService;
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

import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

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
    private ObservableList<Match> matchesList;
    private static final DateTimeFormatter DISPLAY_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public MatchSchedulingController() {
        this.matchService = new MatchService();
        this.teamService = new TeamService();
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
                
                // Clear existing matches
                stmt = conn.createStatement();
                stmt.executeUpdate("DELETE FROM matches");
                stmt.executeUpdate("UPDATE teams SET played = 0, won = 0, drawn = 0, lost = 0, " +
                        "goals_for = 0, goals_against = 0, goal_difference = 0, points = 0, qualified = 0");
                stmt.close();
                stmt = null;
                
                // Read JSON file - use JsonArray to avoid LocalDateTime issues
                reader = new FileReader(file);
                com.google.gson.JsonArray jsonArray = com.google.gson.JsonParser.parseReader(reader).getAsJsonArray();
                reader.close();
                reader = null;
                
                // Insert each match directly
                String sql = "INSERT INTO matches (team1_id, team2_id, team1_score, team2_score, stage, group_name, " +
                             "match_date, match_number, completed) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
                pstmt = conn.prepareStatement(sql);
                
                int count = 0;
                for (com.google.gson.JsonElement element : jsonArray) {
                    com.google.gson.JsonObject obj = element.getAsJsonObject();
                    pstmt.setInt(1, obj.get("team1Id").getAsInt());
                    pstmt.setInt(2, obj.get("team2Id").getAsInt());
                    pstmt.setInt(3, obj.get("team1Score").getAsInt());
                    pstmt.setInt(4, obj.get("team2Score").getAsInt());
                    pstmt.setString(5, obj.get("stage").getAsString());
                    pstmt.setString(6, obj.get("group").getAsString());
                    pstmt.setString(7, null); // matchDate always null
                    pstmt.setInt(8, obj.get("matchNumber").getAsInt());
                    pstmt.setInt(9, obj.get("completed").getAsBoolean() ? 1 : 0);
                    pstmt.executeUpdate();
                    count++;
                }
                
                pstmt.close();
                pstmt = null;
                dbManager.releaseConnection(conn);
                conn = null;
                
                loadMatches();
                lblStatus.setText(count + " matches imported from " + file.getName());
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
