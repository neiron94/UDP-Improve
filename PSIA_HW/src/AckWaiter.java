import java.io.IOException;

public class AckWaiter implements Runnable {
    Packet packet;
    public AckWaiter(Packet packet) {
        this.packet = packet;
    }
    @Override
    public void run() {
        try {
            Thread.sleep(Sender.SOCKET_TIMEOUT);
            if (Sender.ackIndex < packet.decodePacketIndex()) {
                System.out.println("Repeat sending packet " + packet.decodePacketIndex() + "\n");
                packet.sendWithRepeat(Sender.socket, Sender.anotherAddress, Sender.anotherPort);
            }

        } catch (InterruptedException | IOException e) {
            throw new RuntimeException(e);
        }
    }
}