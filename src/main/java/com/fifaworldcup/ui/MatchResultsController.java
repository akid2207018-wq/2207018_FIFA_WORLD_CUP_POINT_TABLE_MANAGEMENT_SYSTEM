package com.fifaworldcup.ui;

import com.fifaworldcup.model.Match;
import com.fifaworldcup.service.MatchService;
import com.fifaworldcup.service.StandingsService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.sql.SQLException;
import java.util.List;

public class MatchResultsController {
    @FXML
    private TableView<Match> tblMatches;
    
    @FXML
    private TableColumn<Match, Integer> colMatchNumber;
    
    @FXML
    private TableColumn<Match, String> colTeam1;
    
    @FXML
    private TableColumn<Match, String> colTeam2;
    
    @FXML
    private TableColumn<Match, Integer> colTeam1Score;
    
    @FXML
    private TableColumn<Match, Integer> colTeam2Score;
    
    @FXML
    private TableColumn<Match, String> colGroup;
    
    @FXML
    private TableColumn<Match, String> colCompleted;
    
    @FXML
    private TextField txtTeam1Score;
    
    @FXML
    private TextField txtTeam2Score;
    
    @FXML
    private Label lblSelectedMatch;
    
    @FXML
    private Button btnUpdateResult;
    
    @FXML
    private Button btnClearResult;
    
    @FXML
    private Button btnRefresh;
    
    @FXML
    private Button btnBack;
    
    @FXML
    private ComboBox<String> cmbFilterStatus;
    
    @FXML
    private Label lblStatus;

    private MatchService matchService;
    private StandingsService standingsService;
    private ObservableList<Match> matchesList;
    private Match selectedMatch;

    public MatchResultsController() {
        this.matchService = new MatchService();
        this.standingsService = new StandingsService();
        this.matchesList = FXCollections.observableArrayList();
    }

    @FXML
    public void initialize() {
        if (cmbFilterStatus != null) {
            cmbFilterStatus.getItems().addAll("All", "Pending", "Completed");
            cmbFilterStatus.setValue("All");
        }
        
        // Setup table columns
        colMatchNumber.setCellValueFactory(new PropertyValueFactory<>("matchNumber"));
        colTeam1.setCellValueFactory(new PropertyValueFactory<>("team1Name"));
        colTeam2.setCellValueFactory(new PropertyValueFactory<>("team2Name"));
        colTeam1Score.setCellValueFactory(new PropertyValueFactory<>("team1Score"));
        colTeam2Score.setCellValueFactory(new PropertyValueFactory<>("team2Score"));
        colGroup.setCellValueFactory(new PropertyValueFactory<>("group"));
        colCompleted.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().isCompleted() ? "Yes" : "No"));
        
        tblMatches.setItems(matchesList);
        loadMatches();
    }

    public void setMatchService(MatchService matchService) {
        this.matchService = matchService;
    }

    @FXML
    public void handleUpdateResult() {
        if (selectedMatch == null) {
            showAlert("Selection Error", "Please select a match to update.", Alert.AlertType.WARNING);
            return;
        }
        
        if (!validateScores()) {
            return;
        }
        
        try {
            int team1Score = Integer.parseInt(txtTeam1Score.getText().trim());
            int team2Score = Integer.parseInt(txtTeam2Score.getText().trim());
            
            matchService.updateMatchResult(selectedMatch.getId(), team1Score, team2Score);
            
            // Update standings after match result
            standingsService.updateStandings(selectedMatch.getId());
            
            loadMatches();
            clearForm();
            
            lblStatus.setText("Match result updated! Standings recalculated.");
            lblStatus.setStyle("-fx-text-fill: green;");
            
        } catch (SQLException e) {
            showAlert("Database Error", "Failed to update result: " + e.getMessage(), Alert.AlertType.ERROR);
            lblStatus.setText("Error: " + e.getMessage());
            lblStatus.setStyle("-fx-text-fill: red;");
        }
    }

    @FXML
    public void handleClearResult() {
        if (selectedMatch == null) {
            showAlert("Selection Error", "Please select a match to clear.", Alert.AlertType.WARNING);
            return;
        }
        
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Clear");
        confirm.setHeaderText("Clear Match Result");
        confirm.setContentText("Are you sure you want to clear the result for this match?");
        
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try {
                matchService.clearMatchResult(selectedMatch.getId());
                
                // Recalculate standings for affected teams
                standingsService.recalculateTeamStats(selectedMatch.getTeam1Id());
                standingsService.recalculateTeamStats(selectedMatch.getTeam2Id());
                
                loadMatches();
                clearForm();
                
                lblStatus.setText("Match result cleared! Standings recalculated.");
                lblStatus.setStyle("-fx-text-fill: blue;");
                
            } catch (SQLException e) {
                showAlert("Database Error", "Failed to clear result: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }

    @FXML
    public void handleRefresh() {
        loadMatches();
        lblStatus.setText("Match list refreshed!");
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
    public void handleMatchSelection() {
        selectedMatch = tblMatches.getSelectionModel().getSelectedItem();
        if (selectedMatch != null) {
            lblSelectedMatch.setText("Selected: " + selectedMatch.getTeam1Name() + " vs " + selectedMatch.getTeam2Name());
            txtTeam1Score.setText(String.valueOf(selectedMatch.getTeam1Score()));
            txtTeam2Score.setText(String.valueOf(selectedMatch.getTeam2Score()));
        }
    }

    @FXML
    public void handleFilterChange() {
        loadMatches();
    }

    private void loadMatches() {
        try {
            matchesList.clear();
            
            String filter = cmbFilterStatus != null ? cmbFilterStatus.getValue() : "All";
            List<Match> matches;
            
            if ("Pending".equals(filter)) {
                matches = matchService.getPendingMatches();
            } else if ("Completed".equals(filter)) {
                matches = matchService.getCompletedMatches();
            } else {
                matches = matchService.getAllMatches();
            }
            
            matchesList.addAll(matches);
            
        } catch (SQLException e) {
            showAlert("Database Error", "Failed to load matches: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void clearForm() {
        txtTeam1Score.clear();
        txtTeam2Score.clear();
        lblSelectedMatch.setText("Select a match from the table below");
        selectedMatch = null;
        tblMatches.getSelectionModel().clearSelection();
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private boolean validateScores() {
        String score1Text = txtTeam1Score.getText().trim();
        String score2Text = txtTeam2Score.getText().trim();
        
        if (score1Text.isEmpty() || score2Text.isEmpty()) {
            showAlert("Validation Error", "Please enter both scores.", Alert.AlertType.WARNING);
            return false;
        }
        
        try {
            int score1 = Integer.parseInt(score1Text);
            int score2 = Integer.parseInt(score2Text);
            
            if (score1 < 0 || score2 < 0) {
                showAlert("Validation Error", "Scores cannot be negative.", Alert.AlertType.WARNING);
                return false;
            }
            
            if (score1 > 20 || score2 > 20) {
                showAlert("Validation Error", "Scores seem unrealistic (max 20).", Alert.AlertType.WARNING);
                return false;
            }
            
        } catch (NumberFormatException e) {
            showAlert("Validation Error", "Please enter valid numbers for scores.", Alert.AlertType.WARNING);
            return false;
        }
        
        return true;
    }
}
