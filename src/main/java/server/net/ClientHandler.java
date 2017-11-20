/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package server.net;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.StringJoiner;
import java.util.concurrent.ForkJoinPool;
import Protocol.common.Constants;
import Protocol.common.MessageException;
import Protocol.common.MsgType;
import Protocol.common.MessageSplitter;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Relax2954
 */
/**
 * Handles all communication with one particular client.
 */
class ClientHandler implements Runnable {

    private static final String GUESS_DELIMETER = " ";

    private final TheServer server;
    private final SocketChannel clientChannel;
    private final ByteBuffer msgFromClient = ByteBuffer.allocateDirect(Constants.MAX_MSG_LENGTH);
    private final MessageSplitter msgSplitter = new MessageSplitter();
    private String guess = null;
    private boolean connected;
    private Allwords myallwords;
    private int score = 0; //the total current score;
    private String chosenword;
    private volatile String checkerString;   //SHOULD IT BE VOLATILE, MAYBE ATOMIC?  check private?
    private volatile int remaining = 0;  //remaining shots to take
    private char[] checker; //ovdje stavlja  capword cifru lokacije pogodjenog slova
    private String tempor; //this is for just printiing out the current ___f__c__

    /**
     * Creates a new instance, which will handle communication with one specific
     * client connected to the specified channel.
     *
     * @param clientChannel The socket to which this handler's client is
     * connected.
     */
    ClientHandler(TheServer server, SocketChannel clientChannel) {
        this.server = server;
        this.clientChannel = clientChannel;
        connected = true;

    }

    public String chooseWord() throws FileNotFoundException { //this is choosing a word from words.txt

        myallwords = new Allwords();
        Random rand = new Random();
        ArrayList<String> names = myallwords.getList();
        String randomword = names.get(rand.nextInt(names.size()));
        return randomword;
    }

    public String gamelogic(String theword, String myguess) { //this is where the game logic magic happens
        if (theword == null) {
            return "Please start the game before guessing";
        }
        String capguess = myguess.toLowerCase();
        String capword = theword.toLowerCase();
        char[] capwordarray = capword.toLowerCase().toCharArray();

        if (remaining == 0) {
            return "Please start a new game.";
        } else if (capguess.length() != capword.length() && capguess.length() != 1) {
            remaining--;
            checkerString = String.valueOf(checker);
            if (remaining == 0) {
                score--;
                return checkerString + "\nRemaining attempts: " + remaining + "\nYour total score is " + score;
            }
            return checkerString + "\nRemaining attempts: " + remaining;
        } else if (capguess.equals(capword)) {
            score++;
            remaining = 0;
            return capword + "\nYour total score is " + score;
        } else if (!capword.contains(capguess)) {
            remaining--;
            checkerString = String.valueOf(checker);
            if (remaining == 0) {
                score--;
                return checkerString + "\nRemaining attempts: " + remaining + "\nYour total score is " + score;
            }
            return checkerString + "\nRemaining attempts: " + remaining;
        } else {
            for (int i = 0; i < capword.length(); i++) {
                if (capguess.charAt(0) == capwordarray[i]) {
                    checker[i] = capguess.charAt(0);  //u ovom praznom array stavlja guess slova
                }
            }
            checkerString = String.valueOf(checker);
            if (!checkerString.contains("_")) {
                score++;
                remaining = 0;
                return capword + "\nYour total score is " + score;
            }
            return checkerString + "\nRemaining attempts: " + remaining;
        }
    }

    public ByteBuffer createBufferedMessage(String msg) {
        StringJoiner joiner = new StringJoiner(Constants.MSG_TYPE_DELIMETER);
        joiner.add(MsgType.NETWORKING.toString());
        joiner.add(msg);
        String messageWithLengthHeader = MessageSplitter.prependLengthHeader(joiner.toString());
        return ByteBuffer.wrap(messageWithLengthHeader.getBytes());
    }

