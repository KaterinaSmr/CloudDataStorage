package common;

import server.User;

import java.sql.SQLException;

public interface AuthorizationService {
    User getUserByLoginPass(String login, String password) throws SQLException;
}
