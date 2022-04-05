package common;

import server.User;

import java.sql.SQLException;

public interface AuthorizationService {
    public User getUserByLoginPass(String login, String password) throws SQLException;
}
