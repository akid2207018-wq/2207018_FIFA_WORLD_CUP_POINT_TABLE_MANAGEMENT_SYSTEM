package com.fifaworldcup.ui;

import com.fifaworldcup.model.Team;
import com.fifaworldcup.service.TeamService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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
import java.sql.SQLException;

public class TeamRegistrationController {
    @FXML
    private TextField txtTeamName;
    
    @FXML
    private TextField txtTeamCode;
    
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
    private ObservableList<Team> teamsList;
    private Team selectedTeam;

    public TeamRegistrationController() {
        this.teamService = new TeamService();
        this.teamsList = FXCollections.observableArrayList();
    }

    @FXML
    public void initialize() {
        // Setup table columns
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colCode.setCellValueFactory(new PropertyValueFactory<>("code"));
        colGroup.setCellValueFactory(new PropertyValueFactory<>("group"));
        
        tblTeams.setItems(teamsList);
        
        // Add selection listener
        tblTeams.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            if (newSel != null) {
                selectedTeam = newSel;
                txtTeamName.setText(newSel.getName());
                txtTeamCode.setText(newSel.getCode());
            }
        });
        
        loadTeams();
    }

    public void setTeamService(TeamService teamService) {
        this.teamService = teamService;
    }

    @FXML
    public void handleAddTeam() {
        String name = txtTeamName.getText().trim();
        String code = txtTeamCode.getText().trim().toUpperCase();
        
        if (name.isEmpty() || code.isEmpty()) {
            showAlert("Validation Error", "Team name and code are required.", Alert.AlertType.WARNING);
            return;
        }
        
        if (!code.matches("^[A-Z]{3}$")) {
            showAlert("Validation Error", "Team code must be exactly 3 uppercase letters.", Alert.AlertType.WARNING);
            return;
        }
        
        try {
            Team team = new Team();
            team.setName(name);
            team.setCode(code);
            
            teamService.addTeam(team);
            loadTeams();
            clearForm();
            lblStatus.setText("Team '" + name + "' added successfully!");
            lblStatus.setStyle("-fx-text-fill: green;");
        } catch (SQLException e) {
            String errorMsg = e.getMessage();
            if (errorMsg.contains("UNIQUE constraint failed") || errorMsg.contains("already exists")) {
                if (errorMsg.contains("name") || errorMsg.contains(name)) {
                    showAlert("Duplicate Team", "A team with the name '" + name + "' already exists. Please use a different name.", Alert.AlertType.WARNING);
                } else if (errorMsg.contains("code") || errorMsg.contains(code)) {
                    showAlert("Duplicate Team Code", "A team with the code '" + code + "' already exists. Please use a different code.", Alert.AlertType.WARNING);
                } else {
                    showAlert("Duplicate Entry", errorMsg, Alert.AlertType.WARNING);
                }
            } else {
                showAlert("Database Error", "Failed to add team: " + errorMsg, Alert.AlertType.ERROR);
            }
            lblStatus.setText("Error: " + e.getMessage());
            lblStatus.setStyle("-fx-text-fill: red;");
        }
    }

    @FXML
    public void handleUpdateTeam() {
        if (selectedTeam == null) {
            showAlert("Selection Error", "Please select a team to update.", Alert.AlertType.WARNING);
            return;
        }
        
        String name = txtTeamName.getText().trim();
        String code = txtTeamCode.getText().trim().toUpperCase();
        
        if (name.isEmpty() || code.isEmpty()) {
            showAlert("Validation Error", "Team name and code are required.", Alert.AlertType.WARNING);
            return;
        }
        
        try {
            selectedTeam.setName(name);
            selectedTeam.setCode(code);
            
            teamService.updateTeam(selectedTeam);
            loadTeams();
            clearForm();
            lblStatus.setText("Team updated successfully!");
            lblStatus.setStyle("-fx-text-fill: green;");
        } catch (SQLException e) {
            String errorMsg = e.getMessage();
            if (errorMsg.contains("UNIQUE constraint failed") || errorMsg.contains("already exists")) {
                showAlert("Duplicate Entry", errorMsg, Alert.AlertType.WARNING);
            } else {
                showAlert("Database Error", "Failed to update team: " + errorMsg, Alert.AlertType.ERROR);
            }
            lblStatus.setText("Error: " + e.getMessage());
            lblStatus.setStyle("-fx-text-fill: red;");
        }
    }

    @FXML
    public void handleDeleteTeam() {
        if (selectedTeam == null) {
            showAlert("Selection Error", "Please select a team to delete.", Alert.AlertType.WARNING);
            return;
        }
        
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Delete");
        confirm.setHeaderText("Delete Team");
        confirm.setContentText("Are you sure you want to delete '" + selectedTeam.getName() + "'? This will also delete related matches and players.");
        
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try {
                teamService.deleteTeam(selectedTeam.getId());
                loadTeams();
                clearForm();
                lblStatus.setText("Team deleted successfully!");
                lblStatus.setStyle("-fx-text-fill: green;");
            } catch (SQLException e) {
                showAlert("Database Error", "Failed to delete team: " + e.getMessage(), Alert.AlertType.ERROR);
                lblStatus.setText("Error: " + e.getMessage());
                lblStatus.setStyle("-fx-text-fill: red;");
            }
        }
    }

    @FXML
    public void handleExportJson() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Teams to JSON");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON Files", "*.json"));
        fileChooser.setInitialFileName("teams.json");
        
        Stage stage = (Stage) btnExportJson.getScene().getWindow();
        File file = fileChooser.showSaveDialog(stage);
        
        if (file != null) {
            try {
                teamService.exportTeamsToJson(file.getAbsolutePath());
                lblStatus.setText("Teams exported successfully to " + file.getName());
                lblStatus.setStyle("-fx-text-fill: green;");
            } catch (Exception e) {
                showAlert("Export Error", "Failed to export teams: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }

    @FXML
    public void handleImportJson() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Import Teams from JSON");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON Files", "*.json"));
        
        Stage stage = (Stage) btnImportJson.getScene().getWindow();
        File file = fileChooser.showOpenDialog(stage);
        
        if (file != null) {
            try {
                teamService.importTeamsFromJson(file.getAbsolutePath());
                loadTeams();
                lblStatus.setText("Teams imported successfully from " + file.getName());
                lblStatus.setStyle("-fx-text-fill: green;");
            } catch (Exception e) {
                showAlert("Import Error", "Failed to import teams: " + e.getMessage(), Alert.AlertType.ERROR);
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

    private void loadTeams() {
        try {
            teamsList.clear();
            teamsList.addAll(teamService.getAllTeams());
        } catch (SQLException e) {
            showAlert("Database Error", "Failed to load teams: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void clearForm() {
        txtTeamName.clear();
        txtTeamCode.clear();
        selectedTeam = null;
        tblTeams.getSelectionModel().clearSelection();
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
