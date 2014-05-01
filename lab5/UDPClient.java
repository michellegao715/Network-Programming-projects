import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
class UDPClient {
	public static void main(String args[]) throws Exception
	{
		BufferedReader inFromUser =
				new BufferedReader(new InputStreamReader
						(System.in));
		DatagramSocket clientSocket = new DatagramSocket();
		//Invoke a DNS lookup that translate hostname to an IP address.
		InetAddress IPAddress =
				InetAddress.getByName("hostname");
		//eg. hostname = www.stanford.com
	
		byte[] sendData = new byte[1024];
		byte[] receiveData = new byte[1024];
		String sentence = inFromUser.readLine();
		sendData = sentence.getBytes();
		DatagramPacket sendPacket =
				new DatagramPacket(sendData, sendData.length,
						IPAddress, 9876);
		clientSocket.send(sendPacket);
		DatagramPacket receivePacket =
				new DatagramPacket(receiveData,receiveData.length);
		clientSocket.receive(receivePacket);
		String modifiedSentence =
				new String(receivePacket.getData());
		System.out.println("FROM SERVER:" +
				modifiedSentence);
		clientSocket.close();
	}
}
