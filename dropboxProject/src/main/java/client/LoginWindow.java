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
import server.ServerCommands;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;


public class LoginWindow implements ServerCommands {
    @FXML
    TextField loginField;
    @FXML
    PasswordField passwordField;
    @FXML
    Button btnLogin;
    @FXML
    Label label;
    private SocketChannel socket;
    private final String ADDR = "localhost";
    private final int PORT = 8189;
    private DataInputStream in;
    private DataOutputStream out;

    @FXML
    public void onLogin (){
        System.out.println("login pressed");
        try {
            send(AUTH + " " + loginField.getText() + " " + passwordField.getText());

            ByteBuffer buffer = ByteBuffer.allocate(256);
            socket.read(buffer);
            buffer.rewind();
            System.out.println("position " + buffer.position() + " limit : " +  buffer.limit());
            String s = new String();
            while (buffer.hasRemaining()){
                s += (char) buffer.get();
            }

            System.out.println("ответ:  " + s);
            if (s.startsWith(AUTHOK)){
                openMainWindow();
            } else {
                label.setText(s);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public LoginWindow() {
        try {
            socket = SocketChannel.open();
            socket.connect(new InetSocketAddress(ADDR, PORT));
            socket.configureBlocking(true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void openMainWindow(){
        System.out.println("okey, opening main window");
        try {

            FXMLLoader fxmlLoader = new FXMLLoader(this.getClass().getResource("mainWindow.fxml"));
            Scene mainWindowScene = new Scene(fxmlLoader.load(), 600,400);
            MainWindow mainWindow = fxmlLoader.getController();
            mainWindow.setSocket(socket);

            Stage mainStage = new Stage();
            mainStage.setScene(mainWindowScene);
            mainStage.setTitle("Welcome!");
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

            currentStage.close();
            mainWindow.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void send(String s) throws IOException{
        ByteBuffer buffer = ByteBuffer.wrap(s.getBytes());
        socket.write(buffer);
        buffer.clear();

    }

    public void onExit(){
        try {
            send(END);
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Login Window is closing");
    }
}
