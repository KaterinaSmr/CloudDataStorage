package server;

public class User {
    private int id;
    private String login;
    private String password;
    private String timestamp;
    private String path;

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
    public String getPath() {
        return path;
    }

    public User(int id, String login, String password, String timestamp, String path) {
        this.id = id;
        this.login = login;
        this.password = password;
        this.timestamp = timestamp;
        this.path = path;
    }


}
