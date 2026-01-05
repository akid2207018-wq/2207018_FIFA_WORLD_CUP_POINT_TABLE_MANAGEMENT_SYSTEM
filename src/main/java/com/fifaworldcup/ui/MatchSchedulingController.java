package com.fifaworldcup.ui;

import com.fifaworldcup.model.Match;
import com.fifaworldcup.service.MatchService;
import com.fifaworldcup.service.TeamService;
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
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
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
    private Button btnGenerateSchedule;
    
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
    public void handleGenerateSchedule() {
        try {
            // Check if matches already exist
            int existingMatches = matchService.getTotalMatchCount();
            int completedMatches = matchService.getCompletedMatchCount();
            
            if (existingMatches > 0) {
                // If there are completed matches, don't allow regeneration
                if (completedMatches > 0) {
                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                    confirm.setTitle("Matches In Progress");
                    confirm.setHeaderText("Tournament Has Started");
                    confirm.setContentText("There are " + completedMatches + " completed matches out of " + existingMatches + " total.\n\n" +
                            "Choose an option:\n" +
                            "• OK = Clear ALL matches and start fresh\n" +
                            "• Cancel = Keep current matches");
                    
                    if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
                        lblStatus.setText("Schedule generation cancelled. " + existingMatches + " matches preserved.");
                        lblStatus.setStyle("-fx-text-fill: blue;");
                        return;
                    }
                    matchService.clearAllMatches();
                } else {
                    // All matches pending - ask to regenerate
                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                    confirm.setTitle("Confirm Regenerate");
                    confirm.setHeaderText("Schedule Already Exists");
                    confirm.setContentText("There are " + existingMatches + " pending matches. Regenerate schedule?");
                    
                    if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
                        return;
                    }
                    matchService.clearAllMatches();
                }
            }
            
            // Check if groups have teams
            boolean hasTeamsInGroups = false;
            int totalTeamsInGroups = 0;
            String[] groups = {"A", "B", "C", "D", "E", "F", "G", "H"};
            StringBuilder groupInfo = new StringBuilder();
            
            for (String group : groups) {
                int count = teamService.getTeamCountInGroup(group);
                if (count >= 2) {
                    hasTeamsInGroups = true;
                    totalTeamsInGroups += count;
                    groupInfo.append("Group ").append(group).append(": ").append(count).append(" teams\n");
                }
            }
            
            if (!hasTeamsInGroups) {
                showAlert("No Teams in Groups", 
                    "Please assign at least 2 teams to groups before generating schedule.\n\n" +
                    "How to fix:\n" +
                    "1. Go to 'Team Registration' and import teams from JSON\n" +
                    "2. Go to 'Group Formation' and assign teams to groups\n" +
                    "3. Come back here and click 'Generate Schedule'", 
                    Alert.AlertType.WARNING);
                return;
            }
            
            matchService.generateGroupStageMatches();
            loadMatches();
            
            int totalMatches = matchService.getTotalMatchCount();
            lblStatus.setText(totalMatches + " group stage matches generated!");
            lblStatus.setStyle("-fx-text-fill: green;");
            
        } catch (SQLException e) {
            showAlert("Database Error", "Failed to generate schedule: " + e.getMessage(), Alert.AlertType.ERROR);
            lblStatus.setText("Error: " + e.getMessage());
            lblStatus.setStyle("-fx-text-fill: red;");
        }
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
