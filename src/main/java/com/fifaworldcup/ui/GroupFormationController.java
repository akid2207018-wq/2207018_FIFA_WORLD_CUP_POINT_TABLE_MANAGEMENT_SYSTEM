package com.fifaworldcup.ui;

import com.fifaworldcup.model.Team;
import com.fifaworldcup.service.TeamService;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.util.List;

public class GroupFormationController {
    @FXML
    private ListView<Team> lstAvailableTeams;
    
    @FXML
    private ListView<Team> lstGroupA;
    
    @FXML
    private ListView<Team> lstGroupB;
    
    @FXML
    private ListView<Team> lstGroupC;
    
    @FXML
    private ListView<Team> lstGroupD;
    
    @FXML
    private ListView<Team> lstGroupE;
    
    @FXML
    private ListView<Team> lstGroupF;
    
    @FXML
    private ListView<Team> lstGroupG;
    
    @FXML
    private ListView<Team> lstGroupH;
    
    @FXML
    private Button btnAssignToGroup;
    
    @FXML
    private Button btnAutoAssign;
    
    @FXML
    private Button btnClearGroups;
    
    @FXML
    private Button btnSaveGroups;
    
    @FXML
    private Button btnBack;
    
    @FXML
    private ComboBox<String> cmbTargetGroup;
    
    @FXML
    private Label lblStatus;

    private TeamService teamService;

    public GroupFormationController() {
        this.teamService = new TeamService();
    }

    @FXML
    public void initialize() {
        if (cmbTargetGroup != null) {
            cmbTargetGroup.getItems().addAll("A", "B", "C", "D", "E", "F", "G", "H");
        }
    }

    public void setTeamService(TeamService teamService) {
        this.teamService = teamService;
    }

    public void handleAssignToGroup() {
    }

    public void handleAutoAssign() {
    }

    public void handleClearGroups() {
    }

    public void handleSaveGroups() {
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

    private void loadTeams() {
    }

    private void refreshGroupLists() {
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
    }
}
