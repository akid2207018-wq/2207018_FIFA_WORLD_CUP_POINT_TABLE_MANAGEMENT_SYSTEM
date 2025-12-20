package com.fifaworldcup.ui;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import java.io.IOException;

public class HomeController {
    @FXML
    private BorderPane root;

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
    private Button btnPlayerStats;

    public void handleTeamRegistration() {
        navigateToScene("/fxml/team_registration.fxml", "Team Registration");
    }

    public void handleGroupFormation() {
        navigateToScene("/fxml/group_formation.fxml", "Group Formation");
    }

    public void handleMatchScheduling() {
        navigateToScene("/fxml/match_scheduling.fxml", "Match Scheduling");
    }

    public void handleMatchResults() {
        navigateToScene("/fxml/match_results.fxml", "Match Results");
    }

    public void handlePointsTable() {
        navigateToScene("/fxml/points_table.fxml", "Points Table");
    }

    public void handleKnockoutBracket() {
        navigateToScene("/fxml/knockout_bracket.fxml", "Knockout Bracket");
    }

    public void handleMatchHistory() {
        navigateToScene("/fxml/match_history.fxml", "Match History");
    }

    public void handlePlayerStats() {
        navigateToScene("/fxml/player_stats.fxml", "Player Statistics");
    }

    private void navigateToScene(String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent newRoot = loader.load();
            
            Stage stage = (Stage) root.getScene().getWindow();
            Scene scene = new Scene(newRoot);
            
            // Apply stylesheet
            java.net.URL cssUrl = getClass().getResource("/styles/style.css");
            if (cssUrl != null) {
                scene.getStylesheets().add(cssUrl.toExternalForm());
            }
            
            stage.setScene(scene);
            stage.setTitle("FIFA World Cup - " + title);
        } catch (IOException e) {
            showAlert("Navigation Error", "Failed to load " + title + " page: " + e.getMessage(), Alert.AlertType.ERROR);
            e.printStackTrace();
        }
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public BorderPane getRoot() {
        return root;
    }
}
