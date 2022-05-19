package server;

import common.FilesTree;
import common.MyObjectOutputStream;
import common.ServerCommands;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;

public class ClientHandler implements ServerCommands {
    private Server serverChannel;
    private SocketChannel socketChannel;
    private User user;
    private MyObjectOutputStream outObjStream;
    private File mainDirectory;
    private FilesTree rootNode;

    public ClientHandler(Server serverChannel, SocketChannel socketChannel)  throws IOException {
            this.serverChannel = serverChannel;
            this.socketChannel = socketChannel;
            this.outObjStream = new MyObjectOutputStream(socketChannel);
    }
    public void read(){
//        ByteBuffer buffer = ByteBuffer.allocate(256);
        try {
            String header = readHeader(COMMAND_LENGTH + SEPARATOR.length());
            System.out.print("Echo: " + header);
            if (header.startsWith(AUTH)) {
                String msg = readMessage();
                user = serverChannel.authorizeMe(msg.split(SEPARATOR)[0], msg.split(SEPARATOR)[1]);
                if (user != null) {
                    System.out.println("login ok " + user.getId());
                    sendInfo(AUTHOK + SEPARATOR + user.getId());
                    mainDirectory = user.getPath().toFile();
                } else sendInfo("Wrong login/password");
            } else if (header.startsWith(GETFILELIST)) {
                sendFilesTree();
            } else if (header.startsWith(RENAME)) {
                String[] strings = readMessage().split(SEPARATOR);
                rename(strings[0], strings[1]);
            } else if (header.startsWith(REMOVE)) {
                String[] strings = readMessage().split(SEPARATOR);
                remove(strings[0]);
            } else if (header.startsWith(NEWFOLDER)) {
                String[] strings = readMessage().split(SEPARATOR);
                createFolder(strings[0], strings[1]);
            } else if (header.startsWith(DOWNLOAD)) {
                String[] strings = readMessage().split(SEPARATOR);
                int filesQty = countFiles(strings[0]);
                System.out.println("qty of files to send: " + filesQty);
                sendInfo(DOWNLCOUNT + filesQty + SEPARATOR);
                sendFiles(strings[0]);
            } else if (header.startsWith(UPLOAD)) {
                String path = readMessageInfo();
                String fileName = readMessageInfo();
                int fileSize = Integer.parseInt(readMessageInfo());
                System.out.println("Path: " + path + " | fileName: " + fileName + " | fileSize: " + fileSize);
                downloadFile(path, fileName, fileSize);
            } else if (header.startsWith(END)) {
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
        rootNode = new FilesTree(mainDirectory);
        outObjStream.writeObject(rootNode);
    }

    private void createFolder(String parent, String name){
        rootNode = new FilesTree(mainDirectory);
        FilesTree parentTree = rootNode.validateFile(parent);
        if (parentTree != null) {
            File newFolder = new File(parentTree.getFile().getAbsolutePath() + "/" + name);
            if (rootNode.validateFile(newFolder.getAbsolutePath()) == null) {
                if (newFolder.mkdir()) {
                    sendInfo(NEWFOLDSTATUS + OK + SEPARATOR + newFolder.getAbsolutePath());
                }
            } else {
                sendInfo(NEWFOLDSTATUS + "File with name " + name + " already exist in directory " + parentTree.getName());
            }
        } else {
            sendInfo(NEWFOLDSTATUS + "Operation failed. Please try again later");
        }
    }

    private void sendFiles(String path){
        rootNode = new FilesTree(mainDirectory);
        FilesTree node2Send = rootNode.validateFile(path);
        if (node2Send == null) {
            sendInfo(DOWNLSTATUS + NOK + "File " + path + " not found");
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
            sendInfo(DOWNLSTATUS + SEPARATOR + OK + file.length() + SEPARATOR + file.getAbsolutePath() + SEPARATOR);
            while (fc.read(bufferOut) > 0 || bufferOut.position() > 0) {
                bufferOut.flip();
                socketChannel.write(bufferOut);
                bufferOut.compact();
            }
            return;
        } catch (IOException e) {
            sendInfo(DOWNLSTATUS + SEPARATOR + NOK + "Unknown error. File " + file.getName() + " cannot be downloaded. Please try again later.");
        }
    }

    private void downloadFile(String path, String name, int size){
        rootNode = new FilesTree(mainDirectory);
        FilesTree parent = rootNode.validateFile(path);
        if (parent == null) {
            sendInfo(UPLOADSTAT + SEPARATOR + NOK + SEPARATOR + "Upload failed. Parent directory not found on server. Please refresh and try again");
            return;
        }
        String newFilePath = path + "/" + name;
        System.out.println("new file path: " + newFilePath);
        readFile(size, newFilePath);
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
                System.out.println("RRRRead: " + read);
                buffer1.flip();
                fileChannel.write(buffer1);
                n += read;
                buffer.clear();
                System.out.println("Length " + n);
            }
            System.out.println("File with " + n + " bytes downloaded");
            sendInfo(UPLOADSTAT + SEPARATOR + OK);
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
            sendInfo(UPLOADSTAT + SEPARATOR + NOK + SEPARATOR + "Upload failed. Please try again later.");
        }
    }

    private int countFiles(String path){
        rootNode = new FilesTree(mainDirectory);
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
        rootNode = new FilesTree(mainDirectory);
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

    private void remove(String path){
        rootNode = new FilesTree(mainDirectory);
        FilesTree node2Remove = rootNode.validateFile(path);
        if (node2Remove != null){
            File file2Remove = node2Remove.getFile();
            boolean remFile = removeDir(file2Remove);
            boolean remTree = rootNode.removeChild(node2Remove);
            if (remFile && remTree) {
                sendInfo(REMSTATUS + OK);
                return;
            } else
                sendInfo(REMSTATUS + "Remove failed. Please try again later");
        } else {
            sendInfo(REMSTATUS  + "File not found on server");
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

    private String readMessage() throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        socketChannel.read(buffer);
        buffer.flip();
        String s = "";
        while (buffer.hasRemaining()) {
            s += (char) buffer.get();
        }
        return s;
    }

    private String readHeader(int bufferSize) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(bufferSize);
        String s = "";
        socketChannel.read(buffer);
        buffer.flip();
        while (buffer.hasRemaining()) {
            s += (char) buffer.get();
        }
        return s;
    }

    private String readMessageInfo() {
        String str = "";
        try {
            while (!str.endsWith(SEPARATOR)) {
                str += readHeader(1);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return str.substring(0, str.length() - SEPARATOR.length());
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
