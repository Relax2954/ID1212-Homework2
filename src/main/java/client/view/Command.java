package client.view;
/**
 *
 * @author Relax2954
 */
/**
 * Defines all commands that can be performed by a user of the application.
 */
public enum Command {
    //start the game
    START,
    //guess
    GUESS,
    /**
     * Establish a connection to the server. The first parameter is IP address
     * (or host name), the second is port number.
     */
    CONNECT,
    /**
     * Leave the application.
     */
    QUIT,
    /**
     * No command was specified. This means the entire command line is
     * interpreted as an entry in the game. This case is also handled in TheServer.java
     */
    NO_COMMAND;

}