    /**
     * The run loop handling all communication with the connected client.
     */
    @Override
    public void run() {
        while (msgSplitter.hasNext()) {
            try {
                Message msg = new Message(msgSplitter.nextMsg());
                switch (msg.msgType) {
                    case START:
                        String gameentry = msg.msgBody;
                        ;
                        if (gameentry.toLowerCase().contains("game".toLowerCase())) {
                            chosenword = chooseWord();
                            remaining = chosenword.length();
                            checker = new char[chosenword.length()];
                            Arrays.fill(checker, '_');
                            checkerString = String.valueOf(checker);
                            ByteBuffer BuffCheckerString = createBufferedMessage(checkerString+" "+ chosenword);
                            sendMsg(BuffCheckerString);
                        } else {
                            ByteBuffer BuffNotification = createBufferedMessage("Please start game or guess the word.");
                            sendMsg(BuffNotification);
                        }
                        break;
                    case GUESS:
                        guess = msg.msgBody;
                        tempor = gamelogic(chosenword, guess);
                        ByteBuffer BufferedTempor = createBufferedMessage(tempor);
                        sendMsg(BufferedTempor);
                        break;
                    case WRONGINPUT:
                         ByteBuffer BufferedWronginput = createBufferedMessage("Please start game or guess the word.");
                        sendMsg(BufferedWronginput);
                        break;
                    case DISCONNECT:
                        ByteBuffer BufferedDisconnect = createBufferedMessage("You are now disconnected.");
                        sendMsg(BufferedDisconnect);
                        disconnectClient();
                        break;
                    default:
                        throw new MessageException("Received corrupt message: " + msg);
                }
            } catch (IOException ex) {
                Logger.getLogger(ClientHandler.class.getName()).log(Level.SEVERE, null, ex);
            }

        }
    }

    /**
     * Sends the specified message to the connected client.
     *
     * @param msg The message to send.
     * @throws IOException If failed to send message.
     */
    void sendMsg(ByteBuffer msg) throws IOException {
        clientChannel.write(msg);
        if (msg.hasRemaining()) {
            throw new MessageException("Could not send message");
        }
    }

    /**
     * Reads a message from the connected client, then submits a task to the
     * default <code>ForkJoinPool</code>. That task which will handle the
     * received message.
     *
     * @throws IOException If failed to read message
     */
    void recvMsg() throws IOException {
        msgFromClient.clear();
        int numOfReadBytes;
        numOfReadBytes = clientChannel.read(msgFromClient);
        if (numOfReadBytes == -1) {
            throw new IOException("Client has closed connection.");
        }
        String recvdString = extractMessageFromBuffer();
        msgSplitter.appendRecvdString(recvdString);
        ForkJoinPool.commonPool().execute(this);
    }

    private String extractMessageFromBuffer() {
        msgFromClient.flip();
        byte[] bytes = new byte[msgFromClient.remaining()];
        msgFromClient.get(bytes);
        return new String(bytes);
    }

    /**
     * Closes this instance's client connection.
     *
     * @throws IOException If failed to close connection.
     */
    void disconnectClient() throws IOException {
        clientChannel.close();
    }

    private static class Message {

        private MsgType msgType;
        private String msgBody;
        private String receivedString;

        private Message(String receivedString) {
            parse(receivedString);
            this.receivedString = receivedString;
        }

        private void parse(String strToParse) {
            try {
                String[] msgTokens = strToParse.split(Constants.MSG_TYPE_DELIMETER);
                msgType = MsgType.valueOf(msgTokens[Constants.MSG_TYPE_INDEX].toUpperCase());
                if (hasBody(msgTokens)) {
                    msgBody = msgTokens[Constants.MSG_BODY_INDEX].trim();
                }
            } catch (Throwable throwable) {
                throw new MessageException(throwable);
            }
        }

        private boolean hasBody(String[] msgTokens) {
            return msgTokens.length > 1;
        }
    }
}
