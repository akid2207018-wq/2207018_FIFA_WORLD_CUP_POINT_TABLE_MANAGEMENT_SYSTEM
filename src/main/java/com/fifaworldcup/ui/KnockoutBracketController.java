package com.fifaworldcup.ui;

import com.fifaworldcup.model.KnockoutMatch;
import com.fifaworldcup.service.KnockoutService;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

public class KnockoutBracketController {
    @FXML
    private ScrollPane scrollPane;
    
    @FXML
    private GridPane bracketGrid;
    
    @FXML
    private ComboBox<String> cmbSelectRound;
    
    @FXML
    private Button btnGenerateBracket;
    
    @FXML
    private Button btnRefresh;
    
    @FXML
    private Button btnBack;
    
    @FXML
    private VBox vboxRound16;
    
    @FXML
    private VBox vboxQuarter;
    
    @FXML
    private VBox vboxSemi;
    
    @FXML
    private VBox vboxFinal;
    
    @FXML
    private Label lblChampion;
    
    @FXML
    private Label lblStatus;

    private KnockoutService knockoutService;

    public KnockoutBracketController() {
        this.knockoutService = new KnockoutService();
    }

    @FXML
    public void initialize() {
        if (cmbSelectRound != null) {
            cmbSelectRound.getItems().addAll("Round of 16", "Quarter-Finals", "Semi-Finals", "Final");
        }
    }

    public void setKnockoutService(KnockoutService knockoutService) {
        this.knockoutService = knockoutService;
    }

    public void handleGenerateBracket() {
    }

    public void handleRefresh() {
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

    public void handleRoundChange() {
    }

    private void loadBracket() {
    }

    private void displayRound16() {
    }

    private void displayQuarterFinals() {
    }

    private void displaySemiFinals() {
    }

    private void displayFinal() {
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
    }
}
