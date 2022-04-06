package server;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class ClientHandler implements ServerCommands {
    private Server serverChannel;
    private SocketChannel socketChannel;
    private User user;

    public ClientHandler(Server serverChannel, SocketChannel socketChannel)  {
        this.serverChannel = serverChannel;
        this.socketChannel = socketChannel;
    }
    public void read(){
        ByteBuffer buffer = ByteBuffer.allocate(256);
        try {
            socketChannel.read(buffer);
            buffer.flip();
            StringBuilder str = new StringBuilder();
            while (buffer.hasRemaining()){
                str.append ((char) buffer.get());
            }
            buffer.clear();
            String s = str.toString();
            System.out.println("Echo: " + s);
            if (s.startsWith(AUTH)) {
                user = serverChannel.authorizeMe(s.split(" ")[1], s.split(" ")[2]);
                if (user != null) {
                    System.out.println("login ok " + user.getId());
                    sendInfo(AUTHOK + " " + user.getId());
                } else sendInfo("Wrong login/password");
            } else if (s.startsWith(GETFILELIST)) {
                String[] filesList = getFilesList();
                String files = FILELIST + SEP;
                for (int i = 0; i < filesList.length; i++) {
                    files += (filesList[i] + SEP);
                }
                sendInfo(files);
            } else if (s.startsWith(END)) {
                serverChannel.unSubscribeMe(this);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendInfo(String s) throws IOException{
        ByteBuffer buffer = ByteBuffer.wrap(s.getBytes());
        buffer.rewind();
        socketChannel.write(buffer);
        buffer.clear();
    }

    private void close(){
        try {
            socketChannel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String[] getFilesList(){
        File mainDirectory = new File(user.getPath());
        return mainDirectory.list();
    }

    public SocketChannel getSocketChannel() {
        return socketChannel;
    }
}
