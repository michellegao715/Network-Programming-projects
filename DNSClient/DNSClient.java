import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;


public class DNSClient {
	private static String DNSServerIP = "";
	private static String DomainName = "";
	private static String QueryType = "";

	public static byte[] hexStringToByteArray(String s) {
		int len = s.length();
		byte[] data = new byte[len / 2];
		for (int i = 0; i < len; i += 2) {
			data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
					+ Character.digit(s.charAt(i+1), 16));
		}
		return data;
	}
	
	// convert domain name(www.google.com) to byte[]{03 77 77 77 06 67....} and then to string(hexStringOfDomain).
	public static String changeIntToHexString(int i) {
		char low = i / 16 < 10 ? (char)(i/16 + '0') : (char)(i/16-10+'a');
		char high = i% 16 < 10 ? (char)(i%16 +'0'): (char)(i%16 - 10 + 'a');
		return ""+ low + high;
	}

	public static String changeStringToHexString(String s) {
		String completeHexString = "";
		for(int i = 0 ; i<s.length(); i++){
			char c = s.charAt(i);
			String oneHexString = changeIntToHexString(c);
			completeHexString += oneHexString;
		}
		return completeHexString;
	}
	// Call this method to change domain to hex string and then apply hexStringToByteArray to generate the byte[] to transfer to socket.
	public static String ToHexString(String domain) {
		//Step 1. If domain has "." convert anything before "." to hexString
		String hexString = "";

		if(domain.contains(".")) {
			int indexOfDot = domain.indexOf(".");
			// Change the size of anything before "." to hexstring. 
			String hexStringOfSize = changeIntToHexString(indexOfDot);
			// Change anything before "." to hexstring.
			String hexStringOfDomain = changeStringToHexString (domain.substring(0,indexOfDot));

			String restDomain = domain.substring(indexOfDot+1, domain.length());
			// recursively call ToHexString to the restDomain. 
			hexString = hexStringOfSize+hexStringOfDomain + ToHexString(restDomain);
		}
		// If domain not contain ".",such as com in "www.google.com".
		else {
			//Step 2. If domain has not "."  change to size+hexString+"00" 
			String hexStringOfSize = changeIntToHexString(domain.length());
			String hexStringOfDomain = changeStringToHexString (domain);
			hexString = hexStringOfSize + hexStringOfDomain + "00";
		}
		return hexString;
	}
	/* Construct the queries(as Wireshark DNS query's Hex view. In the format of
	 * 
	 */
	public static String constructQuery(String hexString) {
		String DNSQuery = "";
		return DNSQuery;
	}
	
	public static String constructQueryType(String QueryType){
		String qType = "";
		
		if(QueryType == null) {
			System.out.println("Please enter Type of DNS query as third argument.");
		}
		switch(QueryType) {
		case "A": qType = "0001"; 
				  break;
		case "NS":qType = "0002"; 
				  break;
		case "CNAME":qType = "0005"; 
					 break;
		default:
				 qType = "0001";
				 break;
			}
		return qType;	
	}
	
	// three input arguments. 
	public static void main(String[] args){ 
		/*An example of DNS message in wireshark.
		String s = "oe5f0100000100000000000003777777087374616e666f7264036564750000010001";*/
		/* test for hexidecimal string of www.google.com.hk
		System.out.println("test for convert string of domain to string of hexcical(byte)");
		System.out.println(changeIntToHexString(3));
		System.out.println(changeIntToHexString(6));
		System.out.println(changeIntToHexString(3));
		System.out.println(changeIntToHexString(2)); */
		//System.out.println(ToHexString("www.google.com.hk"));
		// The Hexidecimal String of www.google.com.hk 
		//System.out.println(ToHexString(args[1]));
		

		DomainName = args[1];
		
		InetAddress IPAddress = null;
		try {
			DNSServerIP = args[0]; //change DNSServerIP to InetAddress. 
			IPAddress = InetAddress.getByName(DNSServerIP);
		} catch (UnknownHostException e) {

			e.printStackTrace();
		}

		DatagramSocket clientSocket;
		try { 
			clientSocket = new DatagramSocket();

			//Encapsulate segment to a datagram and send to a name server.
			String UDPSegment = null; //TODO construct a DNS query message and pass it to UDP. 
			String MessageID = "5678";
			QueryType = args[2];
			String QType = constructQueryType(QueryType);
			String QClass = "0001";   // set the Qclass to be "IN":0001. 
			String s = ToHexString(args[1]);
			String newS = MessageID+"01000001000000000000"+s+QType+QClass;
			byte[] sendData = hexStringToByteArray(newS);


			//byte[] sendData = UDPSegment.getBytes();
			//byte[] sendData = {'6','7','3','a','0,'1',00,00,01,00,00,00,00,00,00,03,77,77,77,08,73,74,61,6e,66,6f,72,64,03,65,75,00,00,01,00,01};

			byte[] receiveData = new byte[1024];
			DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress , 53);
			try {

				clientSocket.send(sendPacket);
				/* check for sendData's correctness. 
				for(int i = 0; i < sendData.length; i++){
					System.out.println(sendData[i]);}  */
				DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
				clientSocket.receive(receivePacket);
				// check for response data: 
				 for(int i = 0; i < receiveData.length; i++){
				 
					System.out.print(receiveData[i]+"-");
				}
				String modifiedSentence =
						new String(receivePacket.getData());
				//System.out.println(modifiedSentence);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			clientSocket.close();


			//TODO print out the response(modifiedSentice) to the output of "dig".
			// http://en.wikipedia.org/wiki/Dig_%28command%29

		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}


/* TODO Example of DNS record for an Internet address. 
 *  https://eyeasme.com/Shayne/DNSLOOKUP/DNSLookup.java */

//TODO Bit manipulation:
//	TODO bitwise operation 

