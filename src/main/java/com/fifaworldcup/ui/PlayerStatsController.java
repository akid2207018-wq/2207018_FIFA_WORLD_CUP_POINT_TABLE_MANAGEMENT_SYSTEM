package com.fifaworldcup.ui;

import com.fifaworldcup.model.Player;
import com.fifaworldcup.model.Team;
import com.fifaworldcup.service.PlayerService;
import com.fifaworldcup.service.TeamService;
import javafx.beans.property.SimpleIntegerProperty;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlayerStatsController {
    @FXML
    private TableView<Player> tblTopScorers;
    
    @FXML
    private TableColumn<Player, Integer> colScorerPosition;
    
    @FXML
    private TableColumn<Player, String> colScorerName;
    
    @FXML
    private TableColumn<Player, String> colScorerTeam;
    
    @FXML
    private TableColumn<Player, Integer> colGoals;
    
    @FXML
    private TableView<Player> tblTopAssists;
    
    @FXML
    private TableColumn<Player, Integer> colAssistPosition;
    
    @FXML
    private TableColumn<Player, String> colAssistName;
    
    @FXML
    private TableColumn<Player, String> colAssistTeam;
    
    @FXML
    private TableColumn<Player, Integer> colAssists;
    
    @FXML
    private TableView<Player> tblAllPlayers;
    
    @FXML
    private TableColumn<Player, String> colPlayerName;
    
    @FXML
    private TableColumn<Player, String> colPlayerTeam;
    
    @FXML
    private TableColumn<Player, Integer> colPlayerNumber;
    
    @FXML
    private TableColumn<Player, String> colPlayerPosition;
    
    @FXML
    private TableColumn<Player, Integer> colPlayerGoals;
    
    @FXML
    private TableColumn<Player, Integer> colPlayerAssists;
    
    @FXML
    private TableColumn<Player, Integer> colPlayerMatches;
    
    @FXML
    private TextField txtPlayerName;
    
    @FXML
    private TextField txtJerseyNumber;
    
    @FXML
    private TextField txtPlayerGoals;
    
    @FXML
    private TextField txtPlayerAssists;
    
    @FXML
    private ComboBox<String> cmbPlayerTeam;
    
    @FXML
    private ComboBox<String> cmbPlayerPosition;
    
    @FXML
    private ComboBox<String> cmbFilterTeam;
    
    @FXML
    private Button btnAddPlayer;
    
    @FXML
    private Button btnUpdatePlayer;
    
    @FXML
    private Button btnDeletePlayer;
    
    @FXML
    private Button btnRefresh;
    
    @FXML
    private Button btnBack;
    
    @FXML
    private Label lblStatus;

    private PlayerService playerService;
    private TeamService teamService;
    private ObservableList<Player> topScorersList;
    private ObservableList<Player> topAssistsList;
    private ObservableList<Player> allPlayersList;
    private Map<String, Integer> teamNameToId;
    private Player selectedPlayer;

    public PlayerStatsController() {
        this.playerService = new PlayerService();
        this.teamService = new TeamService();
        this.topScorersList = FXCollections.observableArrayList();
        this.topAssistsList = FXCollections.observableArrayList();
        this.allPlayersList = FXCollections.observableArrayList();
        this.teamNameToId = new HashMap<>();
    }

    @FXML
    public void initialize() {
        if (cmbPlayerPosition != null) {
            cmbPlayerPosition.getItems().addAll("Goalkeeper", "Defender", "Midfielder", "Forward");
        }
        
        // Setup top scorers table
        if (tblTopScorers != null) {
            colScorerPosition.setCellValueFactory(cellData -> {
                int index = topScorersList.indexOf(cellData.getValue()) + 1;
                return new SimpleIntegerProperty(index).asObject();
            });
            colScorerName.setCellValueFactory(new PropertyValueFactory<>("name"));
            colScorerTeam.setCellValueFactory(new PropertyValueFactory<>("teamName"));
            colGoals.setCellValueFactory(new PropertyValueFactory<>("goals"));
            tblTopScorers.setItems(topScorersList);
        }
        
        // Setup top assists table
        if (tblTopAssists != null) {
            colAssistPosition.setCellValueFactory(cellData -> {
                int index = topAssistsList.indexOf(cellData.getValue()) + 1;
                return new SimpleIntegerProperty(index).asObject();
            });
            colAssistName.setCellValueFactory(new PropertyValueFactory<>("name"));
            colAssistTeam.setCellValueFactory(new PropertyValueFactory<>("teamName"));
            colAssists.setCellValueFactory(new PropertyValueFactory<>("assists"));
            tblTopAssists.setItems(topAssistsList);
        }
        
        // Setup all players table
        if (tblAllPlayers != null) {
            colPlayerName.setCellValueFactory(new PropertyValueFactory<>("name"));
            colPlayerTeam.setCellValueFactory(new PropertyValueFactory<>("teamName"));
            colPlayerNumber.setCellValueFactory(new PropertyValueFactory<>("jerseyNumber"));
            colPlayerPosition.setCellValueFactory(new PropertyValueFactory<>("position"));
            colPlayerGoals.setCellValueFactory(new PropertyValueFactory<>("goals"));
            colPlayerAssists.setCellValueFactory(new PropertyValueFactory<>("assists"));
            colPlayerMatches.setCellValueFactory(new PropertyValueFactory<>("matchesPlayed"));
            tblAllPlayers.setItems(allPlayersList);
        }
        
        loadTeamsForFilter();
        loadTopScorers();
        loadTopAssists();
        loadAllPlayers();
    }

    public void setPlayerService(PlayerService playerService) {
        this.playerService = playerService;
    }

    @FXML
    public void handleAddPlayer() {
        if (!validatePlayerForm()) {
            return;
        }
        
        try {
            Player player = new Player();
            player.setName(txtPlayerName.getText().trim());
            player.setJerseyNumber(Integer.parseInt(txtJerseyNumber.getText().trim()));
            player.setTeamId(teamNameToId.get(cmbPlayerTeam.getValue()));
            player.setPosition(cmbPlayerPosition.getValue());
            player.setGoals(parseIntOrZero(txtPlayerGoals.getText()));
            player.setAssists(parseIntOrZero(txtPlayerAssists.getText()));
            
            playerService.addPlayer(player);
            
            refreshAllData();
            clearForm();
            lblStatus.setText("Player '" + player.getName() + "' added successfully!");
            lblStatus.setStyle("-fx-text-fill: green;");
            
        } catch (SQLException e) {
            showAlert("Database Error", "Failed to add player: " + e.getMessage(), Alert.AlertType.ERROR);
            lblStatus.setText("Error: " + e.getMessage());
            lblStatus.setStyle("-fx-text-fill: red;");
        }
    }

    @FXML
    public void handleUpdatePlayer() {
        if (selectedPlayer == null) {
            showAlert("Selection Error", "Please select a player to update.", Alert.AlertType.WARNING);
            return;
        }
        
        if (!validatePlayerForm()) {
            return;
        }
        
        try {
            selectedPlayer.setName(txtPlayerName.getText().trim());
            selectedPlayer.setJerseyNumber(Integer.parseInt(txtJerseyNumber.getText().trim()));
            selectedPlayer.setTeamId(teamNameToId.get(cmbPlayerTeam.getValue()));
            selectedPlayer.setPosition(cmbPlayerPosition.getValue());
            selectedPlayer.setGoals(parseIntOrZero(txtPlayerGoals.getText()));
            selectedPlayer.setAssists(parseIntOrZero(txtPlayerAssists.getText()));
            
            playerService.updatePlayer(selectedPlayer);
            
            refreshAllData();
            clearForm();
            lblStatus.setText("Player updated successfully!");
            lblStatus.setStyle("-fx-text-fill: green;");
            
        } catch (SQLException e) {
            showAlert("Database Error", "Failed to update player: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    public void handleDeletePlayer() {
        if (selectedPlayer == null) {
            showAlert("Selection Error", "Please select a player to delete.", Alert.AlertType.WARNING);
            return;
        }
        
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Delete");
        confirm.setHeaderText("Delete Player");
        confirm.setContentText("Are you sure you want to delete '" + selectedPlayer.getName() + "'?");
        
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try {
                playerService.deletePlayer(selectedPlayer.getId());
                refreshAllData();
                clearForm();
                lblStatus.setText("Player deleted successfully!");
                lblStatus.setStyle("-fx-text-fill: green;");
            } catch (SQLException e) {
                showAlert("Database Error", "Failed to delete player: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }

    @FXML
    public void handleRefresh() {
        refreshAllData();
        lblStatus.setText("Player statistics refreshed!");
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
        loadAllPlayers();
    }

    @FXML
    public void handlePlayerSelection() {
        selectedPlayer = tblAllPlayers.getSelectionModel().getSelectedItem();
        if (selectedPlayer != null) {
            txtPlayerName.setText(selectedPlayer.getName());
            txtJerseyNumber.setText(String.valueOf(selectedPlayer.getJerseyNumber()));
            txtPlayerGoals.setText(String.valueOf(selectedPlayer.getGoals()));
            txtPlayerAssists.setText(String.valueOf(selectedPlayer.getAssists()));
            cmbPlayerTeam.setValue(selectedPlayer.getTeamName());
            cmbPlayerPosition.setValue(selectedPlayer.getPosition());
        }
    }

    private void refreshAllData() {
        loadTopScorers();
        loadTopAssists();
        loadAllPlayers();
    }

    private void loadTopScorers() {
        try {
            topScorersList.clear();
            topScorersList.addAll(playerService.getTopScorers(10));
        } catch (SQLException e) {
            // Ignore
        }
    }

    private void loadTopAssists() {
        try {
            topAssistsList.clear();
            topAssistsList.addAll(playerService.getTopAssists(10));
        } catch (SQLException e) {
            // Ignore
        }
    }

    private void loadAllPlayers() {
        try {
            allPlayersList.clear();
            
            String filterTeam = cmbFilterTeam != null ? cmbFilterTeam.getValue() : "All";
            List<Player> players;
            
            if ("All".equals(filterTeam) || filterTeam == null) {
                players = playerService.getAllPlayers();
            } else {
                Integer teamId = teamNameToId.get(filterTeam);
                if (teamId != null) {
                    players = playerService.getPlayersByTeam(teamId);
                } else {
                    players = playerService.getAllPlayers();
                }
            }
            
            allPlayersList.addAll(players);
        } catch (SQLException e) {
            showAlert("Database Error", "Failed to load players: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void loadTeamsForFilter() {
        try {
            List<Team> teams = teamService.getAllTeams();
            
            teamNameToId.clear();
            for (Team team : teams) {
                teamNameToId.put(team.getName(), team.getId());
            }
            
            if (cmbPlayerTeam != null) {
                cmbPlayerTeam.getItems().clear();
                for (Team team : teams) {
                    cmbPlayerTeam.getItems().add(team.getName());
                }
            }
            
            if (cmbFilterTeam != null) {
                cmbFilterTeam.getItems().clear();
                cmbFilterTeam.getItems().add("All");
                for (Team team : teams) {
                    cmbFilterTeam.getItems().add(team.getName());
                }
                cmbFilterTeam.setValue("All");
            }
        } catch (SQLException e) {
            // Ignore
        }
    }

    private void clearForm() {
        txtPlayerName.clear();
        txtJerseyNumber.clear();
        txtPlayerGoals.clear();
        txtPlayerAssists.clear();
        if (cmbPlayerTeam != null) cmbPlayerTeam.setValue(null);
        if (cmbPlayerPosition != null) cmbPlayerPosition.setValue(null);
        selectedPlayer = null;
        if (tblAllPlayers != null) tblAllPlayers.getSelectionModel().clearSelection();
    }

    private boolean validatePlayerForm() {
        String name = txtPlayerName.getText().trim();
        String jerseyStr = txtJerseyNumber.getText().trim();
        String team = cmbPlayerTeam != null ? cmbPlayerTeam.getValue() : null;
        String position = cmbPlayerPosition != null ? cmbPlayerPosition.getValue() : null;
        
        if (name.isEmpty()) {
            showAlert("Validation Error", "Player name is required.", Alert.AlertType.WARNING);
            return false;
        }
        
        if (jerseyStr.isEmpty()) {
            showAlert("Validation Error", "Jersey number is required.", Alert.AlertType.WARNING);
            return false;
        }
        
        try {
            int jersey = Integer.parseInt(jerseyStr);
            if (jersey < 1 || jersey > 99) {
                showAlert("Validation Error", "Jersey number must be between 1 and 99.", Alert.AlertType.WARNING);
                return false;
            }
        } catch (NumberFormatException e) {
            showAlert("Validation Error", "Please enter a valid jersey number.", Alert.AlertType.WARNING);
            return false;
        }
        
        if (team == null || team.isEmpty()) {
            showAlert("Validation Error", "Please select a team.", Alert.AlertType.WARNING);
            return false;
        }
        
        if (position == null || position.isEmpty()) {
            showAlert("Validation Error", "Please select a position.", Alert.AlertType.WARNING);
            return false;
        }
        
        return true;
    }

    private int parseIntOrZero(String text) {
        try {
            return text != null && !text.trim().isEmpty() ? Integer.parseInt(text.trim()) : 0;
        } catch (NumberFormatException e) {
            return 0;
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
