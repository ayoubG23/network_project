package de.luh.vss.chat.common;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;

public class UdpUtils{
	public static void sendUdpMessage(DatagramSocket socket,
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
	
	
	public static Message receiveUdpMessage(DatagramSocket socket,DatagramPacket reusablePacket) throws IOException {

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