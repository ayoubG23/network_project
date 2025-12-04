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

        //register for the lease
        var socket = new Socket("130.75.202.197", 4447);
        var writer = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
        User.UserIdentifier MyUserIdentifier = new User.UserIdentifier(7567);
        
        var request = new Message.ServiceRegistrationRequest(MyUserIdentifier,InetAddress.getByName("130.75.202.197"), 4447);
        request.toStream(writer);
        writer.flush();
        new Message.ChatMessagePayload(MyUserIdentifier, "TEST 4_1 RENEW LEASE").toStream(writer);
        // keep the lease
        var scheduler = Executors.newScheduledThreadPool(2);
	        scheduler.scheduleWithFixedDelay(()->{
	        	try {
					request.toStream(writer);
					writer.flush();
					System.out.println("renewed lease");
				} catch (IOException e) {
					e.printStackTrace();
				}
	        	
	        		}, 3,3,TimeUnit.MINUTES);
	        
	    scheduler.schedule(()->{
	    	scheduler.shutdownNow();
		    try {
				writer.close();
				socket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}},20,TimeUnit.MINUTES);
	    
	    try {
			scheduler.awaitTermination(21,TimeUnit.MINUTES);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

    }
}