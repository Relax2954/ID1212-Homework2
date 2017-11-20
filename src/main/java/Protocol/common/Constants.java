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
public class Constants {
    
     /**
     * Separates a message type specifier from the message body.
     */
    public static final String MSG_TYPE_DELIMETER = "##";
    /**
     * Separates a message length header.
     */
    public static final String MSG_LEN_DELIMETER = "###";
    /**
     * The message type specifier is the first token in a message.
     */
    public static final int MSG_TYPE_INDEX = 0;
    /**
     * The message body is the second token in a message.
     */
    public static final int MSG_BODY_INDEX = 1;

    /**
     * The max length of a an entry.
     */
    public static final int MAX_MSG_LENGTH = 70; //more than enough
}
    

