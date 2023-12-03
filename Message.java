import java.io.Serializable;

public class Message implements Serializable {
    int fromID;
    int toID;
    MessageType type;
    int length;
    byte[] payload = null;
    int index = -1;

    public Message(MessageType type) {
        this.type = type;
    }
}
