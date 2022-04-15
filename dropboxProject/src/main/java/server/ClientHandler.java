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
    private File mainDirectory;

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
            StringBuilder sb = new StringBuilder();
            while (buffer.hasRemaining()) {
                sb.append((char) buffer.get());
            }
            buffer.clear();
            String s = sb.toString();
            System.out.println("Echo: " + s);
            if (s.startsWith(AUTH)) {
                user = serverChannel.authorizeMe(s.split(SEPARATOR)[1], s.split(SEPARATOR)[2]);
                if (user != null) {
                    System.out.println("login ok " + user.getId());
                    sendInfo(AUTHOK + SEPARATOR + user.getId());
                    mainDirectory = mainDirectory = user.getPath().toFile();
                } else sendInfo("Wrong login/password");
            } else if (s.startsWith(GETFILELIST)) {
                sendFilesTree();
            } else if (s.startsWith(RENAME)){
                String[] strings = s.split(SEPARATOR);
                rename(strings[1], strings[2]);
            } else if (s.startsWith(END)) {
                serverChannel.unSubscribeMe(this);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendInfo(String s) {
        ByteBuffer buffer = ByteBuffer.wrap(s.getBytes());
        buffer.rewind();
        try {
            socketChannel.write(buffer);
        } catch (IOException e) {
            e.printStackTrace();
        }
        buffer.clear();
    }

    private void sendFilesTree() throws IOException{
        FilesTree rootNode = new FilesTree(mainDirectory);
        outObjStream.writeObject(rootNode);
    }

    private void rename(String path, String newName){
        FilesTree rootNode = new FilesTree(mainDirectory);
        FilesTree node2Change = rootNode.validateFile(path);
        if (node2Change != null) {
            File file2rename = node2Change.getFile();
            String newPath = path.substring(0, path.length() - file2rename.getName().length()) + newName;
            if (rootNode.validateFile(newPath) != null) {
                sendInfo(RENAMSTATUS + "File " + newName + " already exist");
                return;
            }
            File newFile = new File(newPath);
            if (file2rename.renameTo(newFile)) {
                node2Change.setFile(newFile);
                sendInfo(RENAMSTATUS + OK + SEPARATOR + newFile.getAbsolutePath());
                return;
            }
        }
        sendInfo(RENAMSTATUS + "Renaming failed. Please try again later");
    }

    public SocketChannel getSocketChannel() {
        return socketChannel;
    }

    public void close(){
        try {
            socketChannel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
