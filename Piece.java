

public class Piece {
    private int index;
    private byte[] data;
    private int size;
    private boolean isComplete;

    //If Piece
    // public Piece(int index, int size) {
    //     this.index = index;
    //     this.size = size;
    //     this.data = new byte[size];
    //     this.isComplete = false;
    // }

    public Piece(int index, byte[] data) {
        this.index = index;
        this.data = data;
        this.size = data.length;
        this.isComplete = true;
    }

    // Getters
    public int getIndex() {
        return index;
    }

    public byte[] getData() {
        return data;
    }

    public int getSize() {
        return size;
    }

    public boolean isComplete() {
        return isComplete;
    }

    // Setters
    public void setData(byte[] data) {
        this.data = data;
        this.isComplete = true;
    }

    public void setComplete(boolean complete) {
        isComplete = complete;
    }
    
}
