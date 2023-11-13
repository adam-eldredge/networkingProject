public class Bitfield {
    private byte[] data;
    private int size;
    
    public Bitfield(byte[] data){
        this.data = data;
    }
    
    public Bitfield(int size) {
        int byteSize = (size + 7) / 8;
        this.size = byteSize;
        this.data = new byte[byteSize];
    }
    
    public byte[] getData() {
        return data;
    }
    
    public void setData(byte[] data) {
        this.data = data;
    }
    
    public int getSize() {
        return size;
    }
    
    public void setSize(int size) {
        this.size = size;
    }
    
    public void setFull(){
        for (int i = 0; i < size; i++) {
            data[i] = Byte.MAX_VALUE;
        }
    }

    public void setEmpty(){
        for (int i = 0; i < size; i++) {
            data[i] = 0;
        }
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
