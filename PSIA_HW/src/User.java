import java.io.IOException;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.util.Arrays;

public class User {
    protected static Inet4Address myAddress;
    protected static int myPort;
    protected static Inet4Address anotherAddress;
    protected static int anotherPort;
    protected static DatagramSocket socket;
    protected static final int SOCKET_TIMEOUT = 2000;

    protected static void sendAck(PacketTypes type, int packetNumber) throws IOException {
        Packet ackPacket = new Packet();
        byte[] indexAsArray = Utils.intToByteArray(packetNumber);

        ackPacket.createPacket(type, indexAsArray.length, Packet.ACK_TYPE_INDEX, indexAsArray);
        ackPacket.send(socket, anotherAddress, anotherPort);
//        System.out.println("Acknowledge for packet " + packetNumber + " is sent\n");
    }

    protected static boolean noAcknowledge(Packet packet) {
        try {
            packet.decodePacket();
            Packet ackPacket = new Packet();
            ackPacket.receive(socket);
            if (ackPacket.CrcIsOk()) {
                ackPacket.decodePacket();

                int ackIndex = Utils.byteArrayToInt(Arrays.copyOfRange(ackPacket.decodedData, 0, 4));

                if (ackPacket.decodedType == PacketTypes.ACK_OK_TYPE && ackIndex == packet.decodedIndex) {
//                    System.out.println("Got an acknowledge for a packet " + packet.decodedIndex);
                    return false;
                }
            }
        }
        catch (IOException ignored) {
//            System.out.println("Didn't get an acknowledge for a packet " + packet.decodedIndex);
        }
        return true;
    }
}
