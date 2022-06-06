package common;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.io.File;
import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.TimeZone;

public class FilesTree implements Serializable {
    private ArrayList<FilesTree> children;
    private File file;

    private ImageView icon;
    private boolean isRoot = false;
    private String name;
    private String displayName;
    private String type;
    private Long size;
    private String timestamp;

    static DateFormat formatter = new SimpleDateFormat("dd.MM.yy HH:mm");
    static {
        formatter.setTimeZone(TimeZone.getTimeZone("GMT+3"));
    }

    public FilesTree(File f) {
        this.file = f;
        this.children = new ArrayList<>();
        this.name = f.getName();
        this.icon = null;
        this.type = (isDirectory()? "Folder" : "File");
        this.size = f.length();

        Date date = new Date(f.lastModified());
        this.timestamp = formatter.format(date);

        if (this.isDirectory()) {
            File[] files = f.listFiles();
            for (File fl : files) {
                this.addChild(new FilesTree(fl));
            }
        }
    }

    public FilesTree(File f, String rootDisplayName) {
        this(f);
        this.isRoot = true;
        this.displayName = rootDisplayName;
    }

    public FilesTree(File f, boolean isDir, Image icon) {
        this(f);
        setDirectory(isDir);
        setIcon(new ImageView(icon));
    }

    public boolean isDirectory(){
        return file.isDirectory();
    }
    public void setDirectory(boolean isDir){
        this.type = (isDir? "Folder" : "File");
    }

    public void addChild(FilesTree child){
        children.add(child);
    }
    public boolean removeChild(FilesTree child) {
        if (children.remove(child)) return true;
        for (FilesTree f: this.getChildren()) {
            if (f.removeChild(child)) return true;
        }
        return false;
    }
    public File getFile(){
        return file;
    }
    public String getName(){
        return file.getName();
    }

    public FilesTree validateFile(String path){
        FilesTree result = null;
        File file = new File(path);
        if (this.getFile().getAbsolutePath().equals(file.getAbsolutePath()))
            return this;
        for (FilesTree f: this.getChildren()) {
            result = (f.validateFile(path));
            if (result != null) return result;
        }
        return null;
    }

    public void setIcon(ImageView icon){
        this.icon = icon;
    }

    @Override
    public String toString() {
        return this.getDisplayName();
    }

//    @Override
//    public boolean equals(Object o) {
//        if (this == o) return true;
//        if (!(o instanceof FilesTree)) return false;
//        FilesTree filesTree = (FilesTree) o;
//        return file != null ? file.getAbsolutePath().equals(filesTree.file.getAbsolutePath()) : filesTree.file == null;
//    }
//
//    @Override
//    public int hashCode() {
//        return file != null ? file.hashCode() : 0;
//    }

    public ArrayList<FilesTree> getChildren() {
        return children;
    }

    public String getType() {
        return type;
    }

    public Long getSize() {
        return size;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public ImageView getIcon(){ return icon;}

    public String getDisplayName(){
        if (isRoot) return displayName;
        return getName();
    }

    public void printNode(){
        System.out.println(this);
        for (FilesTree f:this.getChildren()) {
            f.printNode();
        }
    }
}
