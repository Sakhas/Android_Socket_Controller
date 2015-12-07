package pt.novaims.slickcontroller.application;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import android.os.AsyncTask;

public class UDPClientTask extends AsyncTask<String, Void, String>{

	byte[] send_data = new byte[1024];
	byte[] receiveData = new byte[1024];
	private final int PORT = 8888;
    private final String udpMessage = "PONG";
	private String udpReply;
	
	@Override
	protected String doInBackground(String... params) {
		boolean udpRequestActive = true;
	   	 DatagramSocket client_socket;
		try {
			client_socket = new DatagramSocket(2362);
			InetAddress IPAddress =  InetAddress.getByName("255.255.255.255"); 
		     send_data = udpMessage.getBytes();
		     
		     while (udpRequestActive) {
		
		        DatagramPacket send_packet = new DatagramPacket(send_data, udpMessage.length(), IPAddress, PORT);
		        client_socket.send(send_packet);                      
		
		   		DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
		   		client_socket.receive(receivePacket);
		   		System.out.println("UDP processing");
		   		
		   		if(receivePacket.getData() != null) {
		   			udpReply = new String(receivePacket.getData());
		   	   		System.out.println("FROM SERVER:" + udpReply);
		   	   		udpRequestActive = false;
		   		}
		     }
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	     
		return udpReply;
	}

}
