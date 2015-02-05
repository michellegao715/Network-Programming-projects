import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class NewAnnounceService implements Runnable {
	public static final String BroadcastIP = "255.255.255.255"; 
	private static final int port = 8888;
	private static int k =0;
	private static InetAddress ip = null;

	@Override
	public void run() {
		DatagramSocket clientSocket;
		try {
			clientSocket = new DatagramSocket();
			clientSocket.setBroadcast(true);
			byte[] sendData = buildSendData(clientSocket, BroadcastIP);
			serviceAnnounce(clientSocket, sendData, BroadcastIP, port);
		} catch (SocketException e) {
			e.printStackTrace();
		}
	}

	private void serviceAnnounce(DatagramSocket _clientSocket,
	byte[] _sendData, String BoradcastIP, int port2) {
		int timeLimit = 3600;
		try {
			ip = InetAddress.getByName(BroadcastIP);
		} catch (UnknownHostException e1) {
			e1.printStackTrace();
		}
		while (true) {
			try {
				DatagramPacket sendPacket = new DatagramPacket(_sendData,
					_sendData.length, ip, port);
				_clientSocket.send(sendPacket);
				//Thread.sleep(timeLimit);
				if (Math.pow(3, k) < timeLimit) {
					Thread.sleep((long) Math.pow(3, k));
					k += 1;
				} else {
					Thread.sleep(timeLimit);
				}
				System.out.println(_clientSocket.getClass().getName()
					+ ">>> Request packet sent to: 255.255.255.255 (DEFAULT)");
			} catch (Exception e) {
				System.err.println(e.toString());
			}
		}
	}

	private byte[] buildSendData(DatagramSocket _clientSocket,
	String MDNSServerIP) {
		byte[] _sendData = null;
		try {
			ip = InetAddress.getByName(MDNSServerIP);
			//Open a random port to send the package
			
			_clientSocket.setBroadcast(true);
			//byte[] sendData = "cs621-cache".getBytes();

			int minimum = 1000;
			int maximum = 8999;
			int randomNum = minimum + (int)(Math.random()*maximum);
			String MessageID = String.valueOf(randomNum);

			String Type = CompleteDNSClient.constructQueryClass("PTR");
			String Class = "0001";
			String Name = CompleteDNSClient.ToHexString("cs621_cache");
			String mdnsQuery = MessageID + "01000001000000000000" + Name + Type + Class;
			// Format: ID+Flag(0100 for query,8400 for response)+Question(0001)+AnswerRRs(0000)+AuthorityRRs(0000)+AdditionalRRs(0000)+QName(cs621_cache)+QType("PTR"000c)+Class("IN":0001)
			
			_sendData = CompleteDNSClient.hexStringToByteArray(mdnsQuery);
		} catch (Exception e) {
			System.err.println(e.toString());
		}
		return _sendData;
	}
		
	/*	byte[] _sendData = null;
	// return _sendData;
	return "Hello World".getBytes();*/
}
