package common;

public interface ServerCommands {
    String AUTH = "/reqauthoriz";
    String AUTHOK ="/authorizaok";
    String GETFILELIST = "/reqfilelist";
    String SEPARATOR = " ";
    String END = "/end";
    String FILES_TREE = "/ftree";
    int COMMAND_LENGTH = 12;
}
