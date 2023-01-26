package server;

import common.AuthService;
import common.ServerCommands;

import javax.management.InstanceNotFoundException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.sql.*;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

public class Server {
    private ServerSocketChannel serverChannel;
    private SocketChannel clientChannel;
    private final int PORT = 8189;
    private AuthService authService;
    private CopyOnWriteArrayList <ClientHandler> clients;
    private Connection connection;

    public Server() {
        try {
            serverChannel = ServerSocketChannel.open();
            serverChannel.bind(new InetSocketAddress("localhost", PORT));
            serverChannel.configureBlocking(false);
            System.out.println("Server is up");
            connectToDB();
            authService = new AuthService(connection);
            clients = new CopyOnWriteArrayList<>();

            Selector selector = Selector.open();
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);

            while (true){
                selector.select();
                Set<SelectionKey> channels = selector.selectedKeys();
                Iterator <SelectionKey> iterator = channels.iterator();
                while (iterator.hasNext()){
                    SelectionKey key = iterator.next();
                    if (key.isAcceptable()){
                        clientChannel = serverChannel.accept();
                        if (clientChannel != null) {
                            System.out.println("Client connected");
                            clientChannel.configureBlocking(false);
                            clientChannel.register(selector, SelectionKey.OP_READ);
                            clients.add(new ClientHandler(this, clientChannel, authService));
                        }
                    } else if (key.isReadable()){
                        SocketChannel temp = (SocketChannel) key.channel();
                        for (ClientHandler c : clients) {
                            if (c.getSocketChannel().equals(temp) && c.getSocketChannel().isConnected()) {
                                c.read();  //дальше работает ClientHandler
                            }
                        }
                    }
                }
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
        c.close();
    }
    private void connectToDB() throws ClassNotFoundException, SQLException {
        Class.forName("org.sqlite.JDBC");
        connection = DriverManager.getConnection("jdbc:sqlite:src/main/resources/server/clientData.db");
    }

    public AuthService getAuthService(){
        return authService;
    }

    private void end(){
        try {
            for (ClientHandler c:clients) {
                c.sendMessage(c.getSocketChannel(), ServerCommands.INFO, "Server down");
            }
            serverChannel.close();
            connection.close();
        } catch (IOException | SQLException e) {
            e.printStackTrace();
        }
    }

}
