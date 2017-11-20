package client.view;

import java.util.Scanner;
import java.net.InetSocketAddress;
import client.net.CommunicationListener;
import client.net.ServerConnection;

/**
 *
 * @author Relax2954
 */
/**
 * Reads and interprets user commands. The command interpreter will run in a
 * separate thread, which is started by calling the <code>start</code> method.
 * Commands are executed in a thread pool, a new prompt will be displayed as
 * soon as a command is submitted to the pool, without waiting for command
 * execution to complete.
 */
public class NonBlockingInterpreter implements Runnable {

    private static final String PROMPT = "> ";
    private final Scanner console = new Scanner(System.in);
    private final ThreadSafeStdOut outMgr = new ThreadSafeStdOut();
    private boolean receivingCmds = false;
    private ServerConnection server;

    /**
     * Starts the interpreter. The interpreter will be waiting for user input
     * when this method returns. Calling <code>start</code> on an interpreter
     * that is already started has no effect.
     */
    public void start() {
        if (receivingCmds) {
            return;
        }
        receivingCmds = true;
        server = new ServerConnection();
        new Thread(this).start();
    }

    /**
     * Interprets and performs user commands.
     */
    @Override
    public void run() {
        while (receivingCmds) {
            try {
                CmdLine cmdLine = new CmdLine(readNextLine());
                switch (cmdLine.getCmd()) {
                    case QUIT:
                        receivingCmds = false;
                        server.disconnect();
                        break;
                    case CONNECT:
                        server.addCommunicationListener(new ConsoleOutput());
                        server.connect(cmdLine.getParameter(0),
                                Integer.parseInt(cmdLine.getParameter(1)));
                        break;
                    case START:
                        server.sendStartgame(cmdLine.getParameter(0));
                        break;
                    case GUESS:
                        server.sendGuess(cmdLine.getParameter(0));
                        break;
                    default:
                        server.sendWrongInput();
                }
            } catch (Exception e) {
                outMgr.println("Operation failed");
            }
        }
    }

    private String readNextLine() {
        outMgr.print(PROMPT);
        return console.nextLine();
    }

     private class ConsoleOutput implements CommunicationListener {
        @Override
        public void recvdMsg(String msg) {
            printToConsole(msg);
        }
        @Override
        public void connected(InetSocketAddress serverAddress) {
            printToConsole("Connected to " + serverAddress.getHostName() + ":"
                           + serverAddress.getPort());
        }

        @Override
        public void disconnected() {
            printToConsole("Disconnected from server.");
        }

        private void printToConsole(String output) {
            outMgr.println(output);
            outMgr.print(PROMPT);
        }
    }
}

