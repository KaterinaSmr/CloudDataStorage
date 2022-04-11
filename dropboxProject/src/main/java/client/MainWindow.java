package client;

import common.FilesTree;
import common.MyObjectInputStream;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import common.ServerCommands;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;


public class MainWindow implements ServerCommands {
    @FXML
    public TreeView <FilesTree> treeView;
    @FXML
    public TableView <FilesTree> tableView;
    @FXML
    private TableColumn columnName;
    @FXML
    private TableColumn columnType;
    @FXML
    private TableColumn columnSize;
    @FXML
    private TableColumn columnTime;
    @FXML
    private Button downloadButton;
    @FXML
    private Button uploadButton;
    @FXML
    private Button refreshButton;
    @FXML
    private Button addFolderButton;
    @FXML
    private Button removeButton;
    @FXML
    private Button renameButton;
    @FXML
    private Button logoutButton;

    private Image iconFolder;
    private TreeItem<FilesTree> rootItem;
    private SocketChannel socketChannel;
    private MyObjectInputStream inObjStream;

    public void main(){
        System.out.println("Method start() is called");
        setupVisualElements();

        try {
            requestFilesTreeRefresh();
            String header = readMessageHeader(COMMAND_LENGTH);
            System.out.println("Header: " + header);
            if (header.startsWith(FILES_TREE)){
                int objectSize = Integer.parseInt(header.split(SEPARATOR)[1]);
                System.out.println("Object size " + objectSize);
                FilesTree filesTree = (FilesTree) inObjStream.readObject(objectSize);
                filesTree.printNode(0);
                refreshFilesTreeAndTable(filesTree);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setSocketChannel(SocketChannel socketChannel) throws IOException {
        this.socketChannel = socketChannel;
        inObjStream = new MyObjectInputStream(socketChannel);
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
        rootItem = buildTreeView(rootNode);
        treeView.setRoot(rootItem);
    }

    private void setSelectedTreeItem (TreeItem<FilesTree> item){
        treeView.getSelectionModel().select(item);
        item.setExpanded(true);
        selectItem();
    }

    private TreeItem<FilesTree> getTreeItemByValue (TreeItem<FilesTree> item, FilesTree value){
        TreeItem<FilesTree> result;
        if (item.getValue() != null && item.getValue().equals(value))
            return item;
        else {
            for (TreeItem<FilesTree> child : item.getChildren()) {
                result = getTreeItemByValue(child, value);
                if (result != null) return result;
            }
        }
        return null;
    }

    public TreeItem<FilesTree> buildTreeView (FilesTree node){
        if (node.isDirectory()) {
            TreeItem<FilesTree> item = new TreeItem<>(node, new ImageView(iconFolder));
//            TreeItem<FilesTree> item = new TreeItem<>(node);
            for (FilesTree f : node.getChildren()) {
                if (f.isDirectory())
                    item.getChildren().add(buildTreeView(f));
            }
            return item;
        }
        return null;
    }
    private void requestFilesTreeRefresh() throws IOException{
        send(GETFILELIST);
    }
    private void send(String s) throws IOException{
        ByteBuffer buffer = ByteBuffer.wrap(s.getBytes());
        socketChannel.write(buffer);
        buffer.clear();
    }
    @FXML
    public void selectItem(){
        TreeItem<FilesTree> item = (TreeItem<FilesTree>) treeView.getSelectionModel().getSelectedItem();
        if (item != null) {
            tableView.getItems().clear();
            FilesTree node = item.getValue();
            System.out.println("Selected: " + item);
            for (FilesTree f:node.getChildren()) {
                tableView.getItems().add(f);
            }
            tableView.sort();
        }
    }

    @FXML
    public void onDownloadButton(){}
    @FXML
    public void onUploadButton(){}
    @FXML
    public void onRenameButton(){}
    @FXML
    public void onRemoveButton(){}
    @FXML
    public void onRefreshButton(){}
    @FXML
    public void onAddFolderButton(){}
    @FXML
    public void onLogoutButton(){}

    public void onExit(){
        try {
            send(END);
            socketChannel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Main Window is closing");
    }

    public void setupVisualElements(){
        //тут мы делаем так, чтобы при двойном клике на папке в табличной части автоматически выделялся
        // соответствующий узел дерева каталогов и мы проваливались в эту папку
        Image iconDownload = new Image(getClass().getResourceAsStream("download_icon.png"),48,48,true,false);
        downloadButton.setGraphic(new ImageView(iconDownload));
        Image iconUpload = new Image(getClass().getResourceAsStream("upload_icon.png"),48,48,true,false);
        uploadButton.setGraphic(new ImageView(iconUpload));
        Image iconAddFolder = new Image(getClass().getResourceAsStream("addFolder_icon.png"),48,48,true,false);
        addFolderButton.setGraphic(new ImageView(iconAddFolder));
        Image iconLogout = new Image(getClass().getResourceAsStream("logout_icon.png"),48,48,true,false);
        logoutButton.setGraphic(new ImageView(iconLogout));
        Image iconRefresh = new Image(getClass().getResourceAsStream("refresh_icon.png"),48,48,true,false);
        refreshButton.setGraphic(new ImageView(iconRefresh));
        Image iconRemove = new Image(getClass().getResourceAsStream("remove_icon.png"),48,48,true,false);
        removeButton.setGraphic(new ImageView(iconRemove));
        Image iconRename = new Image(getClass().getResourceAsStream("rename_icon.png"),48,48,true,false);
        renameButton.setGraphic(new ImageView(iconRename));

        iconFolder = new Image(getClass().getResourceAsStream("folder_icon.png"),20,20,true,false);

        columnName.setCellValueFactory(new PropertyValueFactory<FilesTree, String>("name"));
        columnType.setCellValueFactory(new PropertyValueFactory<FilesTree, String>("type"));
        columnSize.setCellValueFactory(new PropertyValueFactory<FilesTree, Long>("size"));
        columnTime.setCellValueFactory(new PropertyValueFactory<FilesTree, String>("timestamp"));
        tableView.getSortOrder().add(columnType);

        tableView.setRowFactory( tv -> {
            TableRow<FilesTree> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() >= 2) {
                    FilesTree selectedFilesTreeNode = row.getItem();
                    System.out.println("Double clicked row " + selectedFilesTreeNode);
                    TreeItem<FilesTree> selectedTreeItem = getTreeItemByValue(treeView.getRoot(), selectedFilesTreeNode);
                    if (selectedFilesTreeNode.isDirectory())
                        setSelectedTreeItem(selectedTreeItem);
                }
            });
            return row ;
        });
    }


}
