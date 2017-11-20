/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Protocol.common;

/**
 *
 * @author Relax2954
 */
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.StringJoiner;

/**
 * Handles receiving partial or multiple messages. Received data is sent to this
 * class, and individual messages are extracted. All methods are thread safe.
 */
public class MessageSplitter {

    private StringBuilder recvdChars = new StringBuilder();
    private final Queue<String> messages = new ArrayDeque<>();

    /**
     * Appends a newly received string to previously received strings.
     *
     * @param recvdString The received string.
     */
    public synchronized void appendRecvdString(String recvdString) {
        recvdChars.append(recvdString);
        while (extractMsg());
    }

    /**
     * @return The first received message that has not previously been returned,
     * or <code>null</code> if there is no complete message available.
     */
    public synchronized String nextMsg() {
        return messages.poll();
    }

    /**
     * @return <code>true</code> if there is at least one unread complete
     * message, <code>false</code> if there is not.
     */
    public synchronized boolean hasNext() {
        return !messages.isEmpty();
    }

    /**
     * Prepends a length header to the specified message. This method should be
     * used by senders. The returned message can be handled by instances of this
     * class when the message is received.
     *
     * @param msgWithoutHeader A message with no length header
     * @return The specified message, with the appropriate length header
     * prepended.
     */
    public static String prependLengthHeader(String msgWithoutHeader) {
        StringJoiner joiner = new StringJoiner(Constants.MSG_LEN_DELIMETER);
        joiner.add(Integer.toString(msgWithoutHeader.length()));
        joiner.add(msgWithoutHeader);
        return joiner.toString();
    }

    /**
     * Returns the type of the specified message.
     */
    public static MsgType typeOf(String msg) {
        String[] msgParts = msg.split(Constants.MSG_TYPE_DELIMETER);
        return MsgType.valueOf(msgParts[Constants.MSG_TYPE_INDEX].toUpperCase());
    }

    /**
     * Returns the body of the specified message.
     */
    public static String bodyOf(String msg) {
        String[] msgParts = msg.split(Constants.MSG_TYPE_DELIMETER);
        return msgParts[Constants.MSG_BODY_INDEX];
    }

    private boolean extractMsg() {
        String allRecvdChars = recvdChars.toString();
        String[] splitAtHeader = allRecvdChars.split(Constants.MSG_LEN_DELIMETER);
        if (splitAtHeader.length < 2) {
            return false;
        }
        String lengthHeader = splitAtHeader[0];
        int lengthOfFirstMsg = Integer.parseInt(lengthHeader);
        if (hasCompleteMsg(lengthOfFirstMsg, splitAtHeader[1])) {
            String completeMsg = splitAtHeader[1].substring(0, lengthOfFirstMsg);
            messages.add(completeMsg);
            recvdChars.delete(0, lengthHeader.length()
                    + Constants.MSG_LEN_DELIMETER.length() + lengthOfFirstMsg);
            return true;
        }
        return false;
    }

    private boolean hasCompleteMsg(int msgLen, String recvd) {
        return recvd.length() >= msgLen;
    }

}
