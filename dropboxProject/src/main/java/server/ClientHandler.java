package server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.Socket;

public class ClientHandler implements ServerCommands {
    private Server server;
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private User user;

    public ClientHandler(Server server, Socket socket)  {
        this.server = server;
        this.socket = socket;
        try {
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());
            while (true) {
                String s = in.readUTF();
                System.out.println("Echo: " + s);
                if (s.startsWith(AUTH)) {
                    user = server.authorizeMe(s.split(" ")[1], s.split(" ")[2]);
                    if (user != null) {
                        System.out.println("login ok " + user.getId());
                        sendInfo(AUTHOK + user.getId());
                    } else sendInfo("Wrong login/password");
                } else if (s.startsWith(GETFILELIST)){
                    String[] filesList = getFilesList();
                    String files = FILELIST + SEP;
                    for (int i = 0; i < filesList.length; i++) {
                        files += (filesList[i] + SEP);
                    }
                    sendInfo(files);
                } else if (s.startsWith(END)){
                    server.unSubscribeMe(this);
                    break;
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            close();
        }
    }

    public void sendInfo(String s) throws IOException{
        out.writeUTF(s);
        out.flush();
    }
    private void close(){
        try {
            in.close();
            out.close();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String[] getFilesList(){
        File mainDirectory = new File(user.getPath());
        return mainDirectory.list();
    }
}
