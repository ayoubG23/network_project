package de.luh.vss.chat.server;

import static de.luh.vss.chat.common.UdpUtils.*;

import de.luh.vss.chat.common.*;
import de.luh.vss.chat.common.User.UserIdentifier;

import java.io.DataInputStream;
import java.net.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Server {

    private static final int PORT = 5000;
    private static final long TIMEOUT = 120_000;

    private static final Map<UserIdentifier, ClientInfo> clients = new ConcurrentHashMap<>();
    private DatagramSocket udpSocket;

    public static void main(String[] args) throws Exception {
        new Server().start();
    }

    public void start() throws Exception {

        udpSocket = new DatagramSocket(PORT);
        ServerSocket tcpServer = new ServerSocket(PORT);

        System.out.println("Server started on port " + PORT);

        // Cleanup thread for clients that timeout
        new Thread(() -> {
            while (true) {
                long now = System.currentTimeMillis();
                for (ClientInfo c : clients.values()) {
                    if (now - c.lastSeen > TIMEOUT) {
                        clients.remove(c.id);
                        broadcastSystem("User " + c.id.id() + " left");
                    }
                }
                try { Thread.sleep(30_000); } catch (Exception ignored) {}
            }
        }).start();

        // TCP registration thread
        new Thread(() -> {
            while (true) {
                try {
                    Socket s = tcpServer.accept();
                    DataInputStream in = new DataInputStream(s.getInputStream());
                    Message msg = Message.parse(in);

                    if (msg instanceof Message.ServiceRegistrationRequest reg) {
                        ClientInfo existing = clients.get(reg.getUserIdentifier());

                        if (existing == null) {
                            // Use the UDP port from registration but IP from TCP connection
                            ClientInfo info = new ClientInfo(
                                    reg.getUserIdentifier(),
                                    s.getInetAddress(),
                                    reg.getPort()
                            );
                            clients.put(info.id, info);
                            broadcastSystem("User " + info.id.id() + " joined");
                        } else {
                            existing.lastSeen = System.currentTimeMillis();
                        }
                    }
                    s.close();
                } catch (Exception ignored) {}
            }
        }).start();

        // UDP message router
        byte[] buf = new byte[2048];
        DatagramPacket packet = new DatagramPacket(buf, buf.length);

        while (true) {
            Message msg = receiveUdpMessage(udpSocket, packet);
            ClientInfo sender = findSender(packet);

            if (sender == null) continue;

            if (msg instanceof Message.ChatMessagePayload chat) {

                if (chat.getMessage().equals("__ONLINE__")) {
                    sendOnlineList(sender);
                    continue;
                }

                route(chat, sender);
            }
        }
    }

    private ClientInfo findSender(DatagramPacket p) {
        for (ClientInfo c : clients.values()) {
            if (c.address.equals(p.getAddress()) && c.udpPort == p.getPort()) {
                c.lastSeen = System.currentTimeMillis();
                return c;
            }
        }
        return null;
    }

    private void route(Message.ChatMessagePayload msg, ClientInfo sender) throws Exception {
        UserIdentifier target = msg.getRecipient();

        if (target.equals(UserIdentifier.BROADCAST)) {
            for (ClientInfo c : clients.values()) {
                if (!c.id.equals(sender.id)) {
                    sendUdpMessage(udpSocket, new Message.ChatMessagePayload(sender.id, msg.getMessage()), c.address, c.udpPort);
                }
            }
            return;
        }

        ClientInfo dst = clients.get(target);
        if (dst != null) {
            sendUdpMessage(udpSocket, new Message.ChatMessagePayload(sender.id, msg.getMessage()), dst.address, dst.udpPort);
        } else {
            // Notify sender if target doesn't exist
            sendUdpMessage(
                    udpSocket,
                    new Message.ChatMessagePayload(new UserIdentifier(0), "[SYSTEM] Unknown recipient: " + target.id()),
                    sender.address,
                    sender.udpPort
            );
        }
    }

    private void sendOnlineList(ClientInfo dst) throws Exception {
        StringBuilder sb = new StringBuilder("Online users: ");
        boolean first = true;

        for (UserIdentifier id : clients.keySet()) {
            if (!first) sb.append(", ");
            sb.append(id.id());
            first = false;
        }

        sendUdpMessage(
                udpSocket,
                new Message.ChatMessagePayload(new UserIdentifier(0), sb.toString()),
                dst.address,
                dst.udpPort
        );
    }

    private void broadcastSystem(String text) {
        try {
            for (ClientInfo c : clients.values()) {
                sendUdpMessage(
                        udpSocket,
                        new Message.ChatMessagePayload(new UserIdentifier(0), "[SYSTEM] " + text),
                        c.address,
                        c.udpPort
                );
            }
        } catch (Exception ignored) {}
    }

    private static class ClientInfo {
        final UserIdentifier id;
        final InetAddress address;
        final int udpPort;
        volatile long lastSeen;

        ClientInfo(UserIdentifier id, InetAddress address, int udpPort) {
            this.id = id;
            this.address = address;
            this.udpPort = udpPort;
            this.lastSeen = System.currentTimeMillis();
        }
    }
}
