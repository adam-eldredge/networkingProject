import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public abstract class Connection extends Thread {
    protected Socket socket;
    protected volatile ObjectInputStream in;
    protected volatile ObjectOutputStream out;
    protected volatile boolean isTerminated = false;

    Connection(Socket socket) {
        this.socket = socket;
        try{
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(socket.getInputStream());
        }catch(IOException ioException){
            ioException.printStackTrace();
        }
    }
    Connection(String hostName, int portNum) {
        try{
            socket = new Socket("localhost", portNum);
            //initialize inputStream and outputStream
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(socket.getInputStream());
        }catch(IOException ioException){
            ioException.printStackTrace();
        }
    }

    abstract public void run();

    protected void sendMessage(String msg){
        try{
                out.writeObject(msg);
                out.flush();
            }
            catch(IOException ioException){
                ioException.printStackTrace();
            }
    } 
    public void closeConnection(){
        try{
            in.close();
            out.close();
            socket.close();
        }
        catch(IOException ioException){
            ioException.printStackTrace();
        }
    };
    public void terminate() {
        isTerminated = true;
    }
    //Getters 
    public ObjectOutputStream getOutputStream() {
        return out;
    }
    public ObjectInputStream getInputStream() {
        return in;
    }
}
