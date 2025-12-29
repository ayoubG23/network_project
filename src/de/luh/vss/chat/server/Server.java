package de.luh.vss.chat.server;

import static de.luh.vss.chat.common.UdpUtils.receiveUdpMessage;
import static de.luh.vss.chat.common.UdpUtils.sendUdpMessage;
import de.luh.vss.chat.common.*;
import de.luh.vss.chat.common.User.*;
import de.luh.vss.chat.client.*;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class Server{
	public static void main(String[] args) {
		try {
			Server.start();
		} catch (ReflectiveOperationException e) {
			
			e.printStackTrace();
		}
	}

	private static void start() throws ReflectiveOperationException {
		int port = 5000;
		User.UserIdentifier severIdentifier = new UserIdentifier(111);
		AtomicBoolean online = new AtomicBoolean(true);
		
		try (ServerSocket server = new ServerSocket(port);Socket socket = server.accept();){
			//TCP protocol
			
			System.out.println("connected  " + socket.getPort());
			var writer = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
	        var reader = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
	        
	        
	        //UDP protocol
	        
	        
	        DatagramSocket udpSocket = new DatagramSocket(5000);
	        byte[] buffer = new byte[2000];
	        DatagramPacket receivedPacket = new DatagramPacket(buffer, buffer.length);
	        
	        
	        //Registration
	        Message incomming = null;
	        InetAddress clientAddress = null;
	        int clientUdpPort = 0;
	        
	        System.out.println("Waiting for Regestration ...");
	        Message msg = Message.parse(reader); 
	        if(msg instanceof Message.ServiceRegistrationRequest registration) {
		        clientAddress = registration.getAddress();
		        clientUdpPort = registration.getPort();
		        System.out.println(registration);
	        }
	        
	        
	        
	        Message.ChatMessagePayload test_msg = new Message.ChatMessagePayload(severIdentifier,"1|Message 1|06.12.2025 20:14:56");
	        
	        sendUdpMessage(udpSocket, test_msg, clientAddress,clientUdpPort);
	        sendUdpMessage(udpSocket, new Message.ChatMessagePayload(severIdentifier,"2|Message 2|06.12.2025 20:14:44"), clientAddress,clientUdpPort);
	        
	        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
	        scheduler.schedule(() -> {
	            System.out.println("close after 10 seconds");
	            online.set(false);
	        }, 10000, TimeUnit.MILLISECONDS);   
	        while(online.get()) {
	        	
	        	System.out.println("Waiting for UDP ...");
	        	try{
	        		incomming=receiveUdpMessage(udpSocket, receivedPacket);
	        		if(incomming instanceof Message.ChatMessagePayload payloadedMessage){
		        		String text = payloadedMessage.getMessage();
		                System.out.println("recieved : "+text);
		        	}
	        	}catch(SocketException e){
	        		break;
	        	}
	        	
	        	
	        
	        
	        }
	        sendUdpMessage(udpSocket, new Message.ChatMessagePayload(severIdentifier,"SUCCESSFULLY PASSED"), clientAddress,clientUdpPort);
	        scheduler.shutdownNow();
        	writer.close();
	        reader.close();
	        socket.close();
	        udpSocket.close();
	        
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
}