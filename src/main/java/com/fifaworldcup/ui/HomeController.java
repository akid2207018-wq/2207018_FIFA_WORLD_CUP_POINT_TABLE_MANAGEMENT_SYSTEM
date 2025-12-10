package com.fifaworldcup.ui;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;

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
    public void initialize() {
        // Placeholder handlers: replace with real navigation later
        if (btnTeamRegistration != null) btnTeamRegistration.setOnAction(e -> System.out.println("Team Registration clicked"));
        if (btnGroupFormation != null) btnGroupFormation.setOnAction(e -> System.out.println("Group Formation clicked"));
        if (btnMatchScheduling != null) btnMatchScheduling.setOnAction(e -> System.out.println("Match Scheduling clicked"));
        if (btnMatchResults != null) btnMatchResults.setOnAction(e -> System.out.println("Match Results clicked"));
        if (btnPointsTable != null) btnPointsTable.setOnAction(e -> System.out.println("Points Table clicked"));
    }

    public BorderPane getRoot() {
        return root;
    }
}
