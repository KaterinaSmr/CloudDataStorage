package client;

import common.FilesTree;
import common.MyObjectInputStream;
import common.ServerCommands;
import javafx.application.Platform;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.Exchanger;


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
    private SocketChannel socketChannel;
    private MyObjectInputStream inObjStream;
    private FilesTree filesTree = null;
    private TreeItem<FilesTree> rootItem;
    private Exchanger<String> statusExchanger;

    public void main(){
        System.out.println("Method start() is called");
        statusExchanger = new Exchanger<>();
        setupVisualElements();

        try {
            requestFilesTreeRefresh();
        } catch (Exception e) {
            e.printStackTrace();
        }

       new Thread(()->{
            try {
                while (true) {
                    String header = readMessageHeader(COMMAND_LENGTH);
                    System.out.println("Header: " + header);
                    if (header.startsWith(FILES_TREE)){
                        int objectSize = Integer.parseInt(header.split(SEPARATOR)[1]);
                        System.out.println("Object size " + objectSize);
                        try {
                            filesTree = (FilesTree) inObjStream.readObject(objectSize);
                        } catch (ClassNotFoundException e) {
                            e.printStackTrace();
                        }
                        Platform.runLater(()->{
                            refreshFilesTreeAndTable(filesTree);
                        });
                    } else if (header.startsWith(RENAMSTATUS)){
                        String msg = readMessage();
                        try {
                            String resp = statusExchanger.exchange(msg);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    } else if (header.startsWith(INFO)){
                        //отобразить сообщение во всплывающем окошке
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }).start();
    }

    private String readMessage() throws IOException{
        ByteBuffer buffer = ByteBuffer.allocate(128);
        socketChannel.read(buffer);
        buffer.flip();
        String s = "";
        System.out.println("Buffer limit: " + buffer.limit());
        while (buffer.hasRemaining()){
            s += (char) buffer.get();
        }
        return s;
    }

    public void setSocketChannel(SocketChannel socketChannel) throws IOException {
        this.socketChannel = socketChannel;
        inObjStream = new MyObjectInputStream(socketChannel);
    }

    private String readMessageHeader(int bufferSize) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(bufferSize);
        String s = "";
        socketChannel.read(buffer);
        buffer.flip();
        while (buffer.hasRemaining()){
            s += (char) buffer.get();
        }
        return s;
    }

    public void refreshFilesTreeAndTable(FilesTree rootNode) {
        tableView.getItems().clear();
        System.out.println("refresh");
        rootItem = buildTreeView(rootNode);
        treeView.setRoot(rootItem);
    }

    private void setSelectedTreeItem (TreeItem<FilesTree> item){
        treeView.getSelectionModel().select(item);
        item.setExpanded(true);
        selectItem();
    }

    private TreeItem<FilesTree> getTreeItemByValue (TreeItem<FilesTree> treeRoot, FilesTree value){
        TreeItem<FilesTree> result;
        if (treeRoot.getValue() != null && treeRoot.getValue().equals(value))
            return treeRoot;
        else {
            for (TreeItem<FilesTree> child : treeRoot.getChildren()) {
                result = getTreeItemByValue(child, value);
                if (result != null) return result;
            }
        }
        return null;
    }

    public TreeItem<FilesTree> buildTreeView (FilesTree node){
        if (node.isDirectory()) {
            TreeItem<FilesTree> item = new TreeItem<>(node, new ImageView(iconFolder));
            for (FilesTree f : node.getChildren()) {
                if (f.isDirectory())
                    item.getChildren().add(buildTreeView(f));
            }
            return item;
        }
        return null;
    }
    private void requestFilesTreeRefresh(){
        send(GETFILELIST);
    }
    private void requestRename(String path, String newName){
        String str = RENAME + SEPARATOR + path + SEPARATOR + newName;
        send(str);
    }

    private void send(String s) {
        ByteBuffer buffer = null;
        try {
            buffer = ByteBuffer.wrap(s.getBytes());
            socketChannel.write(buffer);
        } catch (IOException e) {
            e.printStackTrace();
        }
        buffer.clear();
    }
    @FXML
    public void selectItem(){
        TreeItem<FilesTree> item = (TreeItem<FilesTree>) treeView.getSelectionModel().getSelectedItem();
        if (item != null) {
            tableView.getItems().clear();
            FilesTree node = item.getValue();
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
    public void onRenameButton(){
    // fire TableColumn.CellEditEvent>
//        если выделена строка - получить позицию и текущее значение
        TablePosition pos = tableView.getSelectionModel().getSelectedCells().get(0);
        TablePosition posNew = new TablePosition(tableView, pos.getRow(), columnName);
        System.out.println("selected row: " + pos.getRow() + " - " + posNew.getRow());
        System.out.println("selected column: " + pos.getColumn() + " - " + posNew.getColumn());
        TableColumn.CellEditEvent<FilesTree, String> event = new TableColumn.CellEditEvent<FilesTree, String>(
                tableView, posNew, TableColumn.editAnyEvent(), tableView.getItems().get(pos.getRow()).getName());
        Event.fireEvent(tableView, event);

    }
    @FXML
    public void onRemoveButton(){}
    @FXML
    public void onRefreshButton(){
        requestFilesTreeRefresh();
    }
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
        iconFolder = new Image(getClass().getResourceAsStream("folder_icon.png"),20,20,true,false);
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

        //тут мы делаем так, чтобы при двойном клике на папке в табличной части автоматически выделялся
        // соответствующий узел дерева каталогов и мы проваливались в эту папку
        columnName.setCellValueFactory(new PropertyValueFactory<FilesTree, String>("name"));
        columnType.setCellValueFactory(new PropertyValueFactory<FilesTree, String>("type"));
        columnSize.setCellValueFactory(new PropertyValueFactory<FilesTree, Long>("size"));
        columnTime.setCellValueFactory(new PropertyValueFactory<FilesTree, String>("timestamp"));
        tableView.getSortOrder().add(columnType);
        columnName.setCellFactory(TextFieldTableCell.<FilesTree>forTableColumn());

        columnName.setOnEditStart(new EventHandler<TableColumn.CellEditEvent>() {
            @Override
            public void handle(TableColumn.CellEditEvent cellEditEvent) {
                System.out.println("Edit start");
            }
        });
        columnName.setOnEditCommit(
                new EventHandler<TableColumn.CellEditEvent>() {
                    @Override
                    public void handle(TableColumn.CellEditEvent t) {
                        FilesTree changedNode = ((FilesTree) t.getTableView().getItems().get(t.getTablePosition().getRow()));
                        System.out.println("changed node " + changedNode);
                        String s = (String) t.getNewValue();
                        if (s.equals(changedNode.getName())) {
                            System.out.println("Name is the same");
                            return;
                        }
                        requestRename(changedNode.getFile().getAbsolutePath(), s);
                        String resp = "";
                        try {
                            resp = statusExchanger.exchange("ok");
                            System.out.println("thread Main got msg: " + resp);
                            if (resp.startsWith(OK)) {
                                System.out.println(resp);
                                String[] newPath = resp.split(SEPARATOR);
                                File newFile = new File(newPath[1]);
                                changedNode.setFile(newFile);
                                treeView.refresh();
                            } else {
                                System.out.println(resp);
                            }
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
        );

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
