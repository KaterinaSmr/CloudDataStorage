package client;

import common.FilesTree;
import common.MyObjectInputStream;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import server.ServerCommands;
import java.io.*;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.file.Path;
import java.nio.file.Paths;


public class MainWindow implements ServerCommands {
    @FXML
    public TreeView treeView;
    @FXML
    public TableView tableView;
    @FXML
    private TableColumn columnName;
    @FXML
    private TableColumn columnType;
    @FXML
    private TableColumn columnSize;
    @FXML
    private TableColumn columnTime;

    private Image image;
    private TreeItem<FilesTree> rootItem;
    private SocketChannel socketChannel;
    private MyObjectInputStream inObjStream;

    public void setSocketChannel(SocketChannel socketChannel) throws IOException {
        this.socketChannel = socketChannel;
        inObjStream = new MyObjectInputStream(socketChannel);
    }

    public void start(){
        System.out.println("Method start() is called");
        try {
            requestFilesTreeRefresh();
            String header = readMessageHeader(COMMAND_LENGTH);
            System.out.println("Header: " + header);
            if (header.startsWith(FILES_TREE)){
                int objectSize = Integer.parseInt(header.split(SEP)[1]);
                System.out.println("Object size " + objectSize);
                FilesTree filesTree = (FilesTree) inObjStream.readObject(objectSize);
                filesTree.printNode(0);
                refreshFilesTreeAndTable(filesTree);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String readMessageHeader(int bufferSize) throws IOException {
        ByteBuffer buffer0 = ByteBuffer.allocate(bufferSize);
        String s = "";
        socketChannel.read(buffer0);
        buffer0.rewind();
        while (buffer0.hasRemaining()){
            s += (char) buffer0.get();
        }
        return s;
    }

    public void refreshFilesTreeAndTable(FilesTree rootNode) {
        image = new Image(getClass().getResourceAsStream("folder_icon.png"),20,20,true,false);

        rootItem = buildTreeView(rootNode);
        treeView.setRoot(rootItem);

        columnName.setCellValueFactory(new PropertyValueFactory<FilesTree, String>("name"));
        columnType.setCellValueFactory(new PropertyValueFactory<FilesTree, String>("type"));
        columnSize.setCellValueFactory(new PropertyValueFactory<FilesTree, Long>("size"));
        columnTime.setCellValueFactory(new PropertyValueFactory<FilesTree, String>("timestamp"));
        tableView.getSortOrder().add(columnType);
    }

    public TreeItem<FilesTree> buildTreeView (FilesTree node){
        if (node.isDirectory()) {
            TreeItem<FilesTree> item = new TreeItem<>(node, new ImageView(image));
//            TreeItem<FilesTree> item = new TreeItem<>(node);
            for (FilesTree f : node.getChildren()) {
                if (f.isDirectory())
                    item.getChildren().add(buildTreeView(f));
            }
            return item;
        }
        return null;
    }

    @FXML
    public void selectItem(){
        TreeItem<FilesTree> item = (TreeItem<FilesTree>) treeView.getSelectionModel().getSelectedItem();
        if (item != null) {
            tableView.getItems().clear();
            FilesTree node = item.getValue();
            System.out.println(item);
            for (FilesTree f:node.getChildren()) {
                tableView.getItems().add(f);
            }
            tableView.sort();
        }
    }
    private void requestFilesTreeRefresh() throws IOException{
        send(GETFILELIST);
    }
    private void send(String s) throws IOException{
        ByteBuffer buffer = ByteBuffer.wrap(s.getBytes());
        socketChannel.write(buffer);
        buffer.clear();
    }
    public void onExit(){
        try {
            send(END);
            socketChannel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Main Window is closing");
    }


}
