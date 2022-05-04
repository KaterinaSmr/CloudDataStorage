package client;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import javafx.stage.Modality;
import javafx.stage.Stage;


public class InputWindow {
    private Stage window;
    private Label label;
    private TextField inputField;
    private Button buttonOk;
    private Button buttonCancel;
    private VBox layout;
    private HBox buttonPane;
    private String result;

    public InputWindow(){
        this.window = new Stage();
        this.label = new Label();
        this.buttonOk = new Button("OK");
        this.buttonCancel = new Button("Cancel");
        this.inputField = new TextField();
        buttonOk.setPrefWidth(80);
        buttonCancel.setPrefWidth(80);

        label.setWrapText(true);
        label.setTextAlignment(TextAlignment.CENTER);
        label.setPrefWidth(175);
        inputField.setMaxWidth(175);

        window.initModality(Modality.APPLICATION_MODAL);
        window.setHeight(200);
        window.setWidth(250);
        window.setResizable(false);
        layout = new VBox(15);
        layout.setPadding(new Insets(30,30,30,30));
        buttonPane = new HBox( 15);
//        buttonPane.setPadding(new Insets(20,0,0,0));

        layout.setAlignment(Pos.CENTER);
        buttonPane.setAlignment(Pos.CENTER);
        buttonPane.getChildren().addAll(buttonOk, buttonCancel);
        layout.getChildren().addAll(label,inputField,buttonPane);

        Scene scene = new Scene(layout);
        window.setScene(scene);

        buttonOk.setOnAction(e -> {
            result = inputField.getText();
            window.hide();
        });
        buttonCancel.setOnAction(e -> {
            result = null;
            window.hide();
        });
    }
    public void show (String title, String message, String defaultValue){
        result = null;
        window.setTitle(title);
        label.setText(message);
        inputField.setText(defaultValue);
        inputField.requestFocus();
        window.showAndWait();
    }

    public String getResult(){
        return result;
    }

}

