package com.fifaworldcup.ui;

import com.fifaworldcup.model.Team;
import com.fifaworldcup.service.TeamService;
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

import java.sql.SQLException;
import java.util.Collections;
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
    private ObservableList<Team> availableTeams;
    private static final int MAX_TEAMS_PER_GROUP = 4;

    public GroupFormationController() {
        this.teamService = new TeamService();
        this.availableTeams = FXCollections.observableArrayList();
    }

    @FXML
    public void initialize() {
        if (cmbTargetGroup != null) {
            cmbTargetGroup.getItems().addAll("A", "B", "C", "D", "E", "F", "G", "H");
        }
        
        // Setup list cell factories to display team names
        setupListView(lstAvailableTeams);
        setupListView(lstGroupA);
        setupListView(lstGroupB);
        setupListView(lstGroupC);
        setupListView(lstGroupD);
        setupListView(lstGroupE);
        setupListView(lstGroupF);
        setupListView(lstGroupG);
        setupListView(lstGroupH);
        
        // Enable double-click to remove from group
        setupDoubleClickRemove(lstGroupA, "A");
        setupDoubleClickRemove(lstGroupB, "B");
        setupDoubleClickRemove(lstGroupC, "C");
        setupDoubleClickRemove(lstGroupD, "D");
        setupDoubleClickRemove(lstGroupE, "E");
        setupDoubleClickRemove(lstGroupF, "F");
        setupDoubleClickRemove(lstGroupG, "G");
        setupDoubleClickRemove(lstGroupH, "H");
        
        loadTeams();
    }

    private void setupListView(ListView<Team> listView) {
        if (listView != null) {
            listView.setCellFactory(param -> new ListCell<Team>() {
                @Override
                protected void updateItem(Team item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                    } else {
                        setText(item.getCode() + " - " + item.getName());
                    }
                }
            });
        }
    }

    private void setupDoubleClickRemove(ListView<Team> listView, String group) {
        if (listView != null) {
            listView.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2) {
                    Team selected = listView.getSelectionModel().getSelectedItem();
                    if (selected != null) {
                        try {
                            teamService.assignTeamToGroup(selected.getId(), null);
                            refreshGroupLists();
                            lblStatus.setText(selected.getName() + " removed from Group " + group);
                            lblStatus.setStyle("-fx-text-fill: blue;");
                        } catch (SQLException e) {
                            showAlert("Error", "Failed to remove team: " + e.getMessage(), Alert.AlertType.ERROR);
                        }
                    }
                }
            });
        }
    }

    public void setTeamService(TeamService teamService) {
        this.teamService = teamService;
    }

    @FXML
    public void handleAssignToGroup() {
        Team selectedTeam = lstAvailableTeams.getSelectionModel().getSelectedItem();
        String targetGroup = cmbTargetGroup.getValue();
        
        if (selectedTeam == null) {
            showAlert("Selection Error", "Please select a team to assign.", Alert.AlertType.WARNING);
            return;
        }
        
        if (targetGroup == null || targetGroup.isEmpty()) {
            showAlert("Selection Error", "Please select a target group.", Alert.AlertType.WARNING);
            return;
        }
        
        try {
            // Check if group is full
            int count = teamService.getTeamCountInGroup(targetGroup);
            if (count >= MAX_TEAMS_PER_GROUP) {
                showAlert("Group Full", "Group " + targetGroup + " already has " + MAX_TEAMS_PER_GROUP + " teams.", Alert.AlertType.WARNING);
                return;
            }
            
            teamService.assignTeamToGroup(selectedTeam.getId(), targetGroup);
            refreshGroupLists();
            lblStatus.setText(selectedTeam.getName() + " assigned to Group " + targetGroup);
            lblStatus.setStyle("-fx-text-fill: green;");
        } catch (SQLException e) {
            showAlert("Database Error", "Failed to assign team: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    public void handleAutoAssign() {
        try {
            List<Team> unassigned = teamService.getTeamsWithoutGroup();
            
            if (unassigned.isEmpty()) {
                showAlert("Info", "No unassigned teams available.", Alert.AlertType.INFORMATION);
                return;
            }
            
            // Shuffle for random assignment
            Collections.shuffle(unassigned);
            
            String[] groups = {"A", "B", "C", "D", "E", "F", "G", "H"};
            int[] groupCounts = new int[8];
            
            // Get current counts
            for (int i = 0; i < groups.length; i++) {
                groupCounts[i] = teamService.getTeamCountInGroup(groups[i]);
            }
            
            int assignedCount = 0;
            for (Team team : unassigned) {
                // Find first group with space
                for (int i = 0; i < groups.length; i++) {
                    if (groupCounts[i] < MAX_TEAMS_PER_GROUP) {
                        teamService.assignTeamToGroup(team.getId(), groups[i]);
                        groupCounts[i]++;
                        assignedCount++;
                        break;
                    }
                }
            }
            
            refreshGroupLists();
            lblStatus.setText(assignedCount + " teams auto-assigned to groups!");
            lblStatus.setStyle("-fx-text-fill: green;");
        } catch (SQLException e) {
            showAlert("Database Error", "Failed to auto-assign teams: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    public void handleClearGroups() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Clear");
        confirm.setHeaderText("Clear All Groups");
        confirm.setContentText("Are you sure you want to remove all teams from groups?");
        
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try {
                teamService.clearAllGroups();
                refreshGroupLists();
                lblStatus.setText("All groups cleared!");
                lblStatus.setStyle("-fx-text-fill: blue;");
            } catch (SQLException e) {
                showAlert("Database Error", "Failed to clear groups: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }

    @FXML
    public void handleSaveGroups() {
        // Groups are saved automatically when assigned
        lblStatus.setText("Groups saved successfully!");
        lblStatus.setStyle("-fx-text-fill: green;");
        showAlert("Success", "All group assignments have been saved.", Alert.AlertType.INFORMATION);
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
        refreshGroupLists();
    }

    private void refreshGroupLists() {
        try {
            // Load available (unassigned) teams
            if (lstAvailableTeams != null) {
                lstAvailableTeams.getItems().clear();
                lstAvailableTeams.getItems().addAll(teamService.getTeamsWithoutGroup());
            }
            
            // Load each group
            loadGroupList(lstGroupA, "A");
            loadGroupList(lstGroupB, "B");
            loadGroupList(lstGroupC, "C");
            loadGroupList(lstGroupD, "D");
            loadGroupList(lstGroupE, "E");
            loadGroupList(lstGroupF, "F");
            loadGroupList(lstGroupG, "G");
            loadGroupList(lstGroupH, "H");
            
        } catch (SQLException e) {
            showAlert("Database Error", "Failed to load teams: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void loadGroupList(ListView<Team> listView, String group) throws SQLException {
        if (listView != null) {
            listView.getItems().clear();
            listView.getItems().addAll(teamService.getTeamsByGroup(group));
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
