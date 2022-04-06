package client;

import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.image.ImageView;
import server.ServerCommands;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class MainWindow implements ServerCommands {
    @FXML
    public TextArea textArea;

//    private DataOutputStream out;
//    private DataInputStream in;
    private SocketChannel socket;

    private class TableEntry{
        ImageView icon;
        String name;
        String type;
        String timestamp;
        int size;

        public ImageView getIcon() {
            return icon;
        }
        public String getName() {
            return name;
        }
        public String getType() {
            return type;
        }
        public String getTimestamp() {
            return timestamp;
        }
        public int getSize() {
            return size;
        }

        public void setIcon(ImageView icon) {
            this.icon = icon;
        }

        public TableEntry(String name, String type, String timestamp, int size) {
            this.icon = null;
            this.name = name;
            this.type = type;
            this.timestamp = timestamp;
            this.size = size;
        }
    }

    public void setSocket(SocketChannel socket) {
        this.socket = socket;
    }

    public void start(){
        try {
            refresh();
            ByteBuffer buffer = ByteBuffer.allocate(256);
            String msg = new String();
            socket.read(buffer);
            buffer.rewind();
            while (buffer.hasRemaining()){
                msg += (char) buffer.get();
            }
            System.out.println(msg);
            buffer.clear();
            if (msg.startsWith(FILELIST)) {
                String[] files = msg.split(SEP);
                for (int i = 1; i < files.length; i++) {
                    textArea.appendText(files[i] + "\n");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void refresh() throws IOException{
        send(GETFILELIST);
    }
    private void send(String s) throws IOException{
        ByteBuffer buffer = ByteBuffer.wrap(s.getBytes());
        socket.write(buffer);
        buffer.clear();
    }
    public void onExit(){
        try {
            send(END);
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Main Window is closing");
    }
}
