package common;

import server.User;

import java.sql.*;

public class AuthService implements AuthorizationService{
    private Connection connection;

    public AuthService(Connection conn) {
        this.connection = conn;
    }

    @Override
    public User getUserByLoginPass(String login, String password) throws SQLException {
        PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM clients " +
                "WHERE login = '" + login + "' AND password = '" + password + "';");
        ResultSet rs = preparedStatement.executeQuery();
        if (rs != null && !rs.isClosed()){
            return new User(rs.getInt(1), rs.getString(2),
                    rs.getString(3), rs.getString(4), rs.getString(5));
        }
        return null;
    }
}
