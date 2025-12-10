package com.fifaworldcup;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.application.Platform;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.SnapshotParameters;
import javafx.scene.paint.Color;
import com.fifaworldcup.ui.HomePage;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            // Ensure OS window decorations are enabled before showing the stage
            primaryStage.initStyle(StageStyle.DECORATED);

            // Prefer loading layout from FXML; fallback to programmatic `HomePage` if FXML missing or fails
            Scene scene;
            java.net.URL fxmlUrl = getClass().getResource("/fxml/home.fxml");
            if (fxmlUrl != null) {
                try {
                    FXMLLoader loader = new FXMLLoader(fxmlUrl);
                    Parent root = loader.load();
                    scene = new Scene(root, 1160, 590);
                } catch (Exception ex) {
                    System.err.println("Failed to load FXML, falling back to programmatic UI: " + ex.getMessage());
                    HomePage homePage = new HomePage();
                    scene = new Scene(homePage.getRoot(), 1160, 590);
                }
            } else {
                HomePage homePage = new HomePage();
                scene = new Scene(homePage.getRoot(), 1160, 590);
            }
            
            // Load CSS stylesheet
            java.net.URL cssUrl = getClass().getResource("/styles/style.css");
            if (cssUrl != null) {
                scene.getStylesheets().add(cssUrl.toExternalForm());
            } else {
                System.err.println("Warning: stylesheet '/styles/style.css' not found on classpath.");
            }

            // Load application icons from resources (if provided). Common sizes: 256,64,32,16
            String[] iconPaths = new String[] {
                "/icons/app-icon-256.png",
                "/icons/app-icon-64.png",
                "/icons/app-icon-32.png",
                "/icons/app-icon-16.png"
            };
            for (String p : iconPaths) {
                try (java.io.InputStream is = getClass().getResourceAsStream(p)) {
                    if (is != null) {
                        Image img = new Image(is);
                        if (!img.isError()) primaryStage.getIcons().add(img);
                    }
                } catch (Exception ignore) {
                }
            }

            // If no icons were loaded from resources, generate a simple fallback icon programmatically
            if (primaryStage.getIcons().isEmpty()) {
                int size = 64;
                Canvas canvas = new Canvas(size, size);
                GraphicsContext gc = canvas.getGraphicsContext2D();
                // background
                gc.setFill(Color.web("#1e3c72"));
                gc.fillRect(0, 0, size, size);
                // gold circle
                gc.setFill(Color.web("#FFD700"));
                gc.fillOval(8, 8, size-16, size-16);
                // letters
                gc.setFill(Color.web("#1e3c72"));
                gc.setFont(javafx.scene.text.Font.font("Arial", javafx.scene.text.FontWeight.BOLD, 24));
                gc.fillText("WC", 18, 42);

                WritableImage wi = new WritableImage(size, size);
                SnapshotParameters sp = new SnapshotParameters();
                sp.setFill(Color.TRANSPARENT);
                canvas.snapshot(sp, wi);
                primaryStage.getIcons().add(wi);
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
