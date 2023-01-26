package common;

import server.User;

import java.io.File;
import java.sql.*;

public class AuthService {
    private final Connection connection;
    private final String defaultPathToStorage = "D:/Projects/j4DB/";

    public AuthService(Connection conn) {
        this.connection = conn;
    }

    public User getUserByLoginPass(String login, String password) throws SQLException {
        PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM clients " +
                "WHERE login = '" + login + "' AND password = '" + password + "';");
        ResultSet rs = preparedStatement.executeQuery();
        if (rs != null && !rs.isClosed()){
            User user = new User(rs.getInt(1), rs.getString(2),
                    rs.getString(3), rs.getString(4), rs.getString(5));
            File rootDirectory = user.getPath().toFile();
            if (!rootDirectory.exists())
                rootDirectory.mkdirs();
            return user;
        }
        return null;
    }

    public boolean loginIsBusy(String login) throws SQLException{
        PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM clients " +
                "WHERE login = '" + login +"';");
        ResultSet rs = preparedStatement.executeQuery();
        return rs != null && !rs.isClosed();
    }

    public String registerNewUser(String login, String password) throws SQLException{
        PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO clients (login, password) VALUES ('" + login + "','" + password + "');");
        preparedStatement.executeUpdate();
        preparedStatement = connection.prepareStatement("SELECT id FROM clients WHERE login = '" + login + "';");
        ResultSet rs1 = preparedStatement.executeQuery();
        int id = 0;
        while (rs1.next()){
            id = rs1.getInt(1);
        }

        String pathToStorage = defaultPathToStorage + id;
        preparedStatement = connection.prepareStatement("UPDATE clients SET path = '" + pathToStorage + "'  WHERE id = " + id + ";");
        if (preparedStatement.executeUpdate() < 1)
            return null;
        return pathToStorage;
    }
}
