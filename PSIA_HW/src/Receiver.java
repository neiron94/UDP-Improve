import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;

public class Receiver extends User {
    private static List<Byte> ackTable;
    private static int ackCount;
    private static volatile boolean lastStop;
    public static void start(int port, byte[] senderIP, int senderPort) throws Exception {
        // Set info for communication
        myPort = port;
        anotherAddress = (Inet4Address) Inet4Address.getByAddress(senderIP);
        anotherPort = senderPort;
        socket = new DatagramSocket(myPort);
        System.out.println("\nSocket was created");
        socket.setSoTimeout(SOCKET_TIMEOUT);
        System.out.println("Socket's timeout is " + SOCKET_TIMEOUT);
        System.out.println("Waiting for the Sender...\n");

        byte[] fileData;
        String filename;
        int fileSize;
        byte[] receivedHash;

        // Receiving
        while (true) {
            List<byte[]> packets = receiveData();

            filename = new String(packets.get(0)).trim();
            fileSize = Utils.byteArrayToInt(packets.get(1));
            receivedHash = packets.get(2);
            // Build file and check sha hash
            fileData = Utils.mergePacketsIntoFile(packets, fileSize);
            if (Utils.checkHash(receivedHash, fileData)) {
                System.out.println("Hashes are equal, stop receiving.");
                sendLastPacket(PacketTypes.STOP_TYPE);
                break;
            } else {
                System.out.println("Hashes are different, repeat receiving.");
                sendLastPacket(PacketTypes.REPEAT_TYPE);
            }
        }

        // Save file
        try (FileOutputStream outputStream = new FileOutputStream(filename)) {
            outputStream.write(fileData, 0, fileSize);
        }
        socket.close();
    }


    private static List<byte[]> receiveData() throws IOException {
        List<byte[]> packets = new ArrayList<>(1000);
        ackTable = new ArrayList<>(1000);
        for (int i = 0; i < 1000; i++) {
            packets.add(new byte[1]);
            ackTable.add((byte) 0);
        }
        ackCount = 0;
        boolean isGotSize = false;
//        byte[][] packets = new byte[packetsCount][Packet.dataBlockSize];

        int packetsCount = 100000;    // infinity
        while(ackCount < packetsCount) {
            Packet packet = new Packet();
            receiveDataPacket(packet);
            packets.set(packet.decodedIndex, packet.decodedData);

            if (!isGotSize && ackTable.get(1) == 1) {
                isGotSize = true;
                packetsCount = Utils.byteArrayToInt(packets.get(1)) / Packet.dataBlockSize + 1 + 3;
                for (int i = 1000; i < packetsCount; i++) {
                    packets.add(new byte[1]);
                    ackTable.add((byte) 0);
                }
            }
        }


        return packets;
    }

    private static void receiveDataPacket(Packet packet) throws IOException {
        while (true) {
            try {
                packet.receive(socket);
                if (packet.CrcIsOk()) {
                    packet.decodePacket();
//                    System.out.println("Packet " + packet.decodedIndex + " is received");

                        // If this packet has been already received
                        if (ackTable.get(packet.decodedIndex) != 1) {
                            ackCount++;
                            ackTable.set(packet.decodedIndex, (byte) 1);
                        }

                        sendAck(PacketTypes.ACK_OK_TYPE, lastOkIndex());
                        break;
                }
                else {
                    sendAck(PacketTypes.ACK_NOK_TYPE, packet.decodedIndex);
                }
            }
            catch (SocketTimeoutException ignored) {
            }
        }
    }

    private static int lastOkIndex() {
        for (int i = 0; i < ackTable.size(); i++) {
            if (ackTable.get(i) != 1)
                return i - 1;
        }
        return ackTable.size() - 1;
    }

    private static void sendLastPacket(PacketTypes type) throws IOException {

        Packet packet = new Packet();
        lastStop = false;

        int index = type == PacketTypes.REPEAT_TYPE ? Packet.REPEAT_TYPE_INDEX : Packet.STOP_TYPE_INDEX;
        packet.createPacket(type, 0, index, new byte[1]);

        Thread stopper = new Thread(new Stopper());
        stopper.start();
        do {
            if (!isStillSending())
                packet.send(socket, anotherAddress, anotherPort);
        } while (noAcknowledge(packet) && !lastStop);
    }

    private static boolean isStillSending() {
        try {
            Packet packet = new Packet();
            packet.receive(socket);
            if (packet.CrcIsOk()) {
                packet.decodePacket();
//                System.out.println("Packet " + packet.decodedIndex + " is received");

                sendAck(PacketTypes.ACK_OK_TYPE, lastOkIndex());
            }
            else {
                sendAck(PacketTypes.ACK_NOK_TYPE, packet.decodedIndex);
            }
            return true;
        }
        catch (SocketTimeoutException ignored) {
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return false;
    }

    private static class Stopper implements Runnable {
        @Override
        public void run() {
            try {
                Thread.sleep(3000);
                lastStop =true;
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
