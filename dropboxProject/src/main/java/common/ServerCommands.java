package common;

public interface ServerCommands {
    String AUTH = "/reqauthor";
    String GETFILELIST = "/reqfileli";
    String RENAME = "/reqrename";
    String REMOVE = "/reqremove";
    String NEWFOLDER = "/reqnewdir";
    String DOWNLOAD = "/reqdownlo";
    String UPLOAD = "/requpload";
    String END = "/requirend";
    String LOGOUT = "/reqlogout";
    String SIGNUP = "/reqsignup";

    String AUTHSTATUS ="/authstatu";
    String FILES_TREE = "/filestree";
    String RENAMSTATUS = "/renamstat";
    String REMSTATUS = "/removalst";
    String NEWFOLDSTATUS = "/newdirsta";
    String DOWNLCOUNT = "/downloqty";
    String DOWNLSTATUS = "/downlstat";
    String UPLOADCHECK = "/uploadche";
    String UPLOADSTAT = "/uploadsta";
    String OK = "OK";
    String NOK = "NO";
    String INFO = "/infomessg";
    String LOGOUTOK = "/logoutoke";
    String SIGNUPSTA = "/signupsta";

    String SEPARATOR = "//";
    String UNKNOWN = "Unknown error. Please try again later.";
    int TIMEOUT_MINS = 5;
    int COMMAND_LENGTH = 10 + SEPARATOR.length();
    int DEFAULT_BUFFER = 2048;
}
