package de.luh.vss.chat.client;
import de.luh.vss.chat.common.*;
import java.io.*;
import java.net.*;

import de.luh.vss.chat.common.Message;
public class ChatClient {

	public static void main(String... args) {
		try {
			new ChatClient().start();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void start() throws IOException {
		System.out.println("Congratulation for successfully setting up your environment for Distributed Systems Exercises!\n");
		//Initialization of the socket 
		var socket=new Socket("130.75.202.197",4446);
		var writer = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
		
		
		
		//send trigger message
		var myIdentifier=new User.UserIdentifier(7567);
		
		
		var req=new Message.ServiceRegistrationRequest(myIdentifier,InetAddress.getByName("130.75.202.197"),4446);
		var msg = new Message.ChatMessagePayload( myIdentifier, "TEST 3_1 SEND MESSAGE WHILE HAVING AN ACTIVE LEASE");
		req.toStream(writer);
		msg.toStream(writer);
		writer.flush();
		
		
		//closing

		writer.close();
		socket.close();
	}

}
