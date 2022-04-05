package client;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {
    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("loginWindow.fxml"));
//        Parent root = fxmlLoader.load();
//        stage.setScene(new Scene(root));
//        stage.show();
        Scene loginWindowScene = new Scene(fxmlLoader.load(), 300, 300);
        LoginWindow loginWindow = fxmlLoader.getController();
//        Controller controller = fxmlLoader.getController();
//        controller.setStage(stage);
//
        stage.setResizable(false);
        stage.setTitle("Hello!");
        stage.setScene(loginWindowScene);
        stage.show();


        stage.setOnCloseRequest(windowEvent -> {
            try {
                System.out.println("Stage is closing");
                loginWindow.onExit();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                Platform.exit();
            }
        });

    }
}
