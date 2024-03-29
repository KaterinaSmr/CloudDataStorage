package client;

import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import javafx.stage.Modality;
import javafx.stage.Stage;


public class MessageWindow {
    private final Stage window;
    private final Label label;
    private final Button buttonOk;
    private final Button buttonCancel;
    private final HBox buttonPane;
    public enum Type {INFORMATION, CONFIRMATION}
    private boolean result;

    public MessageWindow(){
        this.window = new Stage();
        this.label = new Label();
        this.buttonOk = new Button("OK");
        this.buttonCancel = new Button("Cancel");
        buttonOk.setPrefWidth(80);
        buttonCancel.setPrefWidth(80);
        label.setWrapText(true);
        label.setTextAlignment(TextAlignment.CENTER);
        label.setContentDisplay(ContentDisplay.CENTER);
        label.setAlignment(Pos.CENTER);
        label.setPrefWidth(200);

        window.initModality(Modality.APPLICATION_MODAL);
        window.setHeight(200);
        window.setWidth(250);
        window.setResizable(false);
        VBox layout = new VBox(25);
        buttonPane = new HBox( 15);

        layout.setAlignment(Pos.CENTER);
        buttonPane.setAlignment(Pos.CENTER);
        layout.getChildren().addAll(label,buttonPane);

        Scene scene = new Scene(layout);
        window.setScene(scene);

        buttonOk.setOnAction(e -> {
            result = true;
            window.hide();
        });
        buttonCancel.setOnAction(e -> {
            result = false;
            window.hide();
        });
    }
    public void show (String title, String message, Type type){
        if (window.isShowing()) return;
        buttonCancel.setText("Cancel");
        result = false;
        buttonOk.setDisable(false);
        window.setTitle(title);
        label.setText(message);
        buttonPane.getChildren().clear();
        buttonPane.getChildren().add(buttonOk);
        if (type.equals(Type.CONFIRMATION)){
            buttonPane.getChildren().add(buttonCancel);
        }
        window.showAndWait();
    }

    public void show (String title, String message, Type type, String name){
        result = false;
        buttonOk.setDisable(false);
        buttonCancel.setText(name);
        window.setTitle(title);
        label.setText(message);
        buttonPane.getChildren().clear();
        buttonPane.getChildren().add(buttonOk);
        if (type.equals(Type.CONFIRMATION)){
            buttonPane.getChildren().add(buttonCancel);
        }
        window.showAndWait();
    }

    public void show (String title, String message, Type type, boolean buttonOkDisabled){
        if (window.isShowing()) return;
        buttonCancel.setText("Cancel");
        buttonOk.setDisable(buttonOkDisabled);
        result = false;
        window.setTitle(title);
        label.setText(message);
        buttonPane.getChildren().clear();
        buttonPane.getChildren().add(buttonOk);
        if (type.equals(Type.CONFIRMATION)){
            buttonPane.getChildren().add(buttonCancel);
        }
        window.showAndWait();
    }

    public boolean getResult(){
        return result;
    }

}
