import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;

public class Utils {
    // Class for functions, which aren't logically connected to Sender/Receiver functionality

    public static byte[] readFileInByteArray(String filename) throws IOException {
        try (FileInputStream file = new FileInputStream(filename)) {
            byte[] fileAsByteArray = new byte[file.available()];
            file.read(fileAsByteArray, 0, file.available());
            return fileAsByteArray;
        }
    }

    public static byte[][] splitFileIntoPackets(byte[] data, int lastPacketSize) {
        int packetsCount = data.length / Packet.dataBlockSize + 1 + 3;  // +3 for filename, size and hash
        byte[][] packets = new byte[packetsCount][Packet.dataBlockSize];
        for (int i = 0; i < packetsCount - 3; i++) {
            int copyCount = i == packetsCount - 4 ? lastPacketSize : Packet.dataBlockSize;
            System.arraycopy(data, i * Packet.dataBlockSize, packets[i + 3], 0, copyCount);
        }

        return packets;
    }

    public static byte[] mergePacketsIntoFile(List<byte[]> packets, int fileSize) {
        byte[] fileData = new byte[fileSize];
        int lastPacketSize = fileSize - (fileSize / Packet.dataBlockSize * Packet.dataBlockSize);
        int packetsCount = fileSize / Packet.dataBlockSize + 1 + 3;
        for (int i = 0; i < packetsCount - 3; i++) {
            int copyCount = i == packetsCount - 4 ? lastPacketSize : Packet.dataBlockSize;
            System.arraycopy(packets.get(i + 3), 0, fileData, i * Packet.dataBlockSize, copyCount);
        }

        return fileData;
    }


    public static byte[] countHash(byte[] data) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return digest.digest(data);
    }

    public static boolean checkHash(byte[] receivedHash, byte[] fileData) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] countedHash = digest.digest(fileData);

        return Arrays.equals(receivedHash, 0, countedHash.length, countedHash, 0, countedHash.length);
    }


    public static byte[] intToByteArray(int value) {
        byte[] result = new byte[4];
        for (int i = 0; i < 4; i++) {
            int shift = (3 - i) * 8;
            result[i] = (byte)((value >> shift) & 0xff);
        }
        return result;
    }

    public static int byteArrayToInt(byte[] array) {
        int result = 0;
        for (int i = 0; i < 4; i++) {
            int shift = (3 - i) * 8;
            result += ((int)array[i] << shift) & (0xff << shift);
        }
        return result;
    }
    public static byte[] longToByteArray(long value) {
        byte[] result = new byte[8];
        for (int i = 0; i < 8; i++) {
            int shift = (7 - i) * 8;
            result[i] = (byte)((value >> shift) & 0xff);
        }
        return result;
    }

    public static long byteArrayToLong(byte[] array) {
        long result = 0;
        for (int i = 0; i < 8; i++) {
            int shift = (7 - i) * 8;
            result += ((long)array[i] << shift) & (0xffL << shift);
        }
        return result;
    }
}
