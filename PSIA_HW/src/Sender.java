import java.io.IOException;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.SocketTimeoutException;
import java.util.Arrays;

public class Sender extends User {
    private static int window = 500;
    public static volatile int ackIndex;
    public static void start(byte[] receiverIP, int recPort, int port, String filename) throws Exception {
        // Set info for communication
        anotherAddress = (Inet4Address) Inet4Address.getByAddress(receiverIP);
        anotherPort = recPort;
        myPort = port;
        socket = new DatagramSocket(myPort);
        System.out.println("Socket was created");
        socket.setSoTimeout(SOCKET_TIMEOUT);
        System.out.println("Socket's timeout is " + SOCKET_TIMEOUT + "\n");

        // Prepare packets
        byte[] fileData = Utils.readFileInByteArray(filename.trim());
        byte[] encodedHash = Utils.countHash(fileData);
        int lastPacketSize = fileData.length - (fileData.length / Packet.dataBlockSize * Packet.dataBlockSize);
        byte[][] packets = Utils.splitFileIntoPackets(fileData, lastPacketSize);
        // Add file name, file size and hash to packets
        System.arraycopy(filename.getBytes(), 0, packets[0], 0, filename.length());
        System.arraycopy(Utils.intToByteArray(fileData.length), 0, packets[1], 0, 4);
        System.arraycopy(encodedHash, 0, packets[2], 0, encodedHash.length);

        // Sending
        while (true) {
            sendData(packets, lastPacketSize, filename.length(), encodedHash.length);
            if (!isRepeatSending()) {
                System.out.println("Hashes are equal, stop sending.");
                break;
            }
            else {
                System.out.println("Hashes are different, repeat sending.");
            }
        }
        socket.close();
    }

    private static void sendData(byte[][] packets, int lastPacketSize, int fileNameSize, int hashSize) throws IOException {

        ackIndex = -1;
        int sendIndex = 0;

        while (ackIndex < packets.length - 1) {
            // Send packets
            while (window > 0 && sendIndex < packets.length) {
                // Create packet
                Packet packet = new Packet();
                int dataSize;
                PacketTypes type;
                if (sendIndex == 0) {
                    type = PacketTypes.NAME_TYPE;
                    dataSize = fileNameSize;
                }
                else if (sendIndex == 1) {
                    type = PacketTypes.SIZE_TYPE;
                    dataSize = 4;
                }
                else if (sendIndex == 2) {
                    type = PacketTypes.HASH_TYPE;
                    dataSize = hashSize;
                }
                else if (sendIndex == packets.length - 1) {
                    type = PacketTypes.LAST_TYPE;
                    dataSize = lastPacketSize;
                }
                else {
                    type = PacketTypes.DATA_TYPE;
                    dataSize = Packet.dataBlockSize;
                }

                packet.createPacket(type, dataSize, sendIndex, packets[sendIndex]);

                packet.sendWithRepeat(socket, anotherAddress, anotherPort);

                System.out.println("Packet " + sendIndex + " is sent\n");
                sendIndex++;
                window--;
            }

            // Receive acknowledge
            int ackIndexBefore = ackIndex;
            getAcknowledge();
            int ackIndexAfter = ackIndex;
            window += ackIndexAfter - ackIndexBefore;
        }
    }

    private static boolean isRepeatSending() throws IOException {
        Packet packet = new Packet();

        while (true) {
            try {
                packet.receive(socket);
                packet.decodePacket();
                if (packet.CrcIsOk() && (packet.decodedType == PacketTypes.STOP_TYPE || packet.decodedType == PacketTypes.REPEAT_TYPE)) {
                    sendAck(PacketTypes.ACK_OK_TYPE, packet.decodedIndex);
                    break;
                }
                else
                    sendAck(PacketTypes.ACK_NOK_TYPE, packet.decodedIndex);
            }
            catch (SocketTimeoutException e) {
                System.out.println("REPEAT/STOP packet wasn't received");
            }
        }

        return packet.decodedType == PacketTypes.REPEAT_TYPE;
    }

    private static void getAcknowledge() {
        try {
            Packet ackPacket = new Packet();
            ackPacket.receive(socket);
            ackPacket.decodePacket();

            if (ackPacket.CrcIsOk() && ackPacket.decodedType == PacketTypes.ACK_OK_TYPE) {
                int receivedAckIndex = Utils.byteArrayToInt(Arrays.copyOfRange(ackPacket.decodedData, 0, 4));
                System.out.println("Got an acknowledge for a packet " + receivedAckIndex + "\n");
                if (receivedAckIndex > ackIndex)
                    ackIndex = receivedAckIndex;
            }
        }
        catch (IOException ignored) {
        }
    }
}