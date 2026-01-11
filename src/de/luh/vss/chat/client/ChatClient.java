package de.luh.vss.chat.client;

import static de.luh.vss.chat.common.UdpUtils.*;

import de.luh.vss.chat.common.*;
import de.luh.vss.chat.common.User.UserIdentifier;

import java.io.DataOutputStream;
import java.net.*;
import java.util.Scanner;

public class ChatClient {

    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 5000;

    private final UserIdentifier myId;

    public ChatClient(UserIdentifier myId) {
        this.myId = myId;
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.out.println("Usage: ChatClient <userId>");
            return;
        }
        new ChatClient(new UserIdentifier(Integer.parseInt(args[0]))).start();
    }

    public void start() throws Exception {

        DatagramSocket udpSocket = new DatagramSocket();
        System.out.println("Client started as user " + myId.id());
        System.out.println("UDP port: " + udpSocket.getLocalPort());

        // Initial registration
        register(udpSocket);

        // Heartbeat thread for re-registration
        Thread heartbeat = new Thread(() -> {
            try {
                while (true) {
                    Thread.sleep(60_000);
                    register(udpSocket);
                }
            } catch (Exception ignored) {}
        });
        heartbeat.setDaemon(true);
        heartbeat.start();

        // Receiver thread
        Thread receiver = new Thread(() -> {
            try {
                byte[] buffer = new byte[2048];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

                while (true) {
                    Message msg = receiveUdpMessage(udpSocket, packet);
                    if (msg instanceof Message.ChatMessagePayload chat) {
                        System.out.println("\n[from " + chat.getRecipient().id() + "] " + chat.getMessage());
                        System.out.print("> ");
                    }
                }
            } catch (Exception ignored) {}
        });
        receiver.setDaemon(true);
        receiver.start();

        // User input
        Scanner scanner = new Scanner(System.in);
        InetAddress serverAddr = InetAddress.getByName(SERVER_HOST);

        while (true) {
            System.out.print("> ");
            String line = scanner.nextLine();

            if (line.equalsIgnoreCase("exit")) break;

            if (line.equalsIgnoreCase("online")) {
                // Request online list
                sendUdpMessage(
                        udpSocket,
                        new Message.ChatMessagePayload(myId, "__ONLINE__"),
                        serverAddr,
                        SERVER_PORT
                );
                continue;
            }

            int space = line.indexOf(' ');
            if (space < 0) {
                System.out.println("Invalid format");
                continue;
            }

            int target = Integer.parseInt(line.substring(0, space));
            String msg = line.substring(space + 1);

            sendUdpMessage(
                    udpSocket,
                    new Message.ChatMessagePayload(new UserIdentifier(target), msg),
                    serverAddr,
                    SERVER_PORT
            );
        }

        udpSocket.close();
        System.out.println("Client terminated.");
    }

    private void register(DatagramSocket udpSocket) throws Exception {
        Socket tcp = new Socket(SERVER_HOST, SERVER_PORT);
        DataOutputStream out = new DataOutputStream(tcp.getOutputStream());

        new Message.ServiceRegistrationRequest(
                myId,
                InetAddress.getLocalHost(),
                udpSocket.getLocalPort()
        ).toStream(out);

        out.flush();
        tcp.close();
    }
}
