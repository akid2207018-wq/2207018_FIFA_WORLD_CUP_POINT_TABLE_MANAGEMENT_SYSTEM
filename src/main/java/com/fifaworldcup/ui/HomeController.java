package com.fifaworldcup.ui;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Stage;

import java.io.IOException;

public class HomeController {
    @FXML
    private Button btnTeamRegistration;

    @FXML
    private Button btnGroupFormation;

    @FXML
    private Button btnMatchScheduling;

    @FXML
    private Button btnMatchResults;

    @FXML
    private Button btnPointsTable;
    
    @FXML
    private Button btnKnockoutBracket;
    
    @FXML
    private Button btnMatchHistory;

    @FXML
    public void handleTeamRegistration(ActionEvent event) {
        loadScene(event, "/fxml/team_registration.fxml", "Team Registration");
    }

    @FXML
    public void handleGroupFormation(ActionEvent event) {
        loadScene(event, "/fxml/group_formation.fxml", "Group Formation");
    }

    @FXML
    public void handleMatchScheduling(ActionEvent event) {
        loadScene(event, "/fxml/match_scheduling.fxml", "Match Scheduling");
    }

    @FXML
    public void handleMatchResults(ActionEvent event) {
        loadScene(event, "/fxml/match_results.fxml", "Match Results");
    }

    @FXML
    public void handlePointsTable(ActionEvent event) {
        loadScene(event, "/fxml/points_table.fxml", "Points Table");
    }

    @FXML
    public void handleKnockoutBracket(ActionEvent event) {
        loadScene(event, "/fxml/knockout_bracket.fxml", "Knockout Bracket");
    }

    @FXML
    public void handleMatchHistory(ActionEvent event) {
        loadScene(event, "/fxml/match_history.fxml", "Match History");
    }

    private void loadScene(ActionEvent event, String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene currentScene = stage.getScene();
            Scene scene = new Scene(root, currentScene.getWidth(), currentScene.getHeight());
            stage.setScene(scene);
            stage.setTitle(title);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
