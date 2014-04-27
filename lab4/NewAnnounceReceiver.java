import java.io.*;
import java.net.*;
import java.util.*;

public class NewAnnounceReceiver implements Runnable {
	private static HashSet<InetAddress> IPs = new HashSet<InetAddress>();
	
	public HashSet<InetAddress> getAllPeerIps() {
		return IPs;
	}
	
	@Override
	public void run() {
		System.out.println("NewAnnounceReceiver");
		try {
			HashSet<String> localIps = new HashSet<String>();
			Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
			while(interfaces.hasMoreElements()) {
				Enumeration<InetAddress> addrs = interfaces.nextElement().getInetAddresses();
				while(addrs.hasMoreElements()) {
					InetAddress addr = addrs.nextElement();
					if(!addr.isLoopbackAddress() && addr instanceof Inet4Address) {
						System.out.println("Known local ip: " + addr.getHostAddress());
						localIps.add(addr.getHostAddress());
					}
				}
			}
			DatagramSocket serverSocket = new DatagramSocket(8888);
			byte[] receiveData = new byte[1024];
			byte[] sendData = new byte[1024];
			while(true)
			{
				DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
				serverSocket.receive(receivePacket);
				String sentence = new String( receivePacket.getData());
				InetAddress IPAddress = receivePacket.getAddress();
				// System.out.println(IPAddress.getHostAddress());
				if(localIps.contains(IPAddress.getHostAddress()))
					continue;
				System.out.println("RECEIVED from " + IPAddress.getHostAddress() + ": " + sentence);
				IPs.add(IPAddress);
				int port = receivePacket.getPort();
				String capitalizedSentence = sentence.toUpperCase();
				sendData = capitalizedSentence.getBytes();
				DatagramPacket sendPacket =
					new DatagramPacket(sendData, sendData.length, IPAddress, port);
				serverSocket.send(sendPacket);
			}
		} catch (Exception ex) {
		}
	}
}
