package server;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

public class User {
    private final int id;
    private final String login;
    private final String password;
    private String timestamp;
    private Path path;

    public int getId() {
        return id;
    }
    public String getLogin() {
        return login;
    }
    public String getPassword() {
        return password;
    }
    public String getTimestamp() {
        return timestamp;
    }
    public Path getPath() {
        return path;
    }

    public User(int id, String login, String password, String timestamp, String path) {
        this.id = id;
        this.login = login;
        this.password = password;
        this.timestamp = timestamp;
        this.path = Paths.get(path);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return id == user.id && Objects.equals(login, user.login);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, login);
    }
}
