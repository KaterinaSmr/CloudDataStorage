package client;

import common.ChannelDataExchanger;
import common.FilesTree;
import common.MyObjectInputStream;
import common.ServerCommands;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import javafx.scene.robot.Robot;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainWindow implements ServerCommands, ChannelDataExchanger {
    @FXML
    VBox mainPane;
    @FXML
    public TreeView<FilesTree> treeView;
    @FXML
    public TableView<FilesTree> tableView;
    @FXML
    private TableColumn<FilesTree, String> columnType;
    @FXML
    private TableColumn<FilesTree, Long> columnSize;
    @FXML
    private TableColumn<FilesTree, String> columnTime;
    @FXML
    private TableColumn<FilesTree, ImageView> subColumnIcon;
    @FXML
    private TableColumn<FilesTree, String> subColumnName;
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
    @FXML
    private VBox progressBox;
    @FXML
    private ImageView progressImageView;

    private Image iconFolder;
    private Image iconFile;
    private SocketChannel socketChannel;
    private MyObjectInputStream inObjStream;
    private FilesTree filesTree = null;
    private FilesTree tempNode = null;
    private Exchanger<String> statusExchanger;
    private MessageWindow messageWindow;
    private String downloadPath = "D:/Downloads/";
    private String nodeParentPath;
    private CountDownLatch waitingAllFiles;
    private int countFiles;
    private Stage loginStage;
    private AtomicBoolean isLoggedIn = new AtomicBoolean(false);

    public void main() {
        statusExchanger = new Exchanger<>();
        isLoggedIn.set(true);
        setupVisualElements();

        try {
            requestFilesTreeRefresh();
        } catch (Exception e) {
            e.printStackTrace();
        }

        Thread t1 = new Thread(() -> {
                while (isLoggedIn.get()) {
                    try {
                        String header = readHeader(socketChannel, COMMAND_LENGTH).substring(0,10);
                        System.out.println("Header: " + header);
                        String status = readInfo(socketChannel);
                        System.out.println("Status: " + status);
                        String exchangeMessage = status;
                        switch (status) {
                            case OK:
                                String infoMsg = readInfo(socketChannel);
                                int info = 0;
                                System.out.println("Info msg: " + infoMsg);
                                if (infoMsg != null && !infoMsg.equals("")) {
                                    info = Integer.parseInt(infoMsg);
                                }
                                switch (header) {
                                    case FILES_TREE -> {//info = object size
                                        filesTree = (FilesTree) inObjStream.readObject(info);
                                        setIcons(filesTree);
                                        Platform.runLater(() -> {
                                            refreshFilesTreeAndTable(filesTree);
                                        });
                                        continue;
                                    }
                                    case RENAMSTATUS -> {//info = object size
                                        tempNode = (FilesTree) inObjStream.readObject(info);
                                        setIcons(tempNode);
                                    }
                                    case NEWFOLDSTATUS -> exchangeMessage = readMessage(socketChannel);
                                    case DOWNLCOUNT ->// info = qty of files to download
                                            exchangeMessage = Integer.toString(info);
                                    case DOWNLSTATUS -> { //info = filelength
                                        String serverPath = readInfo(socketChannel);
                                        String path = downloadPath + serverPath.substring(nodeParentPath.length() + 1);
                                        downloadFile(info, path);
                                        continue;
                                    }
                                }
                                statusExchanger.exchange(exchangeMessage);
                                break;
                            case NOK:
                                //обработка ошибок
                                String errorMessage = readMessage(socketChannel);
                                System.out.println("Error message: " + errorMessage);
                                Platform.runLater(() -> {
                                    messageWindow.show("Error", errorMessage, MessageWindow.Type.INFORMATION);
                                });
                                statusExchanger.exchange(exchangeMessage);
                                break;
                            default:
                                if (header.startsWith(INFO)) {
                                    String msg = readMessage(socketChannel);
                                    Platform.runLater(() -> {
                                        messageWindow.show("Info", msg, MessageWindow.Type.INFORMATION);
                                    });
                                } else if (header.startsWith(LOGOUT)) {
                                    break;
                                }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        isLoggedIn.set(false);
                        try {
                            socketChannel.close();
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        }
                        Platform.runLater(()->{
                            messageWindow.show("Error", "Server unavailable. Please try to reconnect.", MessageWindow.Type.INFORMATION);
                        });
                        Platform.runLater(()->{
                            Stage thisStage = (Stage) logoutButton.getScene().getWindow();
                            thisStage.close();
                            loginStage.show();
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                        Platform.runLater(() -> {
                            messageWindow.show("Error", "Internal Error. Please try again later", MessageWindow.Type.INFORMATION);
                        });
                    }
                }
            System.out.println("Thread main END");
        });
        t1.setDaemon(true);
        t1.start();
    }

    private void downloadFile(int fileLength, String path) {
        System.out.println("File path: " + path + " | File length " + fileLength);
        int bufferSize = Math.min(fileLength, DEFAULT_BUFFER);
        try {
            File file = new File(path);
            File parentDir = file.getParentFile();
            parentDir.mkdirs();
            FileOutputStream out = new FileOutputStream(path);
            FileChannel fileChannel = out.getChannel();
            ByteBuffer buffer = ByteBuffer.allocate(bufferSize);
            int n = 0;
            int read;
            while ((read = socketChannel.read(buffer)) > 0) {
                buffer.flip();
                fileChannel.write(buffer);
                n += read;
                buffer.clear();
                if ((fileLength - n) < bufferSize) break;
            }
            int remainingBytes = fileLength - n;
            if (remainingBytes > 0) {
                ByteBuffer buffer1 = ByteBuffer.allocate(remainingBytes);
                read = socketChannel.read(buffer1);
                buffer1.flip();
                fileChannel.write(buffer1);
                n += read;
                buffer.clear();
                System.out.println("Length " + n);
            }
            System.out.println("File with " + fileLength + " bytes downloaded");
            countFiles++;
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
            Platform.runLater(() -> {
                progressBox.setVisible(false);
                messageWindow.show("Error", "Error sending file " + filesTree.getName(), MessageWindow.Type.INFORMATION);
            });
        } finally {
            waitingAllFiles.countDown();
        }
    }

    public void setSocketChannel(SocketChannel socketChannel, String authMessage) throws IOException {
        this.socketChannel = socketChannel;
        inObjStream = new MyObjectInputStream(socketChannel);
    }

    private void refreshFilesTreeAndTable(FilesTree rootNode) {
        tableView.getItems().clear();
        TreeItem<FilesTree> rootItem = buildTreeView(rootNode);
        treeView.setRoot(rootItem);
    }

    private void setSelectedTreeItem(TreeItem<FilesTree> item) {
        treeView.getSelectionModel().select(item);
        item.setExpanded(true);
        selectItem();
    }

    private TreeItem<FilesTree> getTreeItemByValue(TreeItem<FilesTree> checkedItem, FilesTree value) {
        TreeItem<FilesTree> result;
        if (checkedItem == null) return null;
        if (checkedItem.getValue().equals(value))
            return checkedItem;
        else {
            for (TreeItem<FilesTree> child : checkedItem.getChildren()) {
                result = getTreeItemByValue(child, value);
                if (result != null) return result;
            }
        }
        return null;
    }

    public TreeItem<FilesTree> buildTreeView(FilesTree node) {
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

    private void requestFilesTreeRefresh() {
        sendMessage(socketChannel, GETFILELIST);
    }

    private void requestRename(String path, String newName) {
        sendMessage(socketChannel,RENAME, path, newName);
    }

    private void requestRemove(String path) {
        sendMessage(socketChannel, REMOVE, path);
    }

    private void removeItem(FilesTree nodeToRemove) {
        String parent = nodeToRemove.getFile().getParent();
        filesTree.removeChild(nodeToRemove);
        TreeItem itemToRemove = getTreeItemByValue(treeView.getRoot(), nodeToRemove);
        if (itemToRemove != null) {
            itemToRemove.getParent().getChildren().remove(itemToRemove);
            treeView.refresh();
        }
        tableView.getItems().removeAll();
        selectItem();
    }


    @FXML
    public void selectItem() {
        TreeItem<FilesTree> item = treeView.getSelectionModel().getSelectedItem();
        if (item != null) {
            tableView.getItems().clear();
            FilesTree node = item.getValue();
            for (FilesTree f : node.getChildren()) {
                tableView.getItems().add(f);
            }
            tableView.sort();
        }
    }

    @FXML
    public void onDownloadButton() {
        countFiles = 0;
        if (tableView.getSelectionModel().getSelectedCells().isEmpty()) {
            messageWindow.show("Warning", "No files selected", MessageWindow.Type.INFORMATION);
            return;
        }
        DirectoryChooser directoryChooser = new DirectoryChooser();
        File selectedDirectory = directoryChooser.showDialog(downloadButton.getScene().getWindow());
        if (selectedDirectory == null) {
            return;
        } else {
            downloadPath = selectedDirectory.getAbsolutePath() + "/";
        }
        System.out.println("Download path: " + downloadPath);
        new Thread(() -> {
            showProgressBar();
            TablePosition pos = tableView.getSelectionModel().getSelectedCells().get(0);
            FilesTree nodeToDownload = tableView.getItems().get(pos.getRow());
            nodeParentPath = nodeToDownload.getFile().getParent();
            sendMessage(socketChannel, DOWNLOAD , nodeToDownload.getFile().getAbsolutePath());
            int filesCount = 0;

            try {
                String info = statusExchanger.exchange("OK");
                if (info.startsWith(NOK)) return;
                filesCount = Integer.parseInt(info);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (filesCount == 0) {
                hideProgressBar();
                Platform.runLater(() -> {
                    messageWindow.show("Warning", "No files to download", MessageWindow.Type.INFORMATION);
                });
                return;
            }
            waitingAllFiles = new CountDownLatch(filesCount);
            boolean result;
            try {
                result = waitingAllFiles.await(TIMEOUT_MINS, TimeUnit.MINUTES);
            } catch (InterruptedException e) {
                e.printStackTrace();
                result = false;
            }
            boolean result1 = result;
            hideProgressBar();
            Platform.runLater(() -> {
                if (result1) {
                    messageWindow.show("Download", "Download completed. " + countFiles + " files downloaded", MessageWindow.Type.CONFIRMATION, "Open folder");
                    try {
                        if (!messageWindow.getResult())
                            Desktop.getDesktop().open(new File(downloadPath));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    messageWindow.show("Error", "Download awaiting timeout. Please try again later", MessageWindow.Type.INFORMATION);
                }
            });
        }).start();
    }

    @FXML
    public void onUploadButton() {
        TreeItem<FilesTree> currentTreeItem = treeView.getSelectionModel().getSelectedItem();
        if (currentTreeItem == null) {
            messageWindow.show("Error", "Destination folder not selected", MessageWindow.Type.INFORMATION);
            return;
        }
        FilesTree parent = currentTreeItem.getValue();
        FileChooser fileChooser = new FileChooser();
        File selectedFile = fileChooser.showOpenDialog(uploadButton.getScene().getWindow());
        if (selectedFile == null) {
            return;
        } else {
            System.out.println("Selected File: " + selectedFile.getAbsolutePath());
        }

        new Thread(() -> {
            showProgressBar();
            String uploadStatMsg = "";
            try {
                FileInputStream fis = new FileInputStream(selectedFile);
                FileChannel fc = fis.getChannel();
                ByteBuffer bufferOut = ByteBuffer.allocate((int) selectedFile.length());
                sendMessage(socketChannel, UPLOAD ,parent.getFile().getAbsolutePath() , selectedFile.getName() , Long.toString(selectedFile.length()));

                String nameCheckStatus = statusExchanger.exchange(OK);
                if (nameCheckStatus.startsWith(NOK)){
                    return;
                }

                int read = 0;
                while ((read = fc.read(bufferOut)) > 0 || bufferOut.position() > 0) {
                    bufferOut.flip();
                    socketChannel.write(bufferOut);
                    bufferOut.compact();
                }
                String status = statusExchanger.exchange(OK);
                if (status.startsWith(NOK)){
                    hideProgressBar();
                    return;
                }
                String newFilePath = parent.getFile().getAbsolutePath() + "/" + selectedFile.getName();
                File newFile = new File(newFilePath);
                FilesTree newNode = new FilesTree(newFile, false, iconFile);
                parent.addChild(newNode);
                treeView.refresh();
                setSelectedTreeItem(currentTreeItem);
                uploadStatMsg = "Upload finished";
                fis.close();
            } catch (IOException | InterruptedException e) {
                uploadStatMsg = "Upload failed. Please try again later";
                e.printStackTrace();
            } finally {
                String status = uploadStatMsg;
                hideProgressBar();
                Platform.runLater(() -> {
                    messageWindow.show("Info", status, MessageWindow.Type.INFORMATION);
                });
            }
        }).start();
    }

    @FXML
    public void onRenameButton() {
        if (tableView.getSelectionModel().getSelectedCells().isEmpty()) {
            messageWindow.show("Warning", "No files selected", MessageWindow.Type.INFORMATION);
            return;
        }
        tableView.requestFocus();
        Robot robot = new Robot();
        robot.keyPress(KeyCode.ENTER);
    }

    @FXML
    public void onRemoveButton() {
        FilesTree nodeToRemove;

        if (tableView.getSelectionModel().getSelectedCells().isEmpty()) {
            messageWindow.show("Warning", "No files selected", MessageWindow.Type.INFORMATION);
            return;
        } else {
            TablePosition pos = tableView.getSelectionModel().getSelectedCells().get(0);
            nodeToRemove = tableView.getItems().get(pos.getRow());
            if (nodeToRemove.equals(filesTree)) {
                messageWindow.show("Removal failed", "Cannot remove root folder", MessageWindow.Type.INFORMATION);
                return;
            }
            messageWindow.show("Please confirm", "Are you sure to remove '" + nodeToRemove.getName() + (nodeToRemove.isDirectory() ? "' with all its content?" : ""), MessageWindow.Type.CONFIRMATION);
        }
        boolean toRemove = messageWindow.getResult();
        if (!toRemove) return;
        new Thread(() -> {
            requestRemove(nodeToRemove.getFile().getAbsolutePath());
            String resp;
            try {
                resp = statusExchanger.exchange("ok");
                if (resp.startsWith(OK)) {
                    removeItem(nodeToRemove);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                hideProgressBar();
            }
        }).start();

    }

    @FXML
    public void onRefreshButton() {
        requestFilesTreeRefresh();
    }

    @FXML
    public void onAddFolderButton() {
        TreeItem<FilesTree> item = treeView.getSelectionModel().getSelectedItem();
        if (item == null) return;

        InputWindow inputWindow = new InputWindow();
        inputWindow.show("New Folder", "Please enter folder name", "New Folder");
        String name = inputWindow.getResult();
        System.out.println("New folder name: " + name);
        if (name == null) return;

        FilesTree parent = item.getValue();

        new Thread(()->{
            showProgressBar();
            sendMessage(socketChannel, NEWFOLDER , parent.getFile().getAbsolutePath(), name);

            try {
                String newPath = statusExchanger.exchange("ok");
                if (newPath.startsWith(NOK)) return;
                File newFile = new File(newPath);
                FilesTree newNode = new FilesTree(newFile, true, iconFolder);
                parent.addChild(newNode);
                TreeItem<FilesTree> newTreeItem = new TreeItem<>(newNode, new ImageView(iconFolder));
                treeView.getSelectionModel().getSelectedItem().getChildren().add(newTreeItem);
                treeView.refresh();
                setSelectedTreeItem(newTreeItem.getParent());

            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                hideProgressBar();
            }
        }).start();

    }

    @FXML
    public void onLogoutButton() {
        messageWindow.show("Please confirm", "Are you sure to log out?", MessageWindow.Type.CONFIRMATION);
        isLoggedIn.set(!messageWindow.getResult());
        if (isLoggedIn.get()) return;
        //close this window
        Stage thisStage = (Stage) logoutButton.getScene().getWindow();
        thisStage.close();
        sendMessage(socketChannel, LOGOUT);
        loginStage.show();
    }

    @FXML
    public void keyboardHandler(KeyEvent ke) {
        if (ke.getCode().equals(KeyCode.DELETE))
            onRemoveButton();
    }

    public void setLoginStage(Stage stage) {
        loginStage = stage;
    }

    public void onExit() {
        try {
            isLoggedIn.set(false);
            sendMessage(socketChannel, END);
            socketChannel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Main Window is closing");
    }

    private void setIcons(FilesTree f) {
        if (!f.isDirectory()) {
            f.setIcon(new ImageView(iconFile));
        } else {
            f.setIcon(new ImageView(iconFolder));
            for (FilesTree ft : f.getChildren()) {
                setIcons(ft);
            }
        }
    }

    public void setupVisualElements() {
        progressBox.setVisible(false);
        messageWindow = new MessageWindow();
        iconFolder = new Image(getClass().getResourceAsStream("folder_icon.png"), 18, 18, true, false);
        iconFile = new Image(getClass().getResourceAsStream("file_icon.png"), 18, 18, true, false);
        Image iconDownload = new Image(getClass().getResourceAsStream("download_icon.png"), 48, 48, true, false);
        downloadButton.setGraphic(new ImageView(iconDownload));
        Image iconUpload = new Image(getClass().getResourceAsStream("upload_icon.png"), 48, 48, true, false);
        uploadButton.setGraphic(new ImageView(iconUpload));
        Image iconAddFolder = new Image(getClass().getResourceAsStream("addFolder_icon.png"), 48, 48, true, false);
        addFolderButton.setGraphic(new ImageView(iconAddFolder));
        Image iconLogout = new Image(getClass().getResourceAsStream("logout_icon.png"), 48, 48, true, false);
        logoutButton.setGraphic(new ImageView(iconLogout));
        Image iconRefresh = new Image(getClass().getResourceAsStream("refresh_icon.png"), 48, 48, true, false);
        refreshButton.setGraphic(new ImageView(iconRefresh));
        Image iconRemove = new Image(getClass().getResourceAsStream("remove_icon.png"), 48, 48, true, false);
        removeButton.setGraphic(new ImageView(iconRemove));
        Image iconRename = new Image(getClass().getResourceAsStream("rename_icon.png"), 48, 48, true, false);
        renameButton.setGraphic(new ImageView(iconRename));

        Image progressGif = new Image(getClass().getResourceAsStream("spinner.gif"), 60, 60, true, false);
        progressImageView.setImage(progressGif);

        subColumnIcon.setCellValueFactory(new PropertyValueFactory("icon"));
        subColumnIcon.setStyle("-fx-pref-height: 0;");

        subColumnName.setCellValueFactory(new PropertyValueFactory<>("displayName"));
        subColumnName.setStyle("-fx-pref-height: 0;");
        columnType.setCellValueFactory(new PropertyValueFactory<>("type"));
        columnSize.setCellValueFactory(new PropertyValueFactory<>("size"));
        columnTime.setCellValueFactory(new PropertyValueFactory<>("timestamp"));

        tableView.getSortOrder().add(columnType);
        subColumnName.setCellFactory(TextFieldTableCell.<FilesTree>forTableColumn());
        tableView.setStyle("-fx-control-inner-background-alt: -fx-control-inner-background; -fx-table-cell-border-color: transparent;");

        subColumnName.setOnEditCommit(
                (EventHandler<TableColumn.CellEditEvent<FilesTree, String>>) t -> {
                    System.out.println("On edit commit called");
                    FilesTree parentNode = treeView.getSelectionModel().getSelectedItem().getValue();
                    FilesTree changedNode = t.getTableView().getItems().get(t.getTablePosition().getRow());
                    String s = t.getNewValue();

                    if (changedNode.equals(filesTree)) {
                        messageWindow.show("Rename failed", "Cannot rename root folder", MessageWindow.Type.INFORMATION);
                        return;
                    }
                    if (s.equals(changedNode.getName())) {
                        return;
                    }

                    new Thread(()->{
                        showProgressBar();
                        requestRename(changedNode.getFile().getAbsolutePath(), s);
                        String resp;
                        try {
                            resp = statusExchanger.exchange("ok");
                            System.out.println("thread Main got msg: " + resp);
                            if (resp.startsWith(OK)) {
                                TreeItem<FilesTree> item = getTreeItemByValue(treeView.getRoot(), changedNode);
                                TreeItem<FilesTree> parentItem = treeView.getSelectionModel().getSelectedItem();
                                parentNode.getChildren().remove(changedNode);
                                FilesTree newNode = tempNode;
                                parentNode.getChildren().add(newNode);

                                if (newNode.isDirectory()) {
                                    parentItem.getChildren().remove(item);
                                    parentItem.getChildren().add(buildTreeView(newNode));
                                }
                                treeView.refresh();
                                setSelectedTreeItem(parentItem);
                            } else {
                                tableView.requestFocus();
                                Robot robot = new Robot();
                                robot.keyPress(KeyCode.ENTER);
                            }
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        } finally {
                            hideProgressBar();
                        }
                    }).start();
                }
        );

//        тут мы делаем так, чтобы при двойном клике на папке в табличной части автоматически выделялся
//         соответствующий узел дерева каталогов и мы проваливались в эту папку
        tableView.setRowFactory(tv -> {
            TableRow<FilesTree> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() >= 2) {
                    FilesTree selectedFilesTreeNode = row.getItem();
                    TreeItem<FilesTree> selectedTreeItem = getTreeItemByValue(treeView.getRoot(), selectedFilesTreeNode);
                    if (selectedFilesTreeNode.isDirectory())
                        setSelectedTreeItem(selectedTreeItem);
                }
            });
            return row;
        });
    }



    private void showProgressBar(){
        Platform.runLater(()->{
            if (!progressBox.isVisible()) {
                progressBox.setVisible(true);
                mainPane.setDisable(true);
            }
        });
    }

    private void hideProgressBar(){
        Platform.runLater(()->{
            if (progressBox.isVisible()) {
                progressBox.setVisible(false);
                mainPane.setDisable(false);
            }
        });
    }

}
