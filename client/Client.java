// Code provided to us from the CNT4007 canvas page files/Project/Sample Client.java

import java.net.*;
import java.io.*;

public class Client extends Peer {

    Socket requestSocket; //socket connect to the server
    ObjectOutputStream out; //stream write to the socket
    ObjectInputStream in; //stream read from the socket
    String message; //message send to the server
    String MESSAGE; //capitalized message read from the server

    public void Client() {}

    void run() {
        try {
            //create a socket to connect to the server
            requestSocket = new Socket("localhost", 8000);
            System.out.println("Connected to localhost in port 8000");

            //initialize inputStream and outputStream
            out = new ObjectOutputStream(requestSocket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(requestSocket.getInputStream());

            handshake(in);

            //get Input from standard input
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));

            while(true) {
                System.out.print("Hello, please input a sentence: ");
                //read a sentence from the standard input
                message = bufferedReader.readLine();
                //Send the sentence to the server
                sendMessage(message);

                //Receive the upperCase sentence from the server
                MESSAGE = (String)in.readObject();
                //show the message to the user
                System.out.println("Receive message: " + MESSAGE);
            }
        }
        catch (ConnectException e) {
            System.err.println("Connection refused. You need to initiate a server first.");
        }
        catch ( ClassNotFoundException e ) {
            System.err.println("Class not found");
        }
        catch(UnknownHostException unknownHost){
            System.err.println("You are trying to connect to an unknown host!");
        }
        catch(IOException ioException){
            ioException.printStackTrace();
        }
        finally{
            //Close connections
            try{
                in.close();
                out.close();
                requestSocket.close();
            }
            catch(IOException ioException){
                ioException.printStackTrace();
            }
        }
    }

    void handshake(ObjectInputStream in){
        //Send initial handshake message when you connect
        peer.sendHandshakeMessage();
        System.out.println("Sent handshake");

        try{
            //Wait for handshake response from server
            String handshakeResponse = (String)in.readObject();
            System.out.println("Received handshake Response: " + handshakeResponse);

            //   Verify Handshake Response
            peer.verifyHandshakeResponse(handshakeResponse, 0/* [INSERT EXPECTED PEER ID] */);

        }catch(IOException ioException){
            ioException.printStackTrace();
        }
        catch (ClassNotFoundException e ) {
            System.err.println("Class not found");
        }
    }

    //main method
    public static void main(String args[]) {
        Client client = new Client();
        client.run();
    }
}
