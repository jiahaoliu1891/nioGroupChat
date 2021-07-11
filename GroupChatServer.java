import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;
import java.io.*;

public class GroupChatServer {
    private Selector selector;
    private ServerSocketChannel listenChannel;
    private static final int PORT = 6666;


    public GroupChatServer() {
        try {
            selector = Selector.open();
            listenChannel = ServerSocketChannel.open();
            listenChannel.socket().bind(new InetSocketAddress("127.0.0.1", PORT));
            listenChannel.configureBlocking(false);
            listenChannel.register(selector, SelectionKey.OP_ACCEPT);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void readData(SelectionKey key) {
        // Each selection key is bind to a socket channel
        SocketChannel client = null;
        try {
            client = (SocketChannel) key.channel();
            // read from client channel to buffer
            ByteBuffer buffer = ByteBuffer.allocate(1024);
            int count = client.read(buffer);
            if (count > 0) {
                String msg = "Client [" + client.getRemoteAddress() + "] : "+ new String(buffer.array());
                // output the msg at server
                System.out.println(msg);
                // send msg to other clients
                sendInfoToOtherClients(msg, client);
            }
        } catch (IOException e) {
            try {
                System.out.println(client.getRemoteAddress() + " is Offline");
                key.cancel();
                client.close();
            } catch (Exception ee) {
                ee.printStackTrace();
            }
            e.printStackTrace();
        }
    }

    private void sendInfoToOtherClients(String msg, SocketChannel self) throws IOException{
        // iterate through all the SocketChannel, except self
        ByteBuffer buffer = ByteBuffer.wrap(msg.getBytes());
        for (SelectionKey key : selector.keys()) {
            // Get Channels from key
            Channel targetClient = key.channel();
            // WARN: there is a SocketServerChannel
            if (targetClient instanceof SocketChannel && targetClient != self) {
                ((SocketChannel)targetClient).write(buffer);
            }
        }
    }

    public void listen() {
        try {
            while (true) {
                int count = selector.select(2000);
                if (count > 0) {
                    Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
                    while (iterator.hasNext()) {
                        SelectionKey key = iterator.next();
                        if (key.isAcceptable()) {
                            System.out.println("### is Acceptable!");
                            // client socket
                            SocketChannel sc = listenChannel.accept();
                            sc.configureBlocking(false);
                            sc.register(selector, SelectionKey.OP_READ);
                            System.out.println(sc.getRemoteAddress() + " is online");
                        }
                        if (key.isReadable()) {
                            System.out.println("### is Readable!");
                            readData(key);
                        }
                        iterator.remove();
                    }
                } else {
                    // System.out.println("Server Waiting for Connection ...");
                    continue;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } 
    }

    public static void main(String[] args) {
        System.out.println("### Hello World, I am Group Chat Server ...");
        GroupChatServer server = new GroupChatServer();
        System.out.println("### Start to listen...");
        server.listen();
    }
}