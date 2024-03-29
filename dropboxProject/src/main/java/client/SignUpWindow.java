package client;

import common.ChannelDataExchanger;
import common.ServerCommands;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.nio.channels.SocketChannel;

public class SignUpWindow implements ServerCommands, ChannelDataExchanger {
    @FXML
    Button btnSignUp;
    @FXML
    Button btnCancel;
    @FXML
    TextField txtLogin;
    @FXML
    PasswordField passwordField;
    @FXML
    PasswordField repeatPswField;
    @FXML
    Label label;

    private SocketChannel socketChannel;
    private Stage loginStage;
    private MessageWindow messageWindow;

    @FXML
    private void onSignUp(){
        String login = txtLogin.getText();
        String pass1 = passwordField.getText();
        String pass2 = repeatPswField.getText();
        if (login == null || pass1 == null || pass2 == null)
            return;
        if (!pass1.equals(pass2)){
            Platform.runLater(()-> {
                messageWindow.show("Check fail", "The passwords don't match", MessageWindow.Type.INFORMATION);
            });
            return;
        }

        try {
            sendMessage(socketChannel, SIGNUP, login, pass1);
            String header = readHeader(socketChannel, COMMAND_LENGTH);
            if (header.startsWith(SIGNUPSTA)) {
                if (readInfo(socketChannel).startsWith(OK)) {
                    Platform.runLater(() -> {
                        messageWindow.show("Congratulations!", "Registration is finished. \n You may now sign in.", MessageWindow.Type.INFORMATION);
                    });
                    txtLogin.clear();
                    passwordField.clear();
                    repeatPswField.clear();
                    ((Stage) btnSignUp.getScene().getWindow()).close();
                    loginStage.show();
                } else {
                    label.setText(readMessage(socketChannel));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    @FXML
    private void onCancel(){
        txtLogin.clear();
        passwordField.clear();
        repeatPswField.clear();
        loginStage.show();
        ((Stage) btnCancel.getScene().getWindow()).close();
    }

    public void setSocketChannel(SocketChannel socketChannel) throws IOException {
        messageWindow = new MessageWindow();
        this.socketChannel = socketChannel;
    }

    public void onExit() {
        try {
            sendMessage(socketChannel, END);
            socketChannel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Sign up window is closing");
    }

    public void setLoginStage(Stage stage){
        loginStage = stage;
    }

}
