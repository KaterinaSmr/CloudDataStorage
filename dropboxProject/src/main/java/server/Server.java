package server;

import common.AuthService;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    private ServerSocket serverSocket;
    private Socket socket;
    private final int PORT = 8189;
    private AuthService authService;
    private CopyOnWriteArrayList <ClientHandler> clients;
    private Connection connection;
    private ExecutorService executorService;

    public Server() {
        try {
            serverSocket = new ServerSocket(PORT);
            System.out.println("Сервер запущен");
            connectToDB();
            authService = new AuthService(connection);
            clients = new CopyOnWriteArrayList<>();
            executorService = Executors.newCachedThreadPool();

            while (true) {
                socket = serverSocket.accept();
                executorService.submit(()-> {
                    System.out.println("Клиент подключился");
                    clients.add(new ClientHandler(this, socket));
                });
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            end();
        }
    }

    public User authorizeMe(String login, String pass){
        try {
            return authService.getUserByLoginPass(login,pass);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
    public void unSubscribeMe(ClientHandler c){
        clients.remove(c);
    }
    private void connectToDB() throws Exception {
        Class.forName("org.sqlite.JDBC");
        connection = DriverManager.getConnection("jdbc:sqlite:src/main/resources/server/clientData.db");
    }

    private void end(){
        try {
            executorService.shutdownNow();
            serverSocket.close();
            connection.close();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

}
