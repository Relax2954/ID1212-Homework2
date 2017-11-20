package client.net;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.StringJoiner;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import Protocol.common.MessageException;
import Protocol.common.Constants;
import Protocol.common.MessageSplitter;
import Protocol.common.MsgType;

/**
 *
 * @author Relax2954
 */
/**
 * Manages all communication with the server, all operations are non-blocking.
 */
public class ServerConnection implements Runnable {

    private static final String FATAL_COMMUNICATION_MSG = "Lost connection.";
    private static final String FATAL_DISCONNECT_MSG = "Could not disconnect, will leave ungracefully.";

    private final ByteBuffer msgFromServer = ByteBuffer.allocateDirect(Constants.MAX_MSG_LENGTH);
    private final Queue<ByteBuffer> messagesToSend = new ArrayDeque<>();
    private final MessageSplitter msgSplitter = new MessageSplitter();
    private final List<CommunicationListener> listeners = new ArrayList<>();
    private InetSocketAddress serverAddress;
    private SocketChannel socketChannel;
    private Selector selector;
    private boolean connected;
    private volatile boolean timeToSend = false;

    /**
     * The communicating thread, all communication is non-blocking. First,
     * server connection is established. Then the thread sends messages
     * submitted via one of the <code>send</code> methods in this class and
     * receives messages from the server.
     */
    
    @Override
    public void run() {
        try {
            initConnection();
            initSelector();

            while (connected || !messagesToSend.isEmpty()) {
                if (timeToSend) {
                    socketChannel.keyFor(selector).interestOps(SelectionKey.OP_WRITE);
                    timeToSend = false;
                }

                selector.select();
                for (SelectionKey key : selector.selectedKeys()) {
                    selector.selectedKeys().remove(key);
                    if (!key.isValid()) {
                        continue;
                    }
                    if (key.isConnectable()) {
                        completeConnection(key);
                    } else if (key.isReadable()) {
                        recvFromServer(key);
                    } else if (key.isWritable()) {
                        sendToServer(key);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println(FATAL_COMMUNICATION_MSG);
        }
        try {
            doDisconnect();
        } catch (IOException ex) {
            System.err.println(FATAL_DISCONNECT_MSG);
        }
    }

    /**
     * The specified listener will be notified when connecting, disconnecting
     * and receiving a message.
     *
     * @param listener The listener to notify.
     */
    public void addCommunicationListener(CommunicationListener listener) {
        listeners.add(listener);
    }

    /**
     * Starts the communicating thread and connects to the server.
     *
     * @param host Host name or IP address of server.
     * @param port Server's port number.
     * @throws IOException If failed to connect.
     */
    public void connect(String host, int port) {
        serverAddress = new InetSocketAddress(host, port);
        new Thread(this).start();
    }

    private void initSelector() throws IOException {
        selector = Selector.open();
        socketChannel.register(selector, SelectionKey.OP_CONNECT);
    }

    private void initConnection() throws IOException {
        socketChannel = SocketChannel.open();
        socketChannel.configureBlocking(false);
        socketChannel.connect(serverAddress);
        connected = true;
    }

    private void completeConnection(SelectionKey key) throws IOException {
        socketChannel.finishConnect();
        key.interestOps(SelectionKey.OP_READ);
        try {
            InetSocketAddress remoteAddress = (InetSocketAddress) socketChannel.getRemoteAddress();
            notifyConnectionDone(remoteAddress);
        } catch (IOException couldNotGetRemAddrUsingDefaultInstead) {
            notifyConnectionDone(serverAddress);
        }
    }

    /**
     * Closes the connection with the server.
     *
     * @throws IOException If failed to close socket.
     */
    public void disconnect() throws IOException {
        connected = false;
        sendMsg(MsgType.DISCONNECT.toString(), null);
    }

    private void doDisconnect() throws IOException {
        socketChannel.close();
        socketChannel.keyFor(selector).cancel();
        notifyDisconnectionDone();
    }

    public void sendGuess(String guess) throws IOException {
        sendMsg(MsgType.GUESS.toString(), guess);
    }

    public void sendStartgame(String start) throws IOException {
        sendMsg(MsgType.START.toString(), start);
    }

    public void sendWrongInput() throws IOException {
        sendMsg(MsgType.WRONGINPUT.toString(), "");
    }

    private void sendMsg(String... parts) {
        StringJoiner joiner = new StringJoiner(Constants.MSG_TYPE_DELIMETER);
        for (String part : parts) {
            joiner.add(part);
        }
        String messageWithLengthHeader = MessageSplitter.prependLengthHeader(joiner.toString());
        synchronized (messagesToSend) {
            messagesToSend.add(ByteBuffer.wrap(messageWithLengthHeader.getBytes()));
        }
        timeToSend = true;
        selector.wakeup();
    }

    private void sendToServer(SelectionKey key) throws IOException {
        ByteBuffer msg;
        synchronized (messagesToSend) {
            while ((msg = messagesToSend.peek()) != null) {
                socketChannel.write(msg);
                if (msg.hasRemaining()) {
                    return;
                }
                messagesToSend.remove();
            }
            key.interestOps(SelectionKey.OP_READ);
        }
    }

    private void recvFromServer(SelectionKey key) throws IOException {
        msgFromServer.clear();
        int numOfReadBytes = socketChannel.read(msgFromServer);
        if (numOfReadBytes == -1) {
            throw new IOException(FATAL_COMMUNICATION_MSG);
        }
        String recvdString = extractMessageFromBuffer();
        msgSplitter.appendRecvdString(recvdString);
        while (msgSplitter.hasNext()) {
            String msg = msgSplitter.nextMsg();
            if (MessageSplitter.typeOf(msg) != MsgType.NETWORKING) {
                throw new MessageException("Received corrupt message: " + msg);
            }
            notifyMsgReceived(MessageSplitter.bodyOf(msg));
        }
    }

    private String extractMessageFromBuffer() {
        msgFromServer.flip();
        byte[] bytes = new byte[msgFromServer.remaining()];
        msgFromServer.get(bytes);
        return new String(bytes);
    }

    private void notifyConnectionDone(InetSocketAddress connectedAddress) {
        Executor pool = ForkJoinPool.commonPool();
        for (CommunicationListener listener : listeners) {
            pool.execute(new Runnable() {
                @Override
                public void run() {
                    listener.connected(connectedAddress);
                }
            });
        }
    }

    private void notifyDisconnectionDone() {
        Executor pool = ForkJoinPool.commonPool();
        for (CommunicationListener listener : listeners) {
            pool.execute(new Runnable() {
                @Override
                public void run() {
                    listener.disconnected();
                }
            });
        }
    }

    private void notifyMsgReceived(String msg) {
        Executor pool = ForkJoinPool.commonPool();
        for (CommunicationListener listener : listeners) {
            pool.execute(new Runnable() {
                @Override
                public void run() {
                    listener.recvdMsg(msg);
                }
            });
        }
    }

}
