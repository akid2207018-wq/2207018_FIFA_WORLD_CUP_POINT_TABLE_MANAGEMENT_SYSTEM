package com.fifaworldcup.ui;

import com.fifaworldcup.model.Team;
import com.fifaworldcup.service.StandingsService;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

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

    public PointsTableController() {
        this.standingsService = new StandingsService();
    }

    @FXML
    public void initialize() {
        if (cmbSelectGroup != null) {
            cmbSelectGroup.getItems().addAll("A", "B", "C", "D", "E", "F", "G", "H");
        }
    }

    public void setStandingsService(StandingsService standingsService) {
        this.standingsService = standingsService;
    }

    public void handleGroupChange() {
    }

    public void handleRefresh() {
    }

    public void handleDetermineQualifiers() {
    }

    public void handleExport() {
    }

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
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
    }
}
