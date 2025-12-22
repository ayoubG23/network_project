package de.luh.vss.chat.client;

import de.luh.vss.chat.common.*;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

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
        var socket = new Socket("130.75.202.197", 4449);
        var udpSocket = new DatagramSocket();
        udpSocket.setSoTimeout(1000);
        boolean Online=true;
        var writer = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
        var reader = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
        
        
        
        User.UserIdentifier MyUserIdentifier = new User.UserIdentifier(7567);
        InetAddress serverIp = InetAddress.getByName("130.75.202.197");
        int serverPort = 2002;

        List<MessageContent> pendingList = new ArrayList<>();
        byte[] buffer = new byte[2000];
        DatagramPacket receivedPacket = new DatagramPacket(buffer, buffer.length);



        
        // ---------------- Lease Registration with TCP----------------
        Message.ServiceRegistrationRequest req =
                new Message.ServiceRegistrationRequest(MyUserIdentifier, socket.getInetAddress(), socket.getPort());
        req.toStream(writer);
        writer.flush();
       
       
        // ---------------- Trigger with UDP ----------------
        sendUdpMessage(
                udpSocket,
                new Message.ChatMessagePayload(MyUserIdentifier, "TEST 6_2 TIMESTAMP REORDERING WHILE OFFLINE"),
                serverIp,
                serverPort
        );

        
	     // ---------------- Main Loop ----------------
	        while (true) {
	
	            Message incoming = receiveUdpMessage(udpSocket, receivedPacket);
	
	            
	            if (incoming == null && Online) {
	            	
	            	pendingList.sort(Comparator.comparingInt((m1)->m1.Min * 60 + m1.Sec));
	            	
	            	for(MessageContent m : pendingList) {
	            		
	            		System.out.println(m);
	            		sendUdpMessage(
	                            udpSocket,
	                            new Message.ChatMessagePayload(MyUserIdentifier,m.All),
	                            serverIp,
	                            serverPort
	                    );
	            		
	            		
	            	}
	            	
	            	continue;
	            }
	
	            // ----- Process received message -----
	            if (incoming instanceof Message.ChatMessagePayload payloadedMessage) {
	                String text = payloadedMessage.getMessage();
	                System.out.println("recieved : "+text);
	            	if (text.contains("SUCCESSFULLY PASSED")|| text.contains("FAILED") ) {
	                    
	                    break; // exit loop
	                }else if(text.contains("Online")){
	                	Online=true;
	                }else if(text.contains("Offline")){
	                	Online=false;
	                }else { 
	                    pendingList.add(new MessageContent(text));
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

class MessageContent {
	String All;
	String ID;
	String Content;
	String Timestamp;
	int Min;
	int Sec;
	public MessageContent(String msg) {
		String[] parts =  msg.split("\\|");
		this.ID=parts[0];
		this.Content=parts[1];
		this.Timestamp=parts[2];
		String[] parts2 =  msg.split(":");
		this.Min=Integer.parseInt(parts2[1]);
		this.Sec=Integer.parseInt(parts2[2]);
		this.All=String.join("|",ID,Content,Timestamp);
	}
	
	public String toString() {
		return All;
		
	}
}
