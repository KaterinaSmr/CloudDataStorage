package server;

public interface ServerCommands {
    String AUTH = "/reqauthoriz";
    String AUTHOK ="/authorizaok";
    String GETFILELIST = "/reqfilelist";
    String SEP = " ";
    String END = "/end";
    String FILES_TREE = "/ftree";
    int COMMAND_LENGTH = 12;
}
