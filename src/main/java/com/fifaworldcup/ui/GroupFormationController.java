package com.fifaworldcup.ui;

import com.fifaworldcup.model.Team;
import com.fifaworldcup.service.TeamService;
import com.fifaworldcup.service.Fifa2022ApiService;
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
import javafx.concurrent.Task;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Group Formation Controller
 * 
 * API/DATA FLOW:
 * 1. LOAD TEAMS:
 *    - Calls: teamService.getTeamsWithoutGroup()
 *    - SQL: SELECT * FROM teams WHERE group_name IS NULL OR group_name = ''
 *    - Returns: List of teams not yet assigned to any group
 * 
 * 2. ASSIGN TO GROUP:
 *    - User selects team from available list
 *    - User selects target group (A-H) from dropdown
 *    - Validates: Max 4 teams per group
 *    - Calls: teamService.assignTeamToGroup(teamId, groupName)
 *    - SQL: UPDATE teams SET group_name = ? WHERE id = ?
 *    - Updates: team.group_name column in database
 * 
 * 3. AUTO-ASSIGN:
 *    - Shuffles all available teams randomly
 *    - Distributes teams evenly across 8 groups (4 teams each)
 *    - Requires: Exactly 32 teams (8 groups × 4 teams)
 *    - Calls: teamService.assignTeamToGroup() for each team
 * 
 * 4. SAVE GROUPS:
 *    - Persists all group assignments to database
 *    - Prepares teams for match scheduling
 * 
 * 5. CLEAR GROUPS:
 *    - Calls: teamService.clearAllGroups()
 *    - SQL: UPDATE teams SET group_name = NULL
 *    - Resets all teams to unassigned state
 */

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
    
    @FXML
    private Button btnLoadFromApi;

    private TeamService teamService;
    private Fifa2022ApiService apiService;
    private ObservableList<Team> availableTeams;
    private static final int MAX_TEAMS_PER_GROUP = 4;

    public GroupFormationController() {
        this.teamService = new TeamService();
        this.apiService = new Fifa2022ApiService();
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
    
    /**
     * Load teams and groups from FIFA 2022 API
     * This will import all 32 teams from the actual World Cup 2022 with their correct groups
     */
    @FXML
    public void handleLoadFromApi() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Import from API");
        confirm.setHeaderText("Load FIFA 2022 World Cup Teams");
        confirm.setContentText("This will import all 32 teams from FIFA World Cup 2022 with their official groups. Continue?");
        
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }
        
        // Disable button during loading
        if (btnLoadFromApi != null) {
            btnLoadFromApi.setDisable(true);
        }
        lblStatus.setText("Loading teams from FIFA 2022 API...");
        lblStatus.setStyle("-fx-text-fill: blue;");
        
        // Run API call in background thread
        Task<Void> task = new Task<Void>() {
            private int importedCount = 0;
            private String errorMessage = null;
            
            @Override
            protected Void call() throws Exception {
                try {
                    // Fetch teams from API
                    List<Map<String, Object>> apiTeams = apiService.fetchAllTeams();
                    
                    if (apiTeams.isEmpty()) {
                        errorMessage = "No teams received from API";
                        return null;
                    }
                    
                    // Import each team
                    for (Map<String, Object> apiTeam : apiTeams) {
                        String teamName = (String) apiTeam.get("name");
                        String teamCode = (String) apiTeam.get("code");
                        String groupName = (String) apiTeam.get("group");
                        
                        try {
                            // Check if team exists
                            Team existingTeam = teamService.getTeamByName(teamName);
                            
                            if (existingTeam != null) {
                                // Update existing team's group
                                teamService.assignTeamToGroup(existingTeam.getId(), groupName);
                            } else {
                                // Add new team with group
                                Team newTeam = new Team();
                                newTeam.setName(teamName);
                                newTeam.setCode(teamCode);
                                newTeam.setGroup(groupName);
                                teamService.addTeam(newTeam);
                                
                                // Assign to group
                                Team addedTeam = teamService.getTeamByName(teamName);
                                if (addedTeam != null) {
                                    teamService.assignTeamToGroup(addedTeam.getId(), groupName);
                                }
                            }
                            importedCount++;
                        } catch (SQLException e) {
                            System.err.println("Error importing team " + teamName + ": " + e.getMessage());
                        }
                    }
                } catch (Exception e) {
                    errorMessage = e.getMessage();
                    e.printStackTrace();
                }
                return null;
            }
            
            @Override
            protected void succeeded() {
                if (btnLoadFromApi != null) {
                    btnLoadFromApi.setDisable(false);
                }
                
                if (errorMessage != null) {
                    lblStatus.setText("API Error: " + errorMessage);
                    lblStatus.setStyle("-fx-text-fill: red;");
                    showAlert("API Error", "Failed to load teams: " + errorMessage, Alert.AlertType.ERROR);
                } else {
                    refreshGroupLists();
                    lblStatus.setText("Successfully imported " + importedCount + " teams from FIFA 2022!");
                    lblStatus.setStyle("-fx-text-fill: green;");
                    showAlert("Success", "Imported " + importedCount + " teams from FIFA World Cup 2022 with their official groups!", Alert.AlertType.INFORMATION);
                }
            }
            
            @Override
            protected void failed() {
                if (btnLoadFromApi != null) {
                    btnLoadFromApi.setDisable(false);
                }
                lblStatus.setText("Failed to load teams from API");
                lblStatus.setStyle("-fx-text-fill: red;");
                showAlert("Error", "Failed to connect to API: " + getException().getMessage(), Alert.AlertType.ERROR);
            }
        };
        
        new Thread(task).start();
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
