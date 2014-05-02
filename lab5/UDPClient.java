import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
class UDPClient {
	public static void main(String args[]) throws Exception
	{
		DatagramSocket clientSocket = new DatagramSocket();
		InetAddress IPAddress = InetAddress.getByName("10.0.0.4");
		clientSocket.connect(IPAddress, 9876);
		byte[] sendData = new byte[1024];
		byte[] receiveData = new byte[1024];
		String sentence = "hello world";
		sendData = sentence.getBytes();
		DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length);
		clientSocket.send(sendPacket);
		
		DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
		clientSocket.receive(receivePacket);
		String modifiedSentence = new String(receivePacket.getData());
		System.out.println("FROM SERVER:" + modifiedSentence);
			  
		clientSocket.close();
	}
}
