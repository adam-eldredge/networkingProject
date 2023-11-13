public class Bitfield {
    private byte[] data;

    public Bitfield(byte[] data){
        this.data = data;
    }

    public boolean hasPiece(int index) {
        int byteIndex = index / 8;
        int offset = index % 8;
        return (data[byteIndex] >> (7 - offset) & 1) != 0;
    }

    public void setPiece(int index) {
        int byteIndex = index / 8;
        int offset = index % 8;
        data[byteIndex] |= 1 << (7 - offset);
    }
}
