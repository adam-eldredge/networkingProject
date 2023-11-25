import java.net.*;


import java.io.*;

public class Server extends Thread{
    
    peerProcess hostPeer;
    int portNum;
    ServerSocket serverSocket = null;

    public Server(peerProcess peer, int portNum) {
        this.hostPeer = peer;
        this.portNum = portNum;
    }

    public void run(){
        System.out.println("The server is running on port: " + portNum);
        try {
            serverSocket = new ServerSocket(portNum);
 
            while(true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected");
                new Handler(clientSocket, this.hostPeer).start();
            }
        }catch (IOException e) {
            e.printStackTrace();
        } finally {
            if(serverSocket != null){
                try{
                    serverSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    /**
    * A handler thread class. Handlers are spawned from the listening
    * loop and are responsible for dealing with a single client's requests.
    */

    private static class Handler extends Connection {

        private String message; //message received from the client
        private String MESSAGE; //uppercase message send to the client
        private String clientPeerID;
        private peerProcess serverPeerIntance; // Parent peer of the server

        public Handler(Socket clientSocket, peerProcess serverPeer) {
            super(clientSocket);
            this.serverPeerIntance = serverPeer;
        }

        @Override
        public void run() {
            try{
                // receive hansdshake
                message = (String)in.readObject();
                String header = message.substring(0,18);
                String zero = message.substring(18,28); 
                clientPeerID = message.substring(28, 32);
                // send handshake
                MESSAGE = header + zero + serverPeerIntance.ID;
                sendMessage(this.MESSAGE);
                // Add neighbor here
                Neighbor neighbor = new Neighbor(this, Integer.valueOf(clientPeerID), false, ConnectionType.SERVER);
                this.serverPeerIntance.neighbors.add(neighbor);
                // log connection received
                serverPeerIntance.getLogger().generateTCPLogReceiver(clientPeerID);
                
                // Send bitfield
                serverPeerIntance.sendMessage(MessageType.BITFIELD, out, in,  Integer.parseInt(clientPeerID), -1);

                // receive stream of messages
                while(true) {
                    serverPeerIntance.receiveMessage(out, in, Integer.parseInt(clientPeerID));
                }
            }
            catch(Exception classnot){
                System.err.println("Data received in unknown format");
            }
            finally{
                //Close connections
                try{
                    in.close();
                    out.close();
                    socket.close();
                }
                catch(IOException ioException){
                    System.out.println("Disconnect with Client " + clientPeerID);
                }
            }
        }

    }
}
