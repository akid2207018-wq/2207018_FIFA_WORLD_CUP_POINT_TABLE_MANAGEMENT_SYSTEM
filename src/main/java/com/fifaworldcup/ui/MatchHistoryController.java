package com.fifaworldcup.ui;

import com.fifaworldcup.model.Match;
import com.fifaworldcup.model.Team;
import com.fifaworldcup.service.MatchService;
import com.fifaworldcup.service.TeamService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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
import java.io.FileWriter;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public class MatchHistoryController {
    @FXML
    private TableView<Match> tblMatchHistory;
    
    @FXML
    private TableColumn<Match, Integer> colMatchNumber;
    
    @FXML
    private TableColumn<Match, String> colStage;
    
    @FXML
    private TableColumn<Match, String> colGroup;
    
    @FXML
    private TableColumn<Match, String> colTeam1;
    
    @FXML
    private TableColumn<Match, Integer> colTeam1Score;
    
    @FXML
    private TableColumn<Match, Integer> colTeam2Score;
    
    @FXML
    private TableColumn<Match, String> colTeam2;
    
    @FXML
    private TableColumn<Match, String> colDate;
    
    @FXML
    private ComboBox<String> cmbFilterStage;
    
    @FXML
    private ComboBox<String> cmbFilterTeam;
    
    @FXML
    private Button btnRefresh;
    
    @FXML
    private Button btnExport;
    
    @FXML
    private Button btnClearFilters;
    
    @FXML
    private Button btnBack;
    
    @FXML
    private Label lblTotalMatches;
    
    @FXML
    private Label lblCompletedMatches;
    
    @FXML
    private Label lblStatus;

    private MatchService matchService;
    private TeamService teamService;
    private ObservableList<Match> matchesList;
    private List<Match> allMatches;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public MatchHistoryController() {
        this.matchService = new MatchService();
        this.teamService = new TeamService();
        this.matchesList = FXCollections.observableArrayList();
    }

    @FXML
    public void initialize() {
        if (cmbFilterStage != null) {
            cmbFilterStage.getItems().addAll("All", "GROUP", "ROUND_16", "QUARTER", "SEMI", "FINAL");
            cmbFilterStage.setValue("All");
        }
        
        // Setup table columns
        colMatchNumber.setCellValueFactory(new PropertyValueFactory<>("matchNumber"));
        colStage.setCellValueFactory(new PropertyValueFactory<>("stage"));
        colGroup.setCellValueFactory(new PropertyValueFactory<>("group"));
        colTeam1.setCellValueFactory(new PropertyValueFactory<>("team1Name"));
        colTeam1Score.setCellValueFactory(new PropertyValueFactory<>("team1Score"));
        colTeam2Score.setCellValueFactory(new PropertyValueFactory<>("team2Score"));
        colTeam2.setCellValueFactory(new PropertyValueFactory<>("team2Name"));
        colDate.setCellValueFactory(cellData -> {
            if (cellData.getValue().getMatchDate() != null) {
                return new SimpleStringProperty(cellData.getValue().getMatchDate().format(DATE_FORMATTER));
            }
            return new SimpleStringProperty("-");
        });
        
        // Color rows based on completion
        tblMatchHistory.setRowFactory(tv -> new TableRow<Match>() {
            @Override
            protected void updateItem(Match match, boolean empty) {
                super.updateItem(match, empty);
                if (match == null || empty) {
                    setStyle("");
                } else if (match.isCompleted()) {
                    setStyle("-fx-background-color: #e8f5e9;"); // Light green for completed
                } else {
                    setStyle("");
                }
            }
        });
        
        tblMatchHistory.setItems(matchesList);
        loadMatchHistory();
        loadTeamsForFilter();
    }

    public void setMatchService(MatchService matchService) {
        this.matchService = matchService;
    }

    @FXML
    public void handleRefresh() {
        loadMatchHistory();
        loadTeamsForFilter();
        lblStatus.setText("Match history refreshed!");
        lblStatus.setStyle("-fx-text-fill: blue;");
    }

    @FXML
    public void handleExport() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Match History to JSON");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON Files", "*.json"));
        fileChooser.setInitialFileName("match_history.json");
        
        Stage stage = (Stage) btnExport.getScene().getWindow();
        File file = fileChooser.showSaveDialog(stage);
        
        if (file != null) {
            try {
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                try (FileWriter writer = new FileWriter(file)) {
                    gson.toJson(matchesList, writer);
                }
                lblStatus.setText("Match history exported to " + file.getName());
                lblStatus.setStyle("-fx-text-fill: green;");
            } catch (Exception e) {
                showAlert("Export Error", "Failed to export: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }

    @FXML
    public void handleClearFilters() {
        if (cmbFilterStage != null) cmbFilterStage.setValue("All");
        if (cmbFilterTeam != null) cmbFilterTeam.setValue("All");
        applyFilters();
        lblStatus.setText("Filters cleared!");
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
        applyFilters();
    }

    private void loadMatchHistory() {
        try {
            allMatches = matchService.getAllMatches();
            applyFilters();
            updateStatistics();
        } catch (SQLException e) {
            showAlert("Database Error", "Failed to load match history: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void applyFilters() {
        if (allMatches == null) return;
        
        String stageFilter = cmbFilterStage != null ? cmbFilterStage.getValue() : "All";
        String teamFilter = cmbFilterTeam != null ? cmbFilterTeam.getValue() : "All";
        
        List<Match> filtered = allMatches.stream()
            .filter(m -> "All".equals(stageFilter) || stageFilter.equals(m.getStage()))
            .filter(m -> {
                if ("All".equals(teamFilter) || teamFilter == null) return true;
                return teamFilter.equals(m.getTeam1Name()) || teamFilter.equals(m.getTeam2Name());
            })
            .collect(Collectors.toList());
        
        matchesList.clear();
        matchesList.addAll(filtered);
        updateStatistics();
    }

    private void loadTeamsForFilter() {
        try {
            if (cmbFilterTeam != null) {
                cmbFilterTeam.getItems().clear();
                cmbFilterTeam.getItems().add("All");
                
                List<Team> teams = teamService.getAllTeams();
                for (Team team : teams) {
                    cmbFilterTeam.getItems().add(team.getName());
                }
                cmbFilterTeam.setValue("All");
            }
        } catch (SQLException e) {
            // Ignore - teams list is optional
        }
    }

    private void updateStatistics() {
        if (lblTotalMatches != null) {
            lblTotalMatches.setText(String.valueOf(matchesList.size()));
        }
        
        if (lblCompletedMatches != null) {
            long completed = matchesList.stream().filter(Match::isCompleted).count();
            lblCompletedMatches.setText(String.valueOf(completed));
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
