package com.fifaworldcup;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.application.Platform;
import com.fifaworldcup.ui.HomePage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            // Ensure OS window decorations are enabled before showing the stage
            primaryStage.initStyle(StageStyle.DECORATED);

            HomePage homePage = new HomePage();
            // Make the Scene slightly smaller than the Stage so OS window controls aren't obstructed
            Scene scene = new Scene(homePage.getRoot(), 1160, 590);
            
            // Load CSS stylesheet (guarded to avoid NPE if resource missing)
            java.net.URL cssUrl = getClass().getResource("/styles/style.css");
            if (cssUrl != null) {
                scene.getStylesheets().add(cssUrl.toExternalForm());
            } else {
                System.err.println("Warning: stylesheet '/styles/style.css' not found on classpath.");
            }
            
            primaryStage.setTitle("FIFA World Cup - Point Table Management System");
            primaryStage.setScene(scene);
            primaryStage.setWidth(1000);
            primaryStage.setHeight(500);
            primaryStage.setResizable(true);
            // Ensure window closes cleanly
            primaryStage.setOnCloseRequest(event -> {
                Platform.exit();
                System.exit(0);
            });
            primaryStage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
