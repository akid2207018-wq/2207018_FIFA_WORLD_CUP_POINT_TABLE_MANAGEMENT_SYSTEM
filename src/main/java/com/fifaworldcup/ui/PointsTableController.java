package com.fifaworldcup.ui;

import com.fifaworldcup.model.Team;
import com.fifaworldcup.service.StandingsService;
import com.fifaworldcup.service.TeamService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import javafx.beans.property.SimpleIntegerProperty;
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
import java.util.List;

public class PointsTableController {
    @FXML
    private TableView<Team> tblStandings;
    
    @FXML
    private TableColumn<Team, Integer> colPosition;
    
    @FXML
    private TableColumn<Team, String> colTeamName;
    
    @FXML
    private TableColumn<Team, Integer> colPlayed;
    
    @FXML
    private TableColumn<Team, Integer> colWon;
    
    @FXML
    private TableColumn<Team, Integer> colDrawn;
    
    @FXML
    private TableColumn<Team, Integer> colLost;
    
    @FXML
    private TableColumn<Team, Integer> colGF;
    
    @FXML
    private TableColumn<Team, Integer> colGA;
    
    @FXML
    private TableColumn<Team, Integer> colGD;
    
    @FXML
    private TableColumn<Team, Integer> colPoints;
    
    @FXML
    private ComboBox<String> cmbSelectGroup;
    
    @FXML
    private Button btnRefresh;
    
    @FXML
    private Button btnDetermineQualifiers;
    
    @FXML
    private Button btnExport;
    
    @FXML
    private Button btnBack;
    
    @FXML
    private Label lblGroupTitle;
    
    @FXML
    private Label lblStatus;

    private StandingsService standingsService;
    private TeamService teamService;
    private ObservableList<Team> standingsList;
    private String currentGroup = "A";

    public PointsTableController() {
        this.standingsService = new StandingsService();
        this.teamService = new TeamService();
        this.standingsList = FXCollections.observableArrayList();
    }

    @FXML
    public void initialize() {
        if (cmbSelectGroup != null) {
            cmbSelectGroup.getItems().addAll("A", "B", "C", "D", "E", "F", "G", "H");
            cmbSelectGroup.setValue("A");
        }
        
        // Setup table columns
        colPosition.setCellValueFactory(cellData -> {
            int index = standingsList.indexOf(cellData.getValue()) + 1;
            return new SimpleIntegerProperty(index).asObject();
        });
        colTeamName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colPlayed.setCellValueFactory(new PropertyValueFactory<>("played"));
        colWon.setCellValueFactory(new PropertyValueFactory<>("won"));
        colDrawn.setCellValueFactory(new PropertyValueFactory<>("drawn"));
        colLost.setCellValueFactory(new PropertyValueFactory<>("lost"));
        colGF.setCellValueFactory(new PropertyValueFactory<>("goalsFor"));
        colGA.setCellValueFactory(new PropertyValueFactory<>("goalsAgainst"));
        colGD.setCellValueFactory(new PropertyValueFactory<>("goalDifference"));
        colPoints.setCellValueFactory(new PropertyValueFactory<>("points"));
        
        // Highlight qualified teams (top 2)
        tblStandings.setRowFactory(tv -> new TableRow<Team>() {
            @Override
            protected void updateItem(Team team, boolean empty) {
                super.updateItem(team, empty);
                if (team == null || empty) {
                    setStyle("");
                } else {
                    int position = standingsList.indexOf(team) + 1;
                    if (position <= 2) {
                        setStyle("-fx-background-color: #d4edda;"); // Light green for qualifiers
                    } else {
                        setStyle("");
                    }
                }
            }
        });
        
        tblStandings.setItems(standingsList);
        loadStandings(currentGroup);
    }

    public void setStandingsService(StandingsService standingsService) {
        this.standingsService = standingsService;
    }

    @FXML
    public void handleGroupChange() {
        String selectedGroup = cmbSelectGroup.getValue();
        if (selectedGroup != null && !selectedGroup.equals(currentGroup)) {
            currentGroup = selectedGroup;
            loadStandings(currentGroup);
        }
    }

    @FXML
    public void handleRefresh() {
        try {
            // Recalculate standings for current group
            standingsService.recalculateGroupStandings(currentGroup);
            loadStandings(currentGroup);
            lblStatus.setText("Standings refreshed for Group " + currentGroup);
            lblStatus.setStyle("-fx-text-fill: blue;");
        } catch (SQLException e) {
            showAlert("Error", "Failed to refresh standings: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    public void handleDetermineQualifiers() {
        try {
            standingsService.determineGroupQualifiers(currentGroup);
            loadStandings(currentGroup);
            
            List<Team> standings = standingsService.getGroupStandings(currentGroup);
            if (standings.size() >= 2) {
                String msg = "Group " + currentGroup + " Qualifiers:\n" +
                            "1st: " + standings.get(0).getName() + "\n" +
                            "2nd: " + standings.get(1).getName();
                showAlert("Qualifiers Determined", msg, Alert.AlertType.INFORMATION);
            }
            
            lblStatus.setText("Top 2 teams from Group " + currentGroup + " marked as qualified!");
            lblStatus.setStyle("-fx-text-fill: green;");
        } catch (SQLException e) {
            showAlert("Error", "Failed to determine qualifiers: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    public void handleExport() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Standings to JSON");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON Files", "*.json"));
        fileChooser.setInitialFileName("group_" + currentGroup + "_standings.json");
        
        Stage stage = (Stage) btnExport.getScene().getWindow();
        File file = fileChooser.showSaveDialog(stage);
        
        if (file != null) {
            try {
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                List<Team> standings = standingsService.getGroupStandings(currentGroup);
                
                try (FileWriter writer = new FileWriter(file)) {
                    gson.toJson(standings, writer);
                }
                
                lblStatus.setText("Standings exported to " + file.getName());
                lblStatus.setStyle("-fx-text-fill: green;");
            } catch (Exception e) {
                showAlert("Export Error", "Failed to export standings: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        }
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

    private void loadStandings(String group) {
        try {
            standingsList.clear();
            List<Team> standings = standingsService.getGroupStandings(group);
            standingsList.addAll(standings);
            
            if (lblGroupTitle != null) {
                lblGroupTitle.setText("Group " + group + " Standings");
            }
            
        } catch (SQLException e) {
            showAlert("Database Error", "Failed to load standings: " + e.getMessage(), Alert.AlertType.ERROR);
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
