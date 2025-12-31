package de.luh.vss.chat.client;

import static de.luh.vss.chat.common.UdpUtils.receiveUdpMessage;
import static de.luh.vss.chat.common.UdpUtils.sendUdpMessage;

import de.luh.vss.chat.common.*;
import de.luh.vss.chat.common.User.UserIdentifier;

import java.io.*;
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

        int id = Integer.parseInt(args[0]);
        new ChatClient(new UserIdentifier(id)).start();
    }

    public void start() throws Exception {

        // ---------------- SOCKETS ----------------

        DatagramSocket udpSocket = new DatagramSocket();
        Socket tcpSocket = new Socket(SERVER_HOST, SERVER_PORT);

        System.out.println("Client started as user " + myId.id());
        System.out.println("UDP port: " + udpSocket.getLocalPort());

        // ---------------- TCP REGISTRATION ----------------

        DataOutputStream tcpOut =
                new DataOutputStream(new BufferedOutputStream(tcpSocket.getOutputStream()));

        Message.ServiceRegistrationRequest reg =
                new Message.ServiceRegistrationRequest(
                        myId,
                        InetAddress.getLocalHost(),
                        udpSocket.getLocalPort()
                );

        reg.toStream(tcpOut);
        tcpOut.flush();

        tcpSocket.close(); // registration done

        // ---------------- UDP RECEIVER THREAD ----------------

        Thread receiver = new Thread(() -> {
            try {
                byte[] buffer = new byte[2048];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

                while (true) {
                    Message msg = receiveUdpMessage(udpSocket, packet);

                    if (msg instanceof Message.ChatMessagePayload chat) {
                        System.out.println(
                                "\n[from " + chat.getRecipient().id() + "] " +
                                chat.getMessage()
                        );
                        System.out.print("> ");
                    }
                }
            } catch (IOException e) {
                System.out.println("Receiver stopped.");
            }
        });

        receiver.setDaemon(true);
        receiver.start();

        // ---------------- USER INPUT LOOP ----------------

        try (Scanner scanner = new Scanner(System.in)) {
			InetAddress serverAddress = InetAddress.getByName(SERVER_HOST);

			System.out.println("Enter messages in format:");
			System.out.println("<targetId> <message>");
			System.out.println("Use 0 as targetId for broadcast.");
			System.out.println();

			while (true) {
			    System.out.print("> ");
			    String line = scanner.nextLine();

			    if (line.equalsIgnoreCase("exit")) {
			        break;
			    }

			    int space = line.indexOf(' ');
			    if (space == -1) {
			        System.out.println("Invalid format.");
			        continue;
			    }

			    int targetId = Integer.parseInt(line.substring(0, space));
			    String message = line.substring(space + 1);

			    Message.ChatMessagePayload chat =
			            new Message.ChatMessagePayload(
			                    new UserIdentifier(targetId),
			                    message
			            );

			    sendUdpMessage(
			            udpSocket,
			            chat,
			            serverAddress,
			            SERVER_PORT
			    );
			}
		}catch(Exception e){
			System.out.println("Invalid format.");
		}
        udpSocket.close();
        System.out.println("Client terminated.");
    }
}
