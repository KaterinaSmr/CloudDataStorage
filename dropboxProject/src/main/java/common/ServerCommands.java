package common;

public interface ServerCommands {
    String AUTH = "/reqauthor";
    String GETFILELIST = "/reqfileli";
    String RENAME = "/reqrename";
    String RENAMSTATUS = "/renamstat";
    String REMOVE = "/reqremove";
    String REMSTATUS = "/removalst";
    String NEWFOLDER = "/reqnewdir";
    String NEWFOLDSTATUS = "/newdirsta";
    String DOWNLOAD = "/reqdownlo";
    String DOWNLCOUNT = "/downloqty";
    String DOWNLSTATUS = "/downs";
    String OK = "OK";
    String NOK = "NO";
    String AUTHOK ="/authoriok";
    String INFO = "/infomessa";
    String SEPARATOR = "//";
    String END = "/end";
    String FILES_TREE = "/filestree";
    int COMMAND_LENGTH = 10;
}
