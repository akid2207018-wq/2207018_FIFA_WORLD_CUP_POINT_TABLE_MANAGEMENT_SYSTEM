package com.fifaworldcup.ui;

import com.fifaworldcup.model.Team;
import com.fifaworldcup.service.TeamService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;

public class TeamRegistrationController {
    @FXML
    private TextField txtTeamName;
    
    @FXML
    private TextField txtTeamCode;
    
    @FXML
    private TextField txtFlagPath;
    
    @FXML
    private Button btnBrowseFlag;
    
    @FXML
    private Button btnAddTeam;
    
    @FXML
    private Button btnUpdateTeam;
    
    @FXML
    private Button btnDeleteTeam;
    
    @FXML
    private Button btnExportJson;
    
    @FXML
    private Button btnImportJson;
    
    @FXML
    private Button btnBack;
    
    @FXML
    private TableView<Team> tblTeams;
    
    @FXML
    private TableColumn<Team, Integer> colId;
    
    @FXML
    private TableColumn<Team, String> colName;
    
    @FXML
    private TableColumn<Team, String> colCode;
    
    @FXML
    private TableColumn<Team, String> colGroup;
    
    @FXML
    private Label lblStatus;

    private TeamService teamService;

    public TeamRegistrationController() {
        this.teamService = new TeamService();
    }

    public void setTeamService(TeamService teamService) {
        this.teamService = teamService;
    }

    public void handleBrowseFlag() {
    }

    public void handleAddTeam() {
    }

    public void handleUpdateTeam() {
    }

    public void handleDeleteTeam() {
    }

    public void handleExportJson() {
    }

    public void handleImportJson() {
    }

    public void handleBack(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/home.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle("FIFA World Cup - Home");
            stage.sizeToScene();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadTeams() {
    }

    private void clearForm() {
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
    }
}
