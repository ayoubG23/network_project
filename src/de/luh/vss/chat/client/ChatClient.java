package de.luh.vss.chat.client;

import de.luh.vss.chat.common.*;


import java.io.*;
import java.net.*;
import java.util.Arrays;
import java.util.LinkedHashMap;
public class ChatClient {
    public static void main(String... args) {
        try {
            new ChatClient().start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    
    public void start() throws IOException {

        //Initialization
        var socket = new Socket("130.75.202.197", 4448);
        var udpSocket = new DatagramSocket();
        udpSocket.setSoTimeout(500);
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        boolean testLanched=false;
        
        var writer = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
        var reader = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
        User.UserIdentifier MyUserIdentifier = new User.UserIdentifier(7567);
        
        
        
        //The lease
        Message.ServiceRegistrationRequest req =new Message.ServiceRegistrationRequest(MyUserIdentifier,socket.getInetAddress(),socket.getPort());
        req.toStream(writer);
        writer.flush();
        //trigger
        new Message.ChatMessagePayload(MyUserIdentifier, "TEST 5_1 LOST MESSAGE HANDLING").toStream(new DataOutputStream(bout));
		DatagramPacket triggerMsg = new DatagramPacket(bout.toByteArray(),bout.toByteArray().length , InetAddress.getByName("130.75.202.197"),5005);
		udpSocket.send(triggerMsg);
		bout.reset();
        
        byte[] buffer = new byte[2000];
        DatagramPacket recievedPacket = new DatagramPacket(buffer,buffer.length);
        LinkedHashMap<String, byte[]> pendingList = new LinkedHashMap<>();

        
        
        
        
        while (true) {
            try {
                udpSocket.receive(recievedPacket);
            } catch (SocketTimeoutException e) {
            	// if the socket does not receive any message until the timeout send the rest message
	            for(String key  : pendingList.keySet()) {
	            	
	            	byte[] echoByte = pendingList.get(key);
	            	DatagramPacket echoPacket = new DatagramPacket(echoByte, echoByte.length,InetAddress.getByName("130.75.202.197"), 5005);
	            	udpSocket.send(echoPacket);
	            	
	            	
	            }
	            continue;    
            } catch (IOException e) {
                e.printStackTrace();
                break; // exit loop on real I/O error
            }

            // process the packet 
            ByteArrayInputStream bin = new ByteArrayInputStream(recievedPacket.getData(), 0, recievedPacket.getLength());
            Message incoming = null;
            try {
                incoming = Message.parse(new DataInputStream(bin));
            } catch (IOException | ReflectiveOperationException e) {
                continue;
            }
            
            if (incoming instanceof Message.ChatMessagePayload payloadedMessage) {
                String text = payloadedMessage.getMessage();
                System.out.println(text);
                // remove Acknowledged message from our list
                if (text.contains("Acknowledged message:")) {
                	pendingList.remove(text);
                    
                }else if(text.contains("SUCCESSFULLY PASSED")) {// test succeeded
                	
                	break;
                }else {// add  message to our list and send it
                	
                	// Save the **received bytes exactly as they came**
                    byte[] receivedBytes = Arrays.copyOf( buffer, recievedPacket.getLength() );
                    
                    

                    // Store in the map
                    pendingList.put(text, receivedBytes);
               }
               
            }
            
            if (pendingList.isEmpty() && testLanched) {
                break;
            }

            
        }

	    writer.close();
	    reader.close();
	    socket.close();
	    udpSocket.close();

    }
}