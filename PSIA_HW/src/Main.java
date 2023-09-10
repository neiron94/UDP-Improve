import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * This class contains main function.
 */
public class Main {
    /**
     * Sets receiver/sender mode, IP addresses ans ports.
     * @param args are not used.
     * @throws IOException IOExceptions while input.
     */
    public static void main(String[] args) throws Exception {
        // Set mode (Receiver/Sender)
        String option = "";
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        do {
            System.out.println("Do you want to receive (print 'r') or to send (print 's')?");
            option = reader.readLine();
        } while (!option.equals("r") && !option.equals("s"));
        System.out.println();

        // Set IP addresses and ports
        // Receiver
        if (option.equals("r")) {
            System.out.println("Receiver mode");

            byte[] senderIP = enterPartnerIPAddress(reader);
            int senderPort = enterPartnerPort(reader);
            int myPort = enterYourPort(reader);

            Receiver.start(myPort, senderIP, senderPort);
        }
        // Sender
        else {
            // Input Receiver's IPv4
            System.out.print("Sender mode");

            byte[] receiverIP = enterPartnerIPAddress(reader);
            int receiverPort = enterPartnerPort(reader);
            int myPort = enterYourPort(reader);
            String filename = enterFileToSend(reader);

            Sender.start(receiverIP, receiverPort, myPort, filename);
        }
        reader.close();
    }

    private static String enterFileToSend(BufferedReader reader) throws IOException {
        System.out.print("\nChoose a file to send: ");
        return reader.readLine().trim();
    }

    private static int enterPartnerPort(BufferedReader reader) throws IOException {
        System.out.println("\nEnter port you want to communicate with: ");
        return Integer.parseInt(reader.readLine().trim());
    }

    private static byte[] enterPartnerIPAddress(BufferedReader reader) throws IOException {
        System.out.println("\nEnter the IPv4 address you want to communicate with: ");
        String[] ip = reader.readLine().split("\\.");
        byte[] senderIP = new byte[4];
        for (int i = 0; i < 4; i++)
            senderIP[i] = (byte)Integer.parseInt(ip[i].trim());

        return senderIP;
    }

    private static int enterYourPort(BufferedReader reader) throws IOException {
        System.out.println("\nEnter your port: ");
        return Integer.parseInt(reader.readLine().trim());
    }
}
