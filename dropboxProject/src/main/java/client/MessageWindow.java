package client;

import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class MessageWindow {
    private Stage window;
    private Label label;
    private Button buttonOk;
    private Button buttonCancel;
    private VBox layout;
    private HBox buttonPane;
    public enum MessageType {INFORMATION, CONFIRMATION};
    private boolean result;

    public MessageWindow(){
        this.window = new Stage();
        this.label = new Label();
        this.buttonOk = new Button("OK");
        this.buttonCancel = new Button("Cancel");
        buttonOk.setPrefWidth(80);
        buttonCancel.setPrefWidth(80);

        window.initModality(Modality.APPLICATION_MODAL);
        window.setHeight(180);
        window.setWidth(250);
        window.setResizable(false);
        layout = new VBox(25);
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
    public void show (String title, String message, MessageType type){
        window.setTitle(title);
        label.setText(message);
        buttonPane.getChildren().clear();
        buttonPane.getChildren().add(buttonOk);
        if (type.equals(MessageType.CONFIRMATION)){
            buttonPane.getChildren().add(buttonCancel);

        }
        window.show();
    }

    public boolean getResult(){
        return result;
    }

}
