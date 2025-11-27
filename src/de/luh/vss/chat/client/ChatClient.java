package de.luh.vss.chat.client;

import de.luh.vss.chat.common.*;
import java.io.*;
import java.net.*;

public class ChatClient {

    public static void main(String... args) {
        try {
            new ChatClient().start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void start() throws IOException {

        //register for the lease
        var socket = new Socket("130.75.202.197", 4446);
        var writer = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));

        var request = new Message.ServiceRegistrationRequest(new User.UserIdentifier(7567),InetAddress.getByName("10.172.63.224"), 4446);
        request.toStream(writer);
        writer.flush();
        
        //the udp-socket
        try {
            DatagramSocket udpSocket = new DatagramSocket();
            udpSocket.setSoTimeout(5000);

            InetAddress serverIp = InetAddress.getByName("130.75.202.197");
            User.UserIdentifier me = new User.UserIdentifier(7567);

            // Send trigger
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            new Message.ChatMessagePayload(me, "TEST 3_2 ECHO MESSAGE FROM USER")
                    .toStream(new DataOutputStream(bout));
            byte[] data = bout.toByteArray();

            udpSocket.send(new DatagramPacket(data, data.length, serverIp, 5252));

            // Receive echo
            byte[] buf = new byte[4096];
            DatagramPacket p = new DatagramPacket(buf, buf.length);
            udpSocket.receive(p);

            ByteArrayInputStream bin = new ByteArrayInputStream(p.getData(), 0, p.getLength());
            Message incoming = Message.parse(new DataInputStream(bin));

            // Echo back again
            if (incoming instanceof Message.ChatMessagePayload) {
                String text = ((Message.ChatMessagePayload) incoming).getMessage();

                bout = new ByteArrayOutputStream();
                new Message.ChatMessagePayload(me, text)
                        .toStream(new DataOutputStream(bout));

                byte[] echoData = bout.toByteArray();
                udpSocket.send(new DatagramPacket(echoData, echoData.length, serverIp, 5252));

                System.out.println(text);
            }

            udpSocket.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
        writer.close();
        socket.close();
        

    }
}