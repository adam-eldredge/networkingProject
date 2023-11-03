import java.net.*;
import java.util.logging.Handler;
import java.io.*;

public class ConnectionHandler {
    /*
     *              CONNECTION HANDLER VARIABLES
     */
    public peerProcess peer; // Peer that connections are being handled for
    public int numConnections = 0;

    /*
     *              CONSTRUCTOR
     */
    public ConnectionHandler(peerProcess peer) {
        this.peer = peer;
    }

    /*
     *              REQUEST A TCP CONNECTION
     */
    public void requestConnection(int peerID, String hostName, int portNum, boolean hasFile) {
        // Create connection object
        Connection c = new Connection(peer, peerID, hostName, portNum, hasFile);

        // Establish the connection
        connect(c);

        // Add c to peers list
        peer.connections.add(c);
        numConnections++;
    }

    /*
     *              RECEIVE A TCP CONNECTION REQUEST
     */
    public void receiveConnection(ServerSocket listenSocket) throws Exception {
        Connection c = new Connection(peer);
        c.setSocket(listenSocket.accept());
        new Handler(c).start();
        System.out.println("Peer: " + (numConnections) + " is connected!");
        numConnections++;
    }
    
    /*
     *              LOOP TO LISTEN FOR TCP CONNECTION REQUESTS
     */
    public void listen() throws Exception {
        System.out.println("Connection Handler is listening on port: " + peer.portNum);
            ServerSocket listenSocket = new ServerSocket(peer.portNum);
            try {
                while (true) {
                    receiveConnection(listenSocket);
                }
            } finally {
                listenSocket.close();
            }
    }

    /*
     *              MAKE THE TCP CONNECTION
     */
    void connect(Connection connection) {
        try {
            // create a socket to connect to the server
            connection.socket = new Socket("localhost", connection.portNum);

            // initialize inputStream and outputStream
            connection.out = new ObjectOutputStream(connection.socket.getOutputStream());
            connection.out.flush();
            connection.in = new ObjectInputStream(connection.socket.getInputStream());

            clientHandshake(connection);
            System.out.println("Connected to " + connection.hostName + " in port " + connection.portNum);
        } catch (ConnectException e) {
            System.err.println("Connection refused. You need to initiate a server first.");
        } catch (UnknownHostException unknownHost) {
            System.err.println("You are trying to connect to an unknown host!");
        } catch (IOException ioException) {
            ioException.printStackTrace();
        } finally {
            // Close connections
            try {
                connection.in.close();
                connection.out.close();
                connection.socket.close();
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }
    }

    void clientHandshake(Connection connection) {
        // Send initial handshake message when you connect
        sendHandshakeMessage(connection);

        try {
            // Wait for handshake response from server
            String handshakeResponse = (String) connection.in.readObject();
            System.out.println("Received handshake Response: " + handshakeResponse);

            // Verify Handshake Response
            boolean verified = verifyHandshakeResponse(handshakeResponse, connection.peerID);

            if (verified) {
                System.out.println("Handshake verified.");
            } else {
                throw new RuntimeException();
            }

        } catch (IOException ioException) {
            ioException.printStackTrace();
        } catch (ClassNotFoundException e) {
            System.err.println("Class not found");
        } catch (RuntimeException e) {
            System.err.println("Handshake from server failed to verify");
        }
    }

    public void sendHandshakeMessage(Connection connection) {
        String header = "P2PFILESHARINGPROJ";
        String zeros = "0000000000";
        String id = String.valueOf(connection.peerID);

        //Do we want to send as byte[] or as String??
        String msgString = header + zeros + id;
        sendMessage(msgString, connection);
    }

    public boolean verifyHandshakeResponse(String msg, int expectedID) {
        String Header = msg.substring(0, 18);
        String zero = msg.substring(18, 28);
        int receivedID = Integer.parseInt(msg.substring(28, 32));

        if (!Header.equals("P2PFILESHARINGPROJ") || !zero.equals("0000000000") || expectedID != receivedID) {
            return false;
        }

        return true;
    }

    public void sendMessage(String msg, Connection connection) {
        try {
            // stream write the message
            connection.out.writeObject(msg);
            connection.out.flush();
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }

    /**
     * A handler thread class. Handlers are spawned from the listening
     * loop and are responsible for dealing with a single client's requests.
     */
    private static class Handler extends Thread {
        private String message; // message received from the client
        private String MESSAGE; // uppercase message send to the client
        private Connection connection;
        private Socket socket;
        private ObjectInputStream in; // stream read from the socket
        private ObjectOutputStream out; // stream write to the socket
        private int no; // The index number of the client
        private int clientPeerID;
        private peerProcess peer; // Parent peer of the server

        public Handler(Connection connection) {
            this.connection = connection;
            this.socket = connection.socket;
        }

        public void run() {
            try {
                // initialize Input and Output streams
                out = new ObjectOutputStream(socket.getOutputStream());
                out.flush();
                in = new ObjectInputStream(socket.getInputStream());
                try {
                    message = (String) in.readObject();
                    
                    // Here is where we need to use the handshake to get our peer ID
                    handshake(message);
                    while (true) {
                        message = (String) in.readObject();
                        peer.receiveMessage(message, out, clientPeerID);
                    }
                } catch (/* ClassNotFoundException */ Exception classnot) {
                    System.err.println("Data received in unknown format");
                }
            } catch (IOException ioException) {
                System.out.println("Disconnect with Client " + no);
            } finally {
                // Close connections
                try {
                    in.close();
                    out.close();
                    socket.close();
                } catch (IOException ioException) {
                    System.out.println("Disconnect with Client " + no);
                }
            }
        }

        public void handshake(String clientmessage) {
            try {
                /* RECEIVE HANDSHAKE */
                System.out.println("Handshake: " + clientmessage + " from client " + no);

                /* VERIFY HANDSHAKE */
                boolean verified = verifyHandshakeResponse(clientmessage);

                if (verified) {
                    System.out.println("Handshake verified.");
                    sendHandshakeMessage(connection.us.ID);
                    System.out.println("Sent handshake");
                }
                else {
                    throw new RuntimeException();
                }
            } catch (RuntimeException runtimeException) {
                System.err.println("Handshake was not verified");
            }
        }

        public void sendHandshakeMessage(int pID) {
    
            String header = "P2PFILESHARINGPROJ";
            String zeros = "0000000000";
            String id = String.valueOf(pID);
    
            //Do we want to send as byte[] or as String??
            String msgString = header + zeros + id;
            sendMessage(msgString);
        }
    
        // Handshake response verification
        public boolean verifyHandshakeResponse(String msg) {
            String Header = msg.substring(0,18);
            String zero = msg.substring(18,28); 
    
            if (!Header.equals("P2PFILESHARINGPROJ") || !zero.equals("0000000000")) { return false; }
    
            return true;
        }

        //send a message to the output stream
        public void sendMessage(String msg) {
            try{
                out.writeObject(msg);
                out.flush();
                System.out.println("Send message: " + msg + " to Client " + no);
            }
            catch(IOException ioException){
                ioException.printStackTrace();
            }
        }
    }
}
