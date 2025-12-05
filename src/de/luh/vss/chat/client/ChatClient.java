package de.luh.vss.chat.client;

import de.luh.vss.chat.common.*;


import java.io.*;
import java.net.*;
import java.util.concurrent.*;
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
        var socket = new Socket("130.75.202.197", 4447);
        var writer = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
        var reader = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
        User.UserIdentifier MyUserIdentifier = new User.UserIdentifier(7567);
        
        
        
        //The lease
        Message.ServiceRegistrationRequest req =new Message.ServiceRegistrationRequest(MyUserIdentifier,socket.getInetAddress(),socket.getPort());
        req.toStream(writer);
        writer.flush();
        //trigger
        new Message.ChatMessagePayload(MyUserIdentifier, "TEST 4_2 LISTEN TO INCOMING MESSAGES AND ECHO SPECIAL MESSAGE").toStream(writer);
        writer.flush();
        
        
        
        Message recieved_msg = null ;
        while(true) {
			try {
				recieved_msg = Message.parse(reader);
			} catch (IOException | ReflectiveOperationException e) {
				e.printStackTrace();
			}
	    	if(  recieved_msg instanceof Message.ChatMessagePayload PayloadedMessage) {
	    		String text =PayloadedMessage.getMessage();
	    		if(text.contains("SPECIAL MESSAGE TEST 4_2")) {
	    			recieved_msg.toStream(writer);
	    			break;
	    		}
	    		
	    	}
	    }
	    writer.close();
	    reader.close();
	    socket.close();

    }
}