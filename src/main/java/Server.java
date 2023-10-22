package src.main.java;
// Code provided to us from the CNT4007 canvas page files/Project/Sample Server.java

import java.net.*;
import java.io.*;

public class Server extends Peer {
    private static final int sPort = 8000; //The server will be listening on this port number
    private static Peer peer = null;

    public Server(Peer p) {
        peer = p;
    }

    public static void main(String[] args) throws Exception {
        System.out.println("The server is running.");
        ServerSocket listener = new ServerSocket(sPort);
        int clientNum = 1;
        try {
            while(true) {
                new Handler(listener.accept(),clientNum, peer).start();
                System.out.println("Client " + clientNum + " is connected!");
                clientNum++;
            }
        } finally {
            listener.close();
        }
    }

    /**
    * A handler thread class. Handlers are spawned from the listening
    * loop and are responsible for dealing with a single client's requests.
    */
    private static class Handler extends Thread {
        private String message; //message received from the client
        private String MESSAGE; //uppercase message send to the client
        private Socket connection;
        private ObjectInputStream in; //stream read from the socket
        private ObjectOutputStream out; //stream write to the socket
        private int no; //The index number of the client
        private Peer peer; // Parent peer

        // state
        // their bitfield
        // etc

        public Handler(Socket connection, int no, Peer peer) {
            this.connection = connection;
            this.no = no;
            this.peer = peer;
        }

        public void run() {
            try{
                //initialize Input and Output streams
                out = new ObjectOutputStream(connection.getOutputStream());
                out.flush();
                in = new ObjectInputStream(connection.getInputStream());
                try{
                    handshake(in);

                    while(true) {
                        message = (String)in.readObject();
                        //handleMessage(message);

                        System.out.println("Receive message: " + message + " from client " + no);
                        //Capitalize all letters in the message
                        MESSAGE = message.toUpperCase();
                        //send MESSAGE back to the client
                        sendMessage(MESSAGE);
                    }
                }
                catch(ClassNotFoundException classnot){
                    System.err.println("Data received in unknown format");
                }
            }
            catch(IOException ioException){
                System.out.println("Disconnect with Client " + no);
            }
            finally{
                //Close connections
                try{
                    in.close();
                    out.close();
                    connection.close();
                }
                catch(IOException ioException){
                    System.out.println("Disconnect with Client " + no);
                }
            }
        }


        public void handshake(ObjectInputStream in) {
            try {
                //Wait for handshake request from client
                String clientmessage = (String)in.readObject();
                System.out.println("Handshake: " + clientmessage + " from client " + no);

                //   Verify Correct Handshake
                //------(Insert Code here)------
                //   Continue to sending messages

                //Send handshake response back to client
                //sendHandshakeMessage();
                System.out.println("Sent handshake");
            } catch (IOException ioException) {
                ioException.printStackTrace();
            } catch (ClassNotFoundException e) {
                System.err.println("Class not found");
            }
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