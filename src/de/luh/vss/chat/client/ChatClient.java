package de.luh.vss.chat.client;

import de.luh.vss.chat.common.*;
import static de.luh.vss.chat.common.UdpUtils.receiveUdpMessage;
import static de.luh.vss.chat.common.UdpUtils.sendUdpMessage;
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
        var socket = new Socket("localhost", 5000);
        System.out.println("connected to port" + socket.getPort() + " or  " +socket.getLocalPort());
        var udpSocket = new DatagramSocket();
        udpSocket.setSoTimeout(2000);
        boolean Online=true;
        var writer = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
        var reader = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
        
        
        
        User.UserIdentifier MyUserIdentifier = new User.UserIdentifier(7567);
        InetAddress serverIp = InetAddress.getByName("localhost");
        int serverPort = 5000;

        List<MessageContent> pendingList = new ArrayList<>();
        byte[] buffer = new byte[2000];
        DatagramPacket receivedPacket = new DatagramPacket(buffer, buffer.length);



        	
        // ---------------- Lease Registration with TCP----------------
        Message.ServiceRegistrationRequest req =
                new Message.ServiceRegistrationRequest(MyUserIdentifier, socket.getLocalAddress(), udpSocket.getLocalPort());
        req.toStream(writer);
        writer.flush();
       
       
        

        
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

    

    
}

class MessageContent implements Comparable<MessageContent> {
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

	@Override
	public int compareTo(MessageContent o) {
		
		return this.Sec - o.Sec;
	}
}
