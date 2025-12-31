package de.luh.vss.chat.server;

import static de.luh.vss.chat.common.UdpUtils.receiveUdpMessage;
import static de.luh.vss.chat.common.UdpUtils.sendUdpMessage;

import de.luh.vss.chat.common.*;
import de.luh.vss.chat.common.User.UserIdentifier;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {

    private static final int PORT = 5000;

    /** All registered clients */
    private static final Map<UserIdentifier, ClientInfo> clients =
            new ConcurrentHashMap<>();

    public static void main(String[] args) throws Exception {
        new Server().start();
    }

    public void start() throws Exception {

        DatagramSocket udpSocket = new DatagramSocket(PORT);
        ServerSocket tcpServer = new ServerSocket(PORT);

        ExecutorService tcpPool = Executors.newCachedThreadPool();

        System.out.println("Server started on port " + PORT);

        // ---------------- TCP REGISTRATION THREAD ----------------
        Thread tcpThread = new Thread(() -> {
            while (true) {
                try {
                    Socket socket = tcpServer.accept();
                    tcpPool.execute(new RegistrationHandler(socket));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        tcpThread.start();

        // ---------------- UDP MESSAGE ROUTER ----------------
        byte[] buffer = new byte[2048];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

        while (true) {
            Message msg = receiveUdpMessage(udpSocket, packet);

            if (msg instanceof Message.ChatMessagePayload chat) {
                routeMessage(udpSocket, chat);
            }
        }
    }

    // ---------------- ROUTING LOGIC ----------------

    private void routeMessage(DatagramSocket socket,
                              Message.ChatMessagePayload msg) {

        UserIdentifier target = msg.getRecipient();

        // ---------- BROADCAST ----------
        if (target.equals(UserIdentifier.BROADCAST)) {
            clients.values().forEach(client -> {
                try {
                    sendUdpMessage(socket, msg, client.address, client.udpPort);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            return;
        }

        // ---------- UNICAST ----------
        ClientInfo client = clients.get(target);
        if (client != null) {
            try {
                sendUdpMessage(socket, msg, client.address, client.udpPort);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("Unknown recipient: " + target);
        }
    }

    // ---------------- TCP REGISTRATION HANDLER ----------------

    private static class RegistrationHandler implements Runnable {

        private final Socket socket;

        RegistrationHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try (DataInputStream in = new DataInputStream(socket.getInputStream())) {

                Message msg = Message.parse(in);

                if (msg instanceof Message.ServiceRegistrationRequest reg) {

                    ClientInfo info = new ClientInfo(
                            reg.getUserIdentifier(),
                            reg.getAddress(),
                            reg.getPort()
                    );

                    clients.put(reg.getUserIdentifier(), info);

                    System.out.println("Registered client: " + reg.getUserIdentifier()
                            + " @ " + reg.getAddress() + ":" + reg.getPort());
                }

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    socket.close();
                } catch (IOException ignored) {}
            }
        }
    }

    // ---------------- CLIENT INFO ----------------

    private static class ClientInfo {
        final UserIdentifier id;
        final InetAddress address;
        final int udpPort;

        ClientInfo(UserIdentifier id, InetAddress address, int udpPort) {
            this.id = id;
            this.address = address;
            this.udpPort = udpPort;
        }
    }
}
