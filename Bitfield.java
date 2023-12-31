import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Vector;

public class Bitfield {
    private volatile byte[] data;
    private int byteSize;
    private int bitSize;
    private int numPieces = 0;
    volatile HashMap<Integer, Vector<Integer>> reqIdx = new HashMap<>();

    public Bitfield(byte[] data) {
        this.data = data;
    }

    public Bitfield(int size) {
        this.bitSize = size;
        this.byteSize = (size + 7) / 8;
        this.data = new byte[this.byteSize];
    }

    public byte[] getData() {
        return data;
    }

    public boolean requested(int index) {
        for (Map.Entry<Integer, Vector<Integer>> entry : reqIdx.entrySet()) {
            if (entry.getValue().contains(index)) return true;
        }
        return false;
    }

    public void addReqIdx(int peerID, int index) {
        if (this.reqIdx.get(peerID) == null) {
            this.reqIdx.put(peerID, new Vector<>());
        }
        this.reqIdx.get(peerID).add(index);
    }

    public void clearReqIdx(int peerID) {
        this.reqIdx.get(peerID).clear();
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public int getByteSize() {
        return this.byteSize;
    }

    public void setByteSize(int byteSize) {
        this.byteSize = byteSize;
    }

    public int getBitSize() {
        return this.bitSize;
    }

    public void setBitSize(int bitSize) {
        this.bitSize = bitSize;
    }

    public void setFull() {
        for (int i = 0; i < byteSize; i++) {
            data[i] = (byte) 0xFF;
        }
        this.numPieces = bitSize;
    }

    public void setEmpty() {
        for (int i = 0; i < byteSize; i++) {
            data[i] = 0;
        }
    }

    public boolean hasPiece(int index) {
        // int byteIndex = index / 8;
        // int offset = index % 8;
        // return (data[byteIndex] >> (7 - offset) & 1) != 0;

        // Alternative implementation
        int byteIndex = index / 8;
        int offset = index % 8;
        byte value = data[byteIndex];
        byte mask = (byte) (1 << (7 - offset));
        boolean isBitSet = (value & mask) != 0;
        return isBitSet;
    }

    public void setPiece(int index) {
        int byteIndex = index / 8;
        int offset = index % 8;
        data[byteIndex] |= 1 << (7 - offset);

        incrementPieces();
    }

    private void incrementPieces() {
        this.numPieces++;
    }

    public int getNumPieces() {
        return this.numPieces;
    }

    public boolean checkFull() {
        for (int i = 0; i < this.bitSize; i++) {
            if (this.hasPiece(i) == false) {
                return false;
            }
        }
        return true;
    }
}
