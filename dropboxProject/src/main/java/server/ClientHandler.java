package server;

import common.FilesTree;
import common.MyObjectOutputStream;
import common.ServerCommands;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class ClientHandler implements ServerCommands {
    private Server serverChannel;
    private SocketChannel socketChannel;
    private User user;
    private MyObjectOutputStream outObjStream;

    public ClientHandler(Server serverChannel, SocketChannel socketChannel)  throws IOException {
            this.serverChannel = serverChannel;
            this.socketChannel = socketChannel;
            this.outObjStream = new MyObjectOutputStream(socketChannel);
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
                sendFilesTree();
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

    private void sendFilesTree() throws IOException{
        File mainDirectory = user.getPath().toFile();
        FilesTree rootNode = new FilesTree(mainDirectory);
        outObjStream.writeObject(rootNode);
    }

    public SocketChannel getSocketChannel() {
        return socketChannel;
    }

    private void close(){
        try {
            outObjStream.close();
            socketChannel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
