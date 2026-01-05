package com.fifaworldcup.ui;

import com.fifaworldcup.model.KnockoutMatch;
import com.fifaworldcup.model.Team;
import com.fifaworldcup.service.KnockoutService;
import com.fifaworldcup.service.StandingsService;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.fxml.FXML;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

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
    private StandingsService standingsService;

    public KnockoutBracketController() {
        this.knockoutService = new KnockoutService();
        this.standingsService = new StandingsService();
    }

    @FXML
    public void initialize() {
        if (cmbSelectRound != null) {
            cmbSelectRound.getItems().addAll("All", "Round of 16", "Quarter-Finals", "Semi-Finals", "Final");
            cmbSelectRound.setValue("All");
        }
        loadBracket();
    }

    public void setKnockoutService(KnockoutService knockoutService) {
        this.knockoutService = knockoutService;
    }

    @FXML
    public void handleGenerateBracket() {
        try {
            // Check if bracket already exists
            List<KnockoutMatch> existing = knockoutService.getAllKnockoutMatches();
            if (!existing.isEmpty()) {
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                confirm.setTitle("Confirm Generate");
                confirm.setHeaderText("Bracket Already Exists");
                confirm.setContentText("A knockout bracket already exists. Do you want to regenerate it?");
                
                if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
                    return;
                }
            }
            
            knockoutService.generateRoundOf16Bracket();
            loadBracket();
            
            lblStatus.setText("Knockout bracket generated successfully!");
            lblStatus.setStyle("-fx-text-fill: green;");
            
        } catch (SQLException e) {
            showAlert("Error", "Failed to generate bracket: " + e.getMessage(), Alert.AlertType.ERROR);
            lblStatus.setText("Error: " + e.getMessage());
            lblStatus.setStyle("-fx-text-fill: red;");
        }
    }

    @FXML
    public void handleRefresh() {
        loadBracket();
        lblStatus.setText("Bracket refreshed!");
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
    public void handleRoundChange() {
        loadBracket();
    }

    private void loadBracket() {
        try {
            String selectedRound = cmbSelectRound != null ? cmbSelectRound.getValue() : "All";
            
            // Clear existing content (keep headers)
            clearVBoxContent(vboxRound16);
            clearVBoxContent(vboxQuarter);
            clearVBoxContent(vboxSemi);
            clearVBoxContent(vboxFinal);
            
            if ("All".equals(selectedRound) || "Round of 16".equals(selectedRound)) {
                displayRound16();
            }
            if ("All".equals(selectedRound) || "Quarter-Finals".equals(selectedRound)) {
                displayQuarterFinals();
            }
            if ("All".equals(selectedRound) || "Semi-Finals".equals(selectedRound)) {
                displaySemiFinals();
            }
            if ("All".equals(selectedRound) || "Final".equals(selectedRound)) {
                displayFinal();
            }
            
            // Check for champion
            Team champion = knockoutService.getChampion();
            if (champion != null && lblChampion != null) {
                lblChampion.setText("🏆 CHAMPION: " + champion.getName() + " 🏆");
                lblChampion.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: gold;");
            }
            
        } catch (SQLException e) {
            showAlert("Error", "Failed to load bracket: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void clearVBoxContent(VBox vbox) {
        if (vbox != null && vbox.getChildren().size() > 1) {
            // Keep the first child (header label)
            Node header = vbox.getChildren().get(0);
            vbox.getChildren().clear();
            vbox.getChildren().add(header);
        }
    }

    private void displayRound16() throws SQLException {
        if (vboxRound16 == null) return;
        
        List<KnockoutMatch> matches = knockoutService.getKnockoutMatchesByRound("ROUND_16");
        for (KnockoutMatch match : matches) {
            vboxRound16.getChildren().add(createMatchCard(match));
        }
    }

    private void displayQuarterFinals() throws SQLException {
        if (vboxQuarter == null) return;
        
        List<KnockoutMatch> matches = knockoutService.getKnockoutMatchesByRound("QUARTER");
        for (KnockoutMatch match : matches) {
            vboxQuarter.getChildren().add(createMatchCard(match));
        }
    }

    private void displaySemiFinals() throws SQLException {
        if (vboxSemi == null) return;
        
        List<KnockoutMatch> matches = knockoutService.getKnockoutMatchesByRound("SEMI");
        for (KnockoutMatch match : matches) {
            vboxSemi.getChildren().add(createMatchCard(match));
        }
    }

    private void displayFinal() throws SQLException {
        if (vboxFinal == null) return;
        
        List<KnockoutMatch> matches = knockoutService.getKnockoutMatchesByRound("FINAL");
        for (KnockoutMatch match : matches) {
            vboxFinal.getChildren().add(createMatchCard(match));
        }
    }

    private VBox createMatchCard(KnockoutMatch match) {
        VBox card = new VBox(5);
        card.setStyle("-fx-background-color: white; -fx-border-color: #2a5298; -fx-border-radius: 5; " +
                     "-fx-background-radius: 5; -fx-padding: 10;");
        card.setPrefWidth(180);
        
        // Match number
        Label lblMatchNum = new Label("Match " + match.getMatchNumber());
        lblMatchNum.setStyle("-fx-font-size: 10px; -fx-text-fill: #666;");
        
        // Team 1
        HBox team1Box = new HBox(5);
        team1Box.setAlignment(Pos.CENTER_LEFT);
        Label lblTeam1 = new Label(match.getTeam1Name());
        lblTeam1.setStyle("-fx-font-weight: bold;");
        Label lblScore1 = new Label(match.isCompleted() ? String.valueOf(match.getTeam1Score()) : "-");
        team1Box.getChildren().addAll(lblTeam1, new Region(), lblScore1);
        HBox.setHgrow(team1Box.getChildren().get(1), Priority.ALWAYS);
        
        // VS label
        Label lblVs = new Label("vs");
        lblVs.setStyle("-fx-text-fill: #999;");
        lblVs.setAlignment(Pos.CENTER);
        
        // Team 2
        HBox team2Box = new HBox(5);
        team2Box.setAlignment(Pos.CENTER_LEFT);
        Label lblTeam2 = new Label(match.getTeam2Name());
        lblTeam2.setStyle("-fx-font-weight: bold;");
        Label lblScore2 = new Label(match.isCompleted() ? String.valueOf(match.getTeam2Score()) : "-");
        team2Box.getChildren().addAll(lblTeam2, new Region(), lblScore2);
        HBox.setHgrow(team2Box.getChildren().get(1), Priority.ALWAYS);
        
        // Highlight winner
        if (match.isCompleted() && match.getWinnerId() > 0) {
            if (match.getWinnerId() == match.getTeam1Id()) {
                lblTeam1.setStyle("-fx-font-weight: bold; -fx-text-fill: green;");
            } else {
                lblTeam2.setStyle("-fx-font-weight: bold; -fx-text-fill: green;");
            }
        }
        
        // Enter result button (if not completed and both teams assigned)
        if (!match.isCompleted() && match.getTeam1Id() > 0 && match.getTeam2Id() > 0) {
            Button btnEnterResult = new Button("Enter Result");
            btnEnterResult.setStyle("-fx-background-color: #2a5298; -fx-text-fill: white; -fx-font-size: 10px;");
            btnEnterResult.setOnAction(e -> showEnterResultDialog(match));
            card.getChildren().addAll(lblMatchNum, team1Box, lblVs, team2Box, btnEnterResult);
        } else {
            card.getChildren().addAll(lblMatchNum, team1Box, lblVs, team2Box);
        }
        
        // Status indicator
        if (match.isCompleted()) {
            Label lblComplete = new Label("✓ Completed");
            lblComplete.setStyle("-fx-text-fill: green; -fx-font-size: 10px;");
            card.getChildren().add(lblComplete);
        }
        
        return card;
    }

    private void showEnterResultDialog(KnockoutMatch match) {
        Dialog<int[]> dialog = new Dialog<>();
        dialog.setTitle("Enter Match Result");
        dialog.setHeaderText(match.getTeam1Name() + " vs " + match.getTeam2Name());
        
        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
        
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        
        TextField txtScore1 = new TextField("0");
        TextField txtScore2 = new TextField("0");
        
        grid.add(new Label(match.getTeam1Name() + " Score:"), 0, 0);
        grid.add(txtScore1, 1, 0);
        grid.add(new Label(match.getTeam2Name() + " Score:"), 0, 1);
        grid.add(txtScore2, 1, 1);
        
        // Winner selection for tie scenario
        ComboBox<String> cmbWinner = new ComboBox<>();
        cmbWinner.getItems().addAll(match.getTeam1Name(), match.getTeam2Name());
        cmbWinner.setPromptText("Select winner (if penalties)");
        grid.add(new Label("Winner (if tie):"), 0, 2);
        grid.add(cmbWinner, 1, 2);
        
        dialog.getDialogPane().setContent(grid);
        
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                try {
                    return new int[]{
                        Integer.parseInt(txtScore1.getText()),
                        Integer.parseInt(txtScore2.getText()),
                        cmbWinner.getSelectionModel().getSelectedIndex()
                    };
                } catch (NumberFormatException e) {
                    return null;
                }
            }
            return null;
        });
        
        Optional<int[]> result = dialog.showAndWait();
        result.ifPresent(scores -> {
            try {
                int score1 = scores[0];
                int score2 = scores[1];
                int winnerIdx = scores[2];
                
                int winnerId;
                if (score1 > score2) {
                    winnerId = match.getTeam1Id();
                } else if (score2 > score1) {
                    winnerId = match.getTeam2Id();
                } else {
                    // Tie - use selected winner (penalties)
                    winnerId = winnerIdx == 0 ? match.getTeam1Id() : match.getTeam2Id();
                }
                
                knockoutService.updateKnockoutMatchResult(match.getId(), score1, score2, winnerId);
                loadBracket();
                lblStatus.setText("Result saved!");
                lblStatus.setStyle("-fx-text-fill: green;");
                
            } catch (SQLException e) {
                showAlert("Error", "Failed to save result: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        });
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
