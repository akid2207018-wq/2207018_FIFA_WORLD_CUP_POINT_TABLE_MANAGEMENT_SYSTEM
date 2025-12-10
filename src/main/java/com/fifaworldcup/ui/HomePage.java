package com.fifaworldcup.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.control.ScrollPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.shape.Rectangle;

public class HomePage {
    private BorderPane root;

    public HomePage() {
        root = new BorderPane();
        root.setStyle("-fx-background-color: #f0f0f0;");
        
        // Top: Header (includes in-app close button)
        root.setTop(createHeader());
        
        // Center: Main Content wrapped in a ScrollPane so content scrolls on small windows
        ScrollPane sp = new ScrollPane(createMainContent());
        sp.setFitToWidth(true);
        sp.setPannable(true);
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sp.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        sp.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        root.setCenter(sp);
        
        // Bottom: Footer
        root.setBottom(createFooter());
    }

    private VBox createHeader() {
        VBox header = new VBox();
        header.setStyle("-fx-background-color: linear-gradient(to right, #1e3c72, #2a5298);");
        header.setPadding(new Insets(20));
        header.setAlignment(Pos.CENTER);
        header.setSpacing(10);

        Label title = new Label("⚽ FIFA WORLD CUP");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 48));
        title.setTextFill(Color.web("#FFFFFF"));

        Label subtitle = new Label("Match & Points Table Management System");
        subtitle.setFont(Font.font("Arial", 18));
        subtitle.setTextFill(Color.web("#FFD700"));

        header.getChildren().addAll(title, subtitle);
        return header;
    }

    private VBox createMainContent() {
        VBox mainContent = new VBox();
        mainContent.setPadding(new Insets(40));
        mainContent.setSpacing(30);
        mainContent.setStyle("-fx-background-color: #f0f0f0;");

        // Welcome Section
        VBox welcomeSection = createWelcomeSection();
        
        // Features Grid
        VBox featuresSection = createFeaturesSection();
        
        mainContent.getChildren().addAll(welcomeSection, featuresSection);
        return mainContent;
    }

    private VBox createWelcomeSection() {
        VBox welcome = new VBox();
        welcome.setSpacing(15);
        welcome.setStyle("-fx-background-color: #ffffff; -fx-border-radius: 10; -fx-padding: 25;");
        welcome.setStyle("-fx-background-color: #ffffff; -fx-border-color: #ddd; -fx-border-radius: 10; -fx-padding: 25;");

        Label welcomeTitle = new Label("Welcome to FIFA World Cup Management System");
        welcomeTitle.setFont(Font.font("Arial", FontWeight.BOLD, 28));
        welcomeTitle.setTextFill(Color.web("#1e3c72"));

        Label welcomeDesc = new Label(
            "Manage team registration, schedule matches, track real-time points, and generate tournament brackets.\n" +
            "This system provides a complete solution for organizing and managing FIFA World Cup tournaments."
        );
        welcomeDesc.setFont(Font.font("Arial", 14));
        welcomeDesc.setWrapText(true);
        welcomeDesc.setTextFill(Color.web("#555555"));

        welcome.getChildren().addAll(welcomeTitle, welcomeDesc);
        return welcome;
    }

    private VBox createFeaturesSection() {
        VBox features = new VBox();
        features.setSpacing(20);

        Label featuresTitle = new Label("Core Features");
        featuresTitle.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        featuresTitle.setTextFill(Color.web("#1e3c72"));
        // Keep only the first five core feature buttons (no functionality attached)
        HBox row1 = new HBox();
        row1.setSpacing(20);
        row1.setFillHeight(true);
        row1.getChildren().addAll(
            createFeatureCard("Team Registration", "Register teams with codes and flags"),
            createFeatureCard("Group Formation", "Organize teams into World Cup groups"),
            createFeatureCard("Match Scheduling", "Generate group-stage fixtures")
        );

        HBox row2 = new HBox();
        row2.setSpacing(20);
        row2.setFillHeight(true);
        row2.getChildren().addAll(
            createFeatureCard("Match Results", "Enter and update match scores"),
            createFeatureCard("Points Table", "View real-time standings & rankings")
        );

        features.getChildren().addAll(featuresTitle, row1, row2);
        return features;
    }

    private HBox createFeatureButtonsRow(String... features) {
        // This method is no longer used; preserve implementation in case of future reuse.
        HBox row = new HBox();
        row.setSpacing(20);
        row.setFillHeight(true);
        row.setPrefHeight(150);
        return row;
    }

    private VBox createFeatureCard(String title, String description) {
        VBox card = new VBox();
        card.setPadding(new Insets(15));
        card.setSpacing(10);
        card.setStyle("-fx-background-color: #ffffff; -fx-border-color: #2a5298; -fx-border-radius: 8; " +
                      "-fx-border-width: 2; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 5, 0, 0, 2);");
        card.setAlignment(Pos.TOP_CENTER);

        Button btn = new Button(title);
        btn.setStyle("-fx-font-size: 13; -fx-font-weight: bold; -fx-padding: 8 15 8 15; " +
                     "-fx-background-color: #2a5298; -fx-text-fill: white; -fx-cursor: hand; " +
                     "-fx-border-radius: 5;");
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setPrefHeight(35);

        Label desc = new Label(description);
        desc.setFont(Font.font("Arial", 12));
        desc.setTextFill(Color.web("#777777"));
        desc.setWrapText(true);

        card.getChildren().addAll(btn, desc);
        return card;
    }

    private HBox createFooter() {
        HBox footer = new HBox();
        footer.setStyle("-fx-background-color: #1e3c72;");
        footer.setPadding(new Insets(20));
        footer.setAlignment(Pos.CENTER);

        Label footerText = new Label("FIFA World Cup Management System v1.0 | © 2025 | All Rights Reserved");
        footerText.setFont(Font.font("Arial", 12));
        footerText.setTextFill(Color.web("#FFD700"));

        footer.getChildren().add(footerText);
        return footer;
    }

    public BorderPane getRoot() {
        return root;
    }
}
