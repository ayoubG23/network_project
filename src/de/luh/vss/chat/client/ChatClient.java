package de.luh.vss.chat.client;

import de.luh.vss.chat.common.*;

import java.io.*;
import java.net.*;
import java.util.Arrays;
import java.util.LinkedHashMap;

public class ChatClient {

    public static void main(String... args) throws ReflectiveOperationException {
        try {
            new ChatClient().start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void start() throws IOException, ReflectiveOperationException {

        // ---------------- Initialization ----------------
        var socket = new Socket("130.75.202.197", 4448);
        var udpSocket = new DatagramSocket();
        udpSocket.setSoTimeout(1000);

        var writer = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
        var reader = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
        
        
        
        User.UserIdentifier MyUserIdentifier = new User.UserIdentifier(7567);
        InetAddress serverIp = InetAddress.getByName("130.75.202.197");
        int serverPort = 7007;

        LinkedHashMap<String, byte[]> pendingList = new LinkedHashMap<>();
        byte[] buffer = new byte[2000];
        DatagramPacket receivedPacket = new DatagramPacket(buffer, buffer.length);



        boolean isOnline = true;
        // ---------------- Lease Registration with TCP----------------
        Message.ServiceRegistrationRequest req =
                new Message.ServiceRegistrationRequest(MyUserIdentifier, socket.getInetAddress(), socket.getPort());
        req.toStream(writer);
        writer.flush();
       
       
        // ---------------- Trigger with UDP ----------------
        sendUdpMessage(
                udpSocket,
                new Message.ChatMessagePayload(MyUserIdentifier, "TEST 5_2 BUFFERED MESSAGING WHILE OFFLINE"),
                serverIp,
                serverPort
        );

        
	     // ---------------- Main Loop ----------------
	        while (true) {
	
	            Message incoming = receiveUdpMessage(udpSocket, receivedPacket);
	
	            // ----- Retry pending messages if online -----
	            if (incoming == null && isOnline) {
	                for (byte[] storedBytes : pendingList.values()) {
	                    DatagramPacket resend = new DatagramPacket(storedBytes, storedBytes.length, serverIp, serverPort);
	                    udpSocket.send(resend);
	                }
	                continue;
	            }
	
	            // ----- Process received message -----
	            if (incoming instanceof Message.ChatMessagePayload payloadedMessage) {
	                String text = payloadedMessage.getMessage();
	                System.out.println(text);
	
	                // ----- Handle acknowledgment -----
	                if (text.startsWith("Acknowledged message: ")) {
	                    String original = text.replace("Acknowledged message: ", "");
	                    pendingList.remove(original);
	                }
	                // ----- Test successfully passed -----
	                else if (text.contains("SUCCESSFULLY PASSED")) {
	                    
	                    break; // exit loop
	                }
	                // ----- User goes offline -----
	                else if (text.contains("User is now Offline and cannot receive messages")) {
	                    isOnline = false;
	                }
	                // ----- User comes online -----
	                else if (text.contains("User is now Online and can receive messages")) {
	                    isOnline = true;
	                    // send all pending messages
	                    for (byte[] storedBytes : pendingList.values()) {
	                        DatagramPacket resend = new DatagramPacket(storedBytes, storedBytes.length, serverIp, serverPort);
	                        udpSocket.send(resend);
	                    }
	                }
	                // ----- Normal message: buffer it for acknowledgment -----
	                else {
	                    // save raw bytes exactly as received
	                    byte[] receivedBytes = Arrays.copyOf(buffer, receivedPacket.getLength());
	                    pendingList.put(text, receivedBytes);
	                }
	            }
	
	            
	            
	        }


        // ---------------- Cleanup ----------------
        writer.close();
        reader.close();
        socket.close();
        udpSocket.close();
    }

    // ---------------- GENERAL UDP HELPERS ----------------

    public void sendUdpMessage(DatagramSocket socket,
                               Message message,
                               InetAddress address,
                               int port) throws IOException {

        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        DataOutputStream dout = new DataOutputStream(bout);

        message.toStream(dout);
        dout.flush();

        byte[] bytes = bout.toByteArray();
        DatagramPacket packet = new DatagramPacket(bytes, bytes.length, address, port);
        socket.send(packet);
    }

    public Message receiveUdpMessage(DatagramSocket socket,
                                     DatagramPacket reusablePacket) throws IOException {

        try {
            socket.receive(reusablePacket);
        } catch (SocketTimeoutException e) {
        	return null; // no message received
        }

        ByteArrayInputStream bin = new ByteArrayInputStream(
                reusablePacket.getData(),
                0,
                reusablePacket.getLength()
        );

        DataInputStream din = new DataInputStream(bin);

        try {
            return Message.parse(din);
        } catch (ReflectiveOperationException e) {
            throw new IOException("Failed to parse incoming UDP message", e);
        }
    }
}
