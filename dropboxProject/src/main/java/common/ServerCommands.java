package common;

public interface ServerCommands {
    String AUTH = "/reqauthor";
    String GETFILELIST = "/reqfileli";
    String RENAME = "/reqrename";
    String REMOVE = "/reqremove";
    String NEWFOLDER = "/reqnewdir";
    String DOWNLOAD = "/reqdownlo";
    String UPLOAD = "/requpload";
    String END = "/end";
    String LOGOUT = "/reqlogout";

    String AUTHOK ="/authoriok";
    String FILES_TREE = "/filestree";
    String RENAMSTATUS = "/renamstat";
    String REMSTATUS = "/removalst";
    String NEWFOLDSTATUS = "/newdirsta";
    String DOWNLCOUNT = "/downloqty";
    String DOWNLSTATUS = "/downs";
    String UPLOADSTAT = "/uploadsta";
    String OK = "OK";
    String NOK = "NO";
    String INFO = "/infomessg";
    String LOGOUTOK = "/logoutoke";

    String SEPARATOR = "//";
    int COMMAND_LENGTH = 10;
    int DEFAULT_BUFFER = 2048;
}
