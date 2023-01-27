package client;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import common.*;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;


public class LoginWindow implements ServerCommands, ChannelDataExchanger {
    @FXML
    TextField loginField;
    @FXML
    PasswordField passwordField;
    @FXML
    Button btnLogin;
    @FXML
    Button btnSignUp;
    @FXML
    Label label;
    private SocketChannel socketChannel;
    private final String ADDR = "localhost";
    private final int PORT = 8189;

    @FXML
    public void onLogin (){
        System.out.println("channel is open " + socketChannel.isOpen());

        if (!socketChannel.isOpen()){
            if (!initSocketChannel()){
                label.setText("Server unavailable.Please try again later.");
                return;
            }
        }
        try {
            sendMessage(socketChannel, AUTH, loginField.getText(), passwordField.getText());
            if (readHeader(socketChannel, COMMAND_LENGTH).startsWith(AUTHSTATUS)) {
                if (readInfo(socketChannel).startsWith(OK))
                    openMainWindow(AUTH + SEPARATOR + loginField.getText() + SEPARATOR + passwordField.getText() + SEPARATOR);
                else {
                    label.setText(readMessage(socketChannel));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void onSignUp(){
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(this.getClass().getResource("signUpWindow.fxml"));
            Scene signUpScene = new Scene(fxmlLoader.load(), 340,300);
            SignUpWindow signUpWindow = fxmlLoader.getController();
            signUpWindow.setSocketChannel(socketChannel);

            Stage signUpStage = new Stage();
            signUpStage.setScene(signUpScene);
            signUpStage.setTitle("Sign up");
            signUpStage.show();
            Stage currentStage = (Stage)btnLogin.getScene().getWindow();

            signUpStage.setOnCloseRequest(windowEvent -> {
                try {
                    System.out.println("Stage is closing");
                    signUpWindow.onExit();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    Platform.exit();
                }
            });
            passwordField.clear();
            label.setText("");
            signUpWindow.setLoginStage(currentStage);
            currentStage.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public LoginWindow() {
           initSocketChannel();
    }

    public boolean initSocketChannel(){
        try {
            socketChannel = SocketChannel.open();
            socketChannel.connect(new InetSocketAddress(ADDR, PORT));
            socketChannel.configureBlocking(true);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private void openMainWindow(String authMessage){
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(this.getClass().getResource("mainWindow.fxml"));
            Scene mainWindowScene = new Scene(fxmlLoader.load(), 700,500);
            MainWindow mainWindow = fxmlLoader.getController();
            mainWindow.setSocketChannel(socketChannel, authMessage);

            Stage mainStage = new Stage();
            mainStage.setScene(mainWindowScene);
            mainStage.setTitle("Welcome!");
            mainStage.setMinHeight(200.0);
            mainStage.setMinWidth(600.0);
            mainStage.show();
            Stage currentStage = (Stage)btnLogin.getScene().getWindow();

            mainStage.setOnCloseRequest(windowEvent -> {
                try {
                    System.out.println("Stage is closing");
                    mainWindow.onExit();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    Platform.exit();
                }
            });
            passwordField.clear();
            label.setText("");
            mainWindow.setLoginStage(currentStage);
            currentStage.close();
            mainWindow.main();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void onExit(){
        try {
            sendMessage(socketChannel, END);
            socketChannel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Login Window is closing");
    }
}
