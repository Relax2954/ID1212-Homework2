package client.startup;

import client.view.NonBlockingInterpreter;

/**
 *
 * @author Relax2954
 */

/**
 * Starts the client.
 */
public class Main {

    /**
     * @param args There are no command line arguments.
     */
    public static void main(String[] args) {
        new NonBlockingInterpreter().start();
    }
}
