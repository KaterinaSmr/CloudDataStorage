package server;

import common.*;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.sql.SQLException;

public class ClientHandler implements ServerCommands, ChannelDataExchanger {
    private Server serverChannel;
    private SocketChannel socketChannel;
    private AuthService authService;
    private User user;
    private MyObjectOutputStream outObjStream;
    private File mainDirectory;
    private FilesTree rootNode;

    public ClientHandler(Server serverChannel, SocketChannel socketChannel, AuthService authService)  throws IOException {
            this.serverChannel = serverChannel;
            this.socketChannel = socketChannel;
            this.outObjStream = new MyObjectOutputStream(socketChannel);
            this.authService =  authService;
    }
    public void read(){
        try {
            String header = readHeader(socketChannel, COMMAND_LENGTH);
            System.out.println("Echo: " + header);
            if (header.startsWith(AUTH)) {
                String msg = readMessage(socketChannel);
                user = serverChannel.authorizeMe(msg.split(SEPARATOR)[0], msg.split(SEPARATOR)[1]);
                if (user != null) {
                    System.out.println("login ok " + user.getId());
                    sendMessage(socketChannel, AUTHSTATUS,OK);
                    mainDirectory = user.getPath().toFile();
                } else sendMessage(socketChannel, AUTHSTATUS, NOK,"Wrong login/password");
            } else if (header.startsWith(GETFILELIST)) {
                sendMessage(socketChannel, FILES_TREE, OK);
                sendFilesTree();
            } else if (header.startsWith(RENAME)) {
                String[] strings = readMessage(socketChannel).split(SEPARATOR);
                rename(strings[0], strings[1]);
            } else if (header.startsWith(REMOVE)) {
                String[] strings = readMessage(socketChannel).split(SEPARATOR);
                remove(strings[0]);
            } else if (header.startsWith(NEWFOLDER)) {
                String[] strings = readMessage(socketChannel).split(SEPARATOR);
                createFolder(strings[0], strings[1]);
            } else if (header.startsWith(DOWNLOAD)) {
                String[] strings = readMessage(socketChannel).split(SEPARATOR);
                int filesQty = countFiles(strings[0]);
                System.out.println("qty of files to send: " + filesQty);
                sendMessage(socketChannel, DOWNLCOUNT, OK, Integer.toString(filesQty));
                sendFiles(strings[0]);
            } else if (header.startsWith(UPLOAD)) {
                String path = readInfo(socketChannel);
                String fileName = readInfo(socketChannel);
                int fileSize = Integer.parseInt(readInfo(socketChannel));
                System.out.println("Path: " + path + " | fileName: " + fileName + " | fileSize: " + fileSize);
                downloadFile(path, fileName, fileSize);
            } else if (header.startsWith(LOGOUT)) {
                logoutUser();
            } else if(header.startsWith(SIGNUP)){
                String[] strings = readMessage(socketChannel).split(SEPARATOR);
                String login = strings[0];
                String pass = strings[1];
                registerNewUser(login,pass);
            } else if (header.startsWith(END)) {
                serverChannel.unSubscribeMe(this);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

//    public void sendMessage(String ... str){
//        String message = "";
//        for (String s: str) {
//            message += s + SEPARATOR;
//        }
//        ByteBuffer buffer = ByteBuffer.wrap(message.getBytes());
//        buffer.rewind();
//        try {
//            socketChannel.write(buffer);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        buffer.clear();
//    }

    private void registerNewUser(String login, String pass) {
        try {
            if (authService.loginIsBusy(login)) {
                sendMessage(socketChannel, SIGNUPSTA, NOK, "Login " + login + " is already taken.");
                return;
            }
            String path = authService.registerNewUser(login, pass);
            if (path == null){
                sendMessage(socketChannel, SIGNUPSTA, NOK, UNKNOWN);
                return;
            }
            File rootDirectory = new File(path);
            if (rootDirectory.mkdirs())
                sendMessage(socketChannel, SIGNUPSTA, OK);
            else sendMessage(socketChannel, SIGNUPSTA, NOK, UNKNOWN);
        } catch (SQLException e) {
            e.printStackTrace();
            sendMessage(socketChannel, SIGNUPSTA, NOK, UNKNOWN);
        }
    }

    private void sendFilesTree() throws IOException{
        rootNode = new FilesTree(mainDirectory, user.getLogin());
        outObjStream.writeObject(rootNode);
    }
    private void sendFilesTree(FilesTree node) throws IOException{
        outObjStream.writeObject(node);
    }

    private void logoutUser(){
        sendMessage(socketChannel, LOGOUTOK, "");
        mainDirectory = null;
        rootNode = null;
        user = null;
    }

    private void createFolder(String parent, String name){
        rootNode = new FilesTree(mainDirectory, user.getLogin());
        FilesTree parentTree = rootNode.validateFile(parent);
        if (parentTree != null) {
            File newFolder = new File(parentTree.getFile().getAbsolutePath() + "/" + name);
            if (rootNode.validateFile(newFolder.getAbsolutePath()) == null) {
                if (newFolder.mkdir()) {
                    sendMessage(socketChannel, NEWFOLDSTATUS , OK , "", newFolder.getAbsolutePath());
                }
            } else {
                sendMessage(socketChannel, NEWFOLDSTATUS , NOK , "File with name " + name + " already exist in directory " + parentTree.getName());
            }
        } else {
            sendMessage(socketChannel, NEWFOLDSTATUS , NOK , "Operation failed. Please try again later");
        }
    }

    private void sendFiles(String path){
        rootNode = new FilesTree(mainDirectory, user.getLogin());
        FilesTree node2Send = rootNode.validateFile(path);
        if (node2Send == null) {
            sendMessage(socketChannel, DOWNLCOUNT, NOK, "File " + path + " not found");
            return;
        }
        if (node2Send.isDirectory()){
            for (FilesTree f:node2Send.getChildren()){
                sendFiles(f.getFile().getAbsolutePath());
            }
        } else {
            sendSingeFile(node2Send.getFile());
        }
    }

    private void sendSingeFile(File file) {
        try {
            FileInputStream fis = new FileInputStream(file);
            FileChannel fc = fis.getChannel();
            ByteBuffer bufferOut = ByteBuffer.allocate((int) file.length());
            sendMessage(socketChannel, DOWNLSTATUS ,OK , Long.toString(file.length()) , file.getAbsolutePath());
            while (fc.read(bufferOut) > 0 || bufferOut.position() > 0) {
                bufferOut.flip();
                socketChannel.write(bufferOut);
                bufferOut.compact();
            }
            return;
        } catch (IOException e) {
            sendMessage(socketChannel, DOWNLSTATUS , NOK, "File " + file.getName() + " cannot be downloaded. " + UNKNOWN);
        }
    }

    private void downloadFile(String path, String name, int size){
        rootNode = new FilesTree(mainDirectory, user.getLogin());
        FilesTree parent = rootNode.validateFile(path);
        if (parent == null) {
            sendMessage(socketChannel, UPLOADCHECK, NOK , "Upload failed. Parent directory not found on server. Please refresh and try again");
            return;
        }
        String newFilePath = path + "/" + name;
        System.out.println("new file path: " + newFilePath);
        FilesTree checkIfExist = rootNode.validateFile(newFilePath);
        if (checkIfExist != null){
            sendMessage(socketChannel, UPLOADCHECK, NOK, "File " + name + " already exist");
            return;
        }
        sendMessage(socketChannel, UPLOADCHECK,OK,"");
        System.out.println("Start read");
        readFile(size, newFilePath);
        System.out.println("Finish read");
    }

    private void readFile(int fileLength, String path) {
        int bufferSize = Math.min(fileLength, DEFAULT_BUFFER);
        System.out.println("Buffer size = " + bufferSize);
        try {
            File file = new File(path);
            FileOutputStream out = new FileOutputStream(path);
            FileChannel fileChannel = out.getChannel();
            ByteBuffer buffer = ByteBuffer.allocate(bufferSize);
            int n = 0;
            int read = socketChannel.read(buffer);
            int remainingBytes;
            while ((remainingBytes = fileLength - n) > bufferSize){
                n += read;
                buffer.flip();
                fileChannel.write(buffer);
                buffer.clear();
                read = socketChannel.read(buffer);
            }

            if (remainingBytes > 0) {
                ByteBuffer buffer1 = ByteBuffer.allocate(remainingBytes);
                read = socketChannel.read(buffer1);
                buffer1.flip();
                fileChannel.write(buffer1);
                n += read;
                buffer.clear();
            }
            System.out.println("File with " + n + " bytes downloaded");
            sendMessage(socketChannel, UPLOADSTAT, OK, "");
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
            sendMessage(socketChannel, UPLOADSTAT, NOK, "Upload failed. Please try again later.");
        }
    }

    private int countFiles(String path){
        rootNode = new FilesTree(mainDirectory, user.getLogin());
        FilesTree node = rootNode.validateFile(path);
        if (node == null) return 0;
        if (!node.isDirectory()) return 1;
        int count = 0;
        for (FilesTree f:node.getChildren()) {
            count += countFiles(f.getFile().getAbsolutePath());
        }
        return count;
    }

    private void rename(String path, String newName){
        rootNode = new FilesTree(mainDirectory, user.getLogin());
        FilesTree node2Change = rootNode.validateFile(path);
        if (node2Change != null) {
            File file2rename = node2Change.getFile();
            String newPath = path.substring(0, path.length() - file2rename.getName().length()) + newName;
            if (rootNode.validateFile(newPath) != null) {
                sendMessage(socketChannel, RENAMSTATUS, NOK, "File " + newName + " already exist");
                return;
            }
            File newFile = new File(newPath);
            if (file2rename.renameTo(newFile)) {
                FilesTree newNode = new FilesTree(newFile);
                FilesTree parentNode = rootNode.validateFile(newFile.getParentFile().getAbsolutePath());
                parentNode.getChildren().remove(node2Change);
                parentNode.getChildren().add(newNode);
                sendMessage(socketChannel, RENAMSTATUS , OK);
                try {
                    sendFilesTree(newNode);
                    System.out.println("Sent updated node: " + newNode.getFile().getName());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return;
            }
        }
        sendMessage(socketChannel, RENAMSTATUS, NOK , "Renaming failed. Please try again later");
    }

    private void remove(String path){
        rootNode = new FilesTree(mainDirectory, user.getLogin());
        FilesTree node2Remove = rootNode.validateFile(path);
        if (node2Remove != null){
            File file2Remove = node2Remove.getFile();
            boolean remFile = removeDir(file2Remove);
            boolean remTree = rootNode.removeChild(node2Remove);
            if (remFile && remTree) {
                sendMessage(socketChannel, REMSTATUS, OK, "");
                return;
            } else
                sendMessage(socketChannel, REMSTATUS, NOK,"Remove failed. Please try again later");
        } else {
            sendMessage(socketChannel, REMSTATUS, NOK, "File not found on server");
        }
    }

    private boolean removeDir(File file){
        File[] contents = file.listFiles();
        if (contents != null) {
            for (File f : contents) {
                removeDir(f);
            }
        }
        return file.delete();
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
