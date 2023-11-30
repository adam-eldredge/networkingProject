
import java.util.*;
import java.io.*;
import java.text.*;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.logging.FileHandler;
import java.util.logging.SimpleFormatter;

public class PeerLogger{
    private String logFileName;
    private String peerId;
    private Logger logger;
    private FileHandler fh;
    private SimpleDateFormat dateFormat = null;

    public PeerLogger(int peerId){
        this.peerId = Integer.toString(peerId);
        startLogger();
    }
    public void startLogger(){
        try{
            this.dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String logFileDir = "project";
            File file = new File(logFileDir);
            file.mkdir();
            this.logFileName = logFileDir + "/log_peer_" + this.peerId + ".log";
            this.fh = new FileHandler(this.logFileName, false);

            System.setProperty("java.util.logging.SimpleFormatter.format", "%5$s %n");

            
            this.fh.setFormatter(new SimpleFormatter());
            this.logger = Logger.getLogger("PeerLogger");
            this.logger.setUseParentHandlers(false);
            
            this.logger.addHandler(this.fh);

        }catch(IOException e){
            e.printStackTrace();
        }
    }
    public synchronized void generateTCPLogSender(String peer){
        Calendar cal = Calendar.getInstance();
        String currTime = this.dateFormat.format(cal.getTime());
        this.logger.log(Level.INFO,
        "[" + currTime + "]: Peer [" + this.peerId + "] makes a connection to Peer " + "[" + peer + "]" + ".\n");
    }
    public synchronized void generateTCPLogReceiver(String peer){
        Calendar cal = Calendar.getInstance();
        String currTime = this.dateFormat.format(cal.getTime());
        this.logger.log(Level.INFO,
        "[" + currTime + "]: Peer [" + this.peerId + "] is connected from Peer " + "[" + peer + "]" + ".\n"); 
    }
    public synchronized void changePreferredNeighbors(List<String> neighbor){
        Calendar cal = Calendar.getInstance();
        String currTime = this.dateFormat.format(cal.getTime());
        String neighborList = "";
        for(String n: neighbor){
            neighborList += n + ",";
        }
        if(neighborList.length() > 0){
            neighborList = neighborList.substring(0, neighborList.length() - 1);
        }
        this.logger.log(Level.INFO,
            "[" + currTime + "]: Peer [" + this.peerId + "] has the preferred neighbors " + "[" + neighborList + "]" + ".\n");
    }
    public synchronized void changeOptimisticallyUnchokedNeighbors(String peer){
        Calendar cal = Calendar.getInstance();
        String currTime = this.dateFormat.format(cal.getTime());
        this.logger.log(Level.INFO,
            "[" + currTime + "]: Peer [" + this.peerId + "] has the optimistically unchoked neighbor " + "[" + peer + "]" + ".\n");
    }
    public synchronized void unchokedNeighbor(String peer){
        Calendar cal = Calendar.getInstance();
        String currTime = this.dateFormat.format(cal.getTime());
        this.logger.log(Level.INFO,
            "[" + currTime + "]: Peer [" + this.peerId + "] is unchoked by " + "[" + peer + "]" + ".\n");
    }
    public synchronized void chokedNeighbor(String peer){
        Calendar cal = Calendar.getInstance();
        String currTime = this.dateFormat.format(cal.getTime());
        this.logger.log(Level.INFO,
            "[" + currTime + "]: Peer [" + this.peerId + "] is choked by " + "[" + peer + "]" + ".\n");
    }
    public synchronized void receiveHave(String peer, int pieceIndex){
        Calendar cal = Calendar.getInstance();
        String currTime = this.dateFormat.format(cal.getTime());
        this.logger.log(Level.INFO,
            "[" + currTime + "]: Peer [" + this.peerId + "] received the 'have' message from " + "[" + peer + "]" + " for the piece " + "[" + String.valueOf(pieceIndex) + "]" + ".\n");
    }
    public synchronized void receiveInterested(String peer){
        Calendar cal = Calendar.getInstance();
        String currTime = this.dateFormat.format(cal.getTime());
        this.logger.log(Level.INFO,
            "[" + currTime + "]: Peer [" + this.peerId + "] received the 'interested' message from " + "[" + peer + "]" + ".\n");
    }
    public synchronized void receiveNotInterested(String peer){
        Calendar cal = Calendar.getInstance();
        String currTime = this.dateFormat.format(cal.getTime());
        this.logger.log(Level.INFO,
            "[" + currTime + "]: Peer [" + this.peerId + "] received the 'not interested' message from " + "[" + peer + "]" + ".\n");
    }
    public synchronized void downloadPiece(String peer, int pieceIndex, int numPieces){
        Calendar cal = Calendar.getInstance();
        String currTime = this.dateFormat.format(cal.getTime());
        this.logger.log(Level.INFO,
            "[" + currTime + "]: Peer [" + this.peerId + "] has downloaded the piece [" + String.valueOf(pieceIndex)
            + "] from [" + peer + "]. Now the number of pieces it has is [" + String.valueOf(numPieces) + "].\n"
        );

    }
    public synchronized void completeDownload(){
        Calendar cal = Calendar.getInstance();
        String currTime = this.dateFormat.format(cal.getTime());
        this.logger.log(Level.INFO,
            "[" + currTime + "]: Peer [" + this.peerId + "] has downloaded the complete file."
        );
    }
    public void closeLogger(){
        try{
            if(this.fh != null){
                this.fh.close();
            }
        }catch(Exception e){
            System.out.println("Error in closing the logger");
            e.printStackTrace();
        }
    }

}
