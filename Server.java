import java.net.*;

import javax.print.DocFlavor.STRING;

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

    private static class Handler extends Thread {
        private Socket clientSocket;
        private ObjectInputStream in; //stream read from the socket
        private ObjectOutputStream out; //stream write to the socket
        private String message; //message received from the client
        private String MESSAGE; //uppercase message send to the client
        private String clientPeerID;
        private peerProcess serverPeerIntance; // Parent peer of the server

        public Handler(Socket clientSocket, peerProcess serverPeer) {
            this.clientSocket = clientSocket;
            this.serverPeerIntance = serverPeer;
        }

        public void run() {
            try{
                //initialize Input and Output streams
                out = new ObjectOutputStream(clientSocket.getOutputStream());
                out.flush();
                in = new ObjectInputStream(clientSocket.getInputStream());
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
                    Neighbor neighbor = new Neighbor(this.serverPeerIntance, Integer.parseInt(clientPeerID), false, in, out);
                    this.serverPeerIntance.neighbors.add(neighbor);

                    // log connection received
                    serverPeerIntance.getLogger().generateTCPLogReceiver(clientPeerID);
                    
                    handleBitfield(neighbor, in);
                    this.serverPeerIntance.messenger.sendMessage(MessageType.BITFIELD, out, in, Integer.parseInt(clientPeerID), -1);
                    
                    // receive stream of messages
                    while(true) {
                        serverPeerIntance.receiveMessage(out, in, Integer.parseInt(clientPeerID));
                    }
                }
                catch(Exception classnot){
                    System.err.println("Data received in unknown format");
                }
            }
            catch(IOException ioException){
                System.out.println("Disconnect with Client " + clientPeerID);
            }
            finally{
                //Close connections
                try{
                    in.close();
                    out.close();
                    clientSocket.close();
                }
                catch(IOException ioException){
                    System.out.println("Disconnect with Client " + clientPeerID);
                }
            }
        }

        //send a message to the output stream
        private void sendMessage(String msg) {
            try{
                out.writeObject(msg);
                out.flush();
                System.out.println("Send message: " + msg + " to Client " + this.clientPeerID);
            }
            catch(IOException ioException){
                ioException.printStackTrace();
            }
        } 
        
        private void handleBitfield(Neighbor neighbor, ObjectInputStream in){
            try{
                int length = in.readInt();
                int type = in.readByte();
                byte[] payload = in.readNBytes(length);
    
                Bitfield neighborBitfield = new Bitfield(payload);
                neighbor.updatePeerBitfield(neighborBitfield);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
