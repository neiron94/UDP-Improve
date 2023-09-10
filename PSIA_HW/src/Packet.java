import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.util.Arrays;
import java.util.zip.CRC32;

public class Packet {
    private static final int MAX_DATA_SIZE = 1024;

    // Not-data type packets have these numbers in index data block
    public static final int NAME_TYPE_INDEX = -1;
    public static final int SIZE_TYPE_INDEX = -2;
    public static final int HASH_TYPE_INDEX = -3;
    public static final int REPEAT_TYPE_INDEX = -4;
    public static final int STOP_TYPE_INDEX = -5;
    public static final int ACK_TYPE_INDEX = -6;


    // Indexes of service information about the packet in data array

    public static final int crcIndex = 0;     // crc32 code
    public static final int typeIndex = 8;    // packet type
    public static final int sizeIndex = 9;    // size of data block
    public static final int packetIDIndex = 13;   // index of packet relative to all packets
    public static final int dataIndex = 17;     // start of data block
    public static final int serviceInfoSize = 17;  // size of service information block
    public static final int dataBlockSize = MAX_DATA_SIZE - serviceInfoSize;
    public byte[] data;
    public int size;
    public PacketTypes decodedType;
    public int decodedSize;
    public int decodedIndex;
    public byte[] decodedData;
//    public Inet4Address senderIP;   // IP from which this packet was received
//    public int senderPort;          // port from which this packet was received

    public Packet() {
        this.size = MAX_DATA_SIZE;
        this.data = new byte[size];
    }

    public void createPacket(PacketTypes type, int size, int index, byte[] data) {
        this.addTypeInfo(type);
        this.addSizeInfo(size);
        this.addPacketIndex(index);
        this.addData(data);
        this.addCRC32();
    }
    public void decodePacket() {
        decodedType = decodeTypeInfo();
        decodedSize = decodeSizeInfo();
        decodedIndex = decodePacketIndex();
        decodedData = decodeData();
    }

    public void addTypeInfo(PacketTypes type) {
        switch(type) {
            case DATA_TYPE:
                this.data[typeIndex] = (byte)0;
                break;
            case LAST_TYPE:
                this.data[typeIndex] = (byte)1;
                break;
            case NAME_TYPE:
                this.data[typeIndex] = (byte)2;
                break;
            case SIZE_TYPE:
                this.data[typeIndex] = (byte)3;
                break;
            case HASH_TYPE:
                this.data[typeIndex] = (byte)4;
                break;
            case ACK_OK_TYPE:
                this.data[typeIndex] = (byte)5;
                break;
            case REPEAT_TYPE:
                this.data[typeIndex] = (byte)6;
                break;
            case STOP_TYPE:
            this.data[typeIndex] = (byte)7;
                break;
            case ACK_NOK_TYPE:
                this.data[typeIndex] = (byte)8;
                break;
        }
    }
    public PacketTypes decodeTypeInfo() {
        switch(this.data[typeIndex]) {
            case 0:
                return PacketTypes.DATA_TYPE;
            case 1:
                return PacketTypes.LAST_TYPE;
            case 2:
                return PacketTypes.NAME_TYPE;
            case 3:
                return PacketTypes.SIZE_TYPE;
            case 4:
                return PacketTypes.HASH_TYPE;
            case 5:
                return PacketTypes.ACK_OK_TYPE;
            case 6:
                return PacketTypes.REPEAT_TYPE;
            case 7:
                return PacketTypes.STOP_TYPE;
            case 8:
                return PacketTypes.ACK_NOK_TYPE;
        }
        return null;
    }

    public void addSizeInfo(int size) { System.arraycopy(Utils.intToByteArray(size), 0, data, sizeIndex, 4); }
    public int decodeSizeInfo() { return Utils.byteArrayToInt(Arrays.copyOfRange(data, sizeIndex, sizeIndex + 4)); }

    public void addPacketIndex(int index) { System.arraycopy(Utils.intToByteArray(index), 0, data, packetIDIndex, 4); }
    public int decodePacketIndex() { return Utils.byteArrayToInt(Arrays.copyOfRange(data, packetIDIndex, packetIDIndex + 4)); }

    public void addData(byte[] data) { System.arraycopy(data, 0, this.data, dataIndex, this.decodeSizeInfo()); }
    public byte[] decodeData() { return Arrays.copyOfRange(data, dataIndex, dataIndex + this.decodeSizeInfo()); }

    public void addCRC32() {
        CRC32 crc = new CRC32();
        crc.update(this.data, typeIndex, size - 8);
        long crcValue = crc.getValue();
        System.arraycopy(Utils.longToByteArray(crcValue), 0, data, crcIndex, 8);
    }

    public long decodeCRC32() { return Utils.byteArrayToLong(Arrays.copyOfRange(data, crcIndex, crcIndex + 8)); }

    public boolean CrcIsOk() {
        CRC32 crc = new CRC32();
        crc.update(this.data, typeIndex, size - 8);
        long newCountedCrcValue = crc.getValue();
        long receivedCrcValue = decodeCRC32();

        return newCountedCrcValue == receivedCrcValue;
    }

    public void send(DatagramSocket socket, Inet4Address receiverAddress, int receiverPort) throws IOException {
        DatagramPacket datagramPacket = new DatagramPacket(data, size, receiverAddress, receiverPort);
        socket.send(datagramPacket);
//        System.out.println("Sent");     // DEBUG
//        printPacketInfo();              // DEBUG
    }

    public void sendWithRepeat(DatagramSocket socket, Inet4Address receiverAddress, int receiverPort) throws IOException {
        DatagramPacket datagramPacket = new DatagramPacket(data, size, receiverAddress, receiverPort);
        socket.send(datagramPacket);
        Thread ackWaiter = new Thread(new AckWaiter(this));
        ackWaiter.start();
//        System.out.println("Sent");     // DEBUG
//        printPacketInfo();              // DEBUG
    }

    public void receive(DatagramSocket socket) throws IOException {
        DatagramPacket datagramPacket = new DatagramPacket(data, size);
        socket.receive(datagramPacket);
//        senderIP = (Inet4Address) datagramPacket.getAddress();
//        senderPort = datagramPacket.getPort();
//        System.out.println("Received"); // DEBUG
//        printPacketInfo();              // DEBUG
    }

//    private void printPacketInfo() {    // DEBUG
//        System.out.println("Index of a packet: " + decodePacketIndex());
//        System.out.println("Type of a packet: " + decodeTypeInfo());
//        System.out.println("Size of a packet: " + decodeSizeInfo());
//        System.out.println("CRC of a packet: " + decodeCRC32());
//        System.out.println();
//    }
}
