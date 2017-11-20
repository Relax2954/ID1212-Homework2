/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package client.net;
import java.net.InetSocketAddress;


/**
 *
 * @author Relax2954
 */
public interface CommunicationListener {
    /**
     * Called when a message from the server has been received. That message originates
     * from a client.
     *
     * @param msg The message from the server.
     */
    public void recvdMsg(String msg);

    /**
     * Called when the local client is successfully connected to the server.
     *
     * @param serverAddress The address of the server to which connection is established.
     */
    public void connected(InetSocketAddress serverAddress);

    /**
     * Called when the local client is successfully disconnected from the server.
     */
    public void disconnected();
}

