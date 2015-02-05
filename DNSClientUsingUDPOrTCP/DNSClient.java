import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Arrays;

public class DNSClient{
	private static String DNSServerIP = "";
	private static String DomainName = "";
	private static String QueryType = "";

	public static byte[] hexStringToByteArray(String s) {
		int len = s.length();
		byte[] data = new byte[len / 2];
		for (int i = 0; i < len; i += 2) {
			data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character
					.digit(s.charAt(i + 1), 16));
		}
		return data;
	}

	// convert domain name(www.google.com) to string(hexStringOfDomain) and then
	// to byte[]{03 77 77 77 06 67....}
	public static String changeIntToHexString(int i) {
		char low = i / 16 < 10 ? (char) (i / 16 + '0')
				: (char) (i / 16 - 10 + 'a');
		char high = i % 16 < 10 ? (char) (i % 16 + '0')
				: (char) (i % 16 - 10 + 'a');
		return "" + low + high;
	}
	// change int into hexString(length of 2 bytes): eg. 33--0021
	public static String intToTwoBytesOfHexString(int a){
		String s1 = changeIntToHexString(a);
		String s2 = ""; // if s1 = 21; s2 = 0021.
		for(int i=0; i < 4-s1.length(); i++){
			s2+="0";
		}
		s2+=s1;
		return s2;
	}
	public static String changeStringToHexString(String s) {
		String completeHexString = "";
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			String oneHexString = changeIntToHexString(c);
			completeHexString += oneHexString;
		}
		return completeHexString;
	}

	// Call this method to change domain to hex string and then apply, such as www.cnn.com to 03 77 77 77 03 63 6e 6e 03 63 6f 6d 00.
	// hexStringToByteArray to generate the byte[] to transfer to socket.
	public static String ToHexString(String domain) {
		// Step 1. If domain has "." convert anything before "." to hexString
		String hexString = "";

		if (domain.contains(".")) {
			int indexOfDot = domain.indexOf(".");
			// Change the size of anything before "." to hexstring.
			String hexStringOfSize = changeIntToHexString(indexOfDot);
			// Change anything before "." to hexstring.
			String hexStringOfDomain = changeStringToHexString(domain
					.substring(0, indexOfDot));

			String restDomain = domain.substring(indexOfDot + 1,
					domain.length());
			// recursively call ToHexString to the restDomain.
			hexString = hexStringOfSize + hexStringOfDomain
					+ ToHexString(restDomain);
		}
		// If domain not contain ".",such as com in "www.google.com".
		else {
			// Step 2. If domain has not "." change to size+hexString+"00"
			String hexStringOfSize = changeIntToHexString(domain.length());

			String hexStringOfDomain = changeStringToHexString(domain);
			hexString = hexStringOfSize + hexStringOfDomain + "00";
		}
		return hexString;
	}

	public static String constructQueryClass(String QueryType) {
		String qType = "";
		if (QueryType == null) {
			System.out
			.println("Please enter Type of DNS query as third argument.");
		}
		switch (QueryType) {
		case "A":
			qType = "0001";
			break;
		case "NS":
			qType = "0002";
			break;
		case "CNAME":
			qType = "0005";
			break;
		case "SOA":
			qType = "0006";
			break;
		case "WKS":
			qType = "000B";
			break;
		case "PTR":
			qType = "000C";
			break;
		case "MX":
			qType = "000F";
			break;
		case "SRV":
			qType = "0021";
			break;
		default:
			qType = "0001";
			break;
		}
		return qType;
	}

	public static String getClass(String hexStringOfClass) {
		String qType = "";
		switch (hexStringOfClass) {
		case "01":
			qType = "A";
			break;
		case "02":
			qType = "NS";
			break;
		case "05":
			qType = "CNAME";
			break;
		case "06":
			qType = "SOA";
			break;
		case "0B":
			qType = "WKS";
			break;
		case "0C":
			qType = "PTR";
			break;
		case "0F":
			qType = "MX";
			break;
		case "21":
			qType = "SRV";
			break;
		default:
			qType = "A";
			break;
		}
		return qType;
	}
	// call getAnswer in main: getAnswer(receiveData, QNAME, startPos) this
	// startPos is the length of query, since answer starts right after query.
	public static void getAnswer(byte[] receiveData, String DomainName,
			int startPos) {

		int[] response = new int[receiveData.length];
		// since the first two bytes of TCP's response is length, start with the third byte.

		for (int i = 0; i < receiveData.length; ++i) {
			if (receiveData[i] >= 0)
				response[i] = receiveData[i];
			else
				response[i] = receiveData[i] + 256;
		}
		// ;; ->>HEADER<<- opcode: QUERY, status: NOERROR, id: 2912
		// ;; flags: qr rd ra; QUERY: 1, ANSWER: 3, AUTHORITY: 0, ADDITIONAL: 0
		StringBuffer header = new StringBuffer();

		int id = ((response[0] & 0xffffffff) << 8) + response[1];
		int numOfAnswer = ((response[6] & 0xffffffff) << 8) + response[7];
		int numOfQuestion = ((response[4] & 0xffffffff) << 8) + response[5];
		int authorityRR = ((response[8] & 0xffffffff) << 8) + response[9];
		int additional = ((response[10] & 0xffffffff) << 8) + response[11]; 
		int qr = response[2] & 0x80;
		String QR = "";
		if (qr > 0) {
			QR = "Query";
		}
		// TODO add the status part in header.
		header.append(";; ->>HEADER<<- opcode: " + QR + " " + "id: " + id
				+ "\n" +";;flags:qr rd ra"+"   QUERY: "+numOfQuestion+ "   Answer: "+numOfAnswer+"   Authority: "+authorityRR+"   Additional: "+additional+"\n");

		header.append("");
		String Type = "";
		int type = ((response[startPos + 2] & 0xffffffff) << 8)
				+ response[startPos + 3];
		if (type > 0) {
			Type = "IN";
		}
		String Class = "";

		// construct AnswerSection www.google.com.hk. 299 IN A 74.125.239.55
		StringBuffer as = new StringBuffer();
		// Add the Time-To-Live in AnswerSection.
		int TTLMin = response[startPos + 6] * 16777216 + response[startPos + 7]
				* 65536 + response[startPos + 8] * 255 + response[startPos + 9];
		as.append(";; ANSWER SECTION:" + "\n");

		StringBuffer answer = new StringBuffer();
		int intClass = 0;
		int answerStartPos = startPos;
		for (int i = 0; i < numOfAnswer + authorityRR + additional; i++) { // each answer if 16 bytes long.
			intClass = ((response[answerStartPos + 4] & 0xffffffff) << 8)
					+ response[answerStartPos + 5];
			String hexStringClass = changeIntToHexString(intClass);
			Class = getClass(hexStringClass);

			int type_i = (response[answerStartPos + 2] << 8) + response[answerStartPos + 3];
			String type_s = "";
			if(type_i == 1)
				type_s = "A";
			else if(type_i == 2)
				type_s = "NS";
			else if(type_i == 5)
				type_s = "CNAME";
			else if(type_i == 6)
				type_s = "SOA";
			else if(type_i == 11)
				type_s = "WKS";
			else if(type_i == 12)
				type_s = "PTR";
			else if(type_i == 15)
				type_s = "MX";
			else if(type_i == 33)
				type_s = "SRV";
			else if(type_i == 38)
				type_s = "A6";
			else
				type_s = "UNKNOWN";

			long TTL = (response[answerStartPos + 6] << 24)
					+ (response[answerStartPos + 7] << 16)
					+ (response[answerStartPos + 8] << 8)
					+ response[answerStartPos + 9];
			// answer.append(DomainName+"\t"+TTL+"  "+ Type + "\t" + Class +
			// "\t"+receiveData[startPos+12+0+16*i]+"."+response[startPos+12+1+16*i]+"."+response[startPos+12+2+16*i]+"."+response[startPos+12+3+16*i]+"\n");
			int Rlength = ((response[answerStartPos + 10]) << 8)
					+ response[answerStartPos + 11];

			int WIDTH = 20;
			answer.append(DomainName);
			for (int j = 0; j < WIDTH - DomainName.length(); ++j) {
				answer.append(" ");
			}
			answer.append(TTL);
			int widOfTTL = 20;
			for (int j = 0; j < widOfTTL - new Long(TTL).toString().length(); ++j) {
				answer.append(" ");
			}
			answer.append(type_s);
			int widOfClass = 10;
			for (int j = 0; j < widOfClass - Class.length(); ++j) {
				answer.append(" ");
			}
			answer.append(type);
			for (int j = 0; j < WIDTH - Type.length(); ++j)
				answer.append(" ");
			if(type_s == "CNAME" || type_s == "NS") {
				int sp = answerStartPos + 12; // 12 is the offset to get to RDATA. 
				boolean first = true;
				while(true) {
					int l = response[sp]; // length of 
					//	System.out.println("l = " + l);
					if(l == 0) // when come to 00,it's the end of answer.
						break;
					if(l >= 192) {  // get next 14 bits specifies offset in packet of name.(when comes to "c0"(192)then the next byte is the offset,
						//from which we can get the name for the "NS" query: such as n1.google.com 
						//response[sp], response[sp+1] two bytes, get the last 14 bits of the two bytes according to this:
						//http://f12.class2go.stanford.edu/networking/Fall2012/videos/dns2
						sp = ((response[sp] & 0x3f) << 8) | (response[sp+1] & 0xff);
						continue;
					}
					if(!first)
						answer.append('.');
					first = false;
					for(int j = 0;j < l;++j)
						answer.append((char)response[sp + 1 + j]);
					sp += (1 + l);
				}
			}
			else {
				answer.append(response[answerStartPos + 12 + 0] + "."
						+ response[answerStartPos + 12 + 1] + "."
						+ response[answerStartPos + 12 + 2] + "."
						+ response[answerStartPos + 12 + 3]);
			}
			answer.append("\n");
			answerStartPos += (12 + Rlength);

		}
		//System.out.println("this is 4 byte for ttl .......");

		// add the header section and question section and answer section.
		header.append(as).append(answer);
		System.out.println(header);
	}


	/*TODO deal with wrong input error : such as not tdp(not tcp or udp for args[0]
		4 input arguments: example input:  udp 8.8.8.8 www.cnn.com A
			if(args[0].equalsIgnoreCase("UDP")) {
				Protocol = "udp";
			}
			else if (args[0].equalsIgnoreCase("TCP")) {
				Protocol = "tcp";
			}
	 */

	/*  TODO : un-comment to get the three arguments.
			DNSServerIP = args[1];
			DomainName = args[2];
			QueryType = args[3];
	 */	
	// This method is turn integer to two bytes' hexString(such as the length is 33, then hexString is 0021)

	public static String getLenString(int i) {
		if(i == 33)
			return "0021";
		else
			return "0000";
	}
	//  this is for the prefix of TCP request, which is the length of the request.
	//	public static  byte[] intToTwoBytes(int a){
	//		int i = 1025;
	//		byte[] b = {1, 0};
	//		System.out.println(i & 0x00F0);
	//		b[0] = (byte)((i & 0xff00) >> 8);
	//		b[1] = (byte)(i & 0x00ff);
	//		return b;
	//	}
	// convert the length to string of length of 4.
	public static String intToTwoBytes(int a){
		StringBuilder aString = new StringBuilder();
		for(int i =4; i > aString.length(); i--) {
			aString.append("0");
		}
		aString.append(a);
		System.out.println("the string of int is "+aString.toString());
		return aString.toString();
	}

	// convert the inputstream of TCP to byte array.
	/*public static byte[] readMessage(DataInputStream din) throws IOException {
		int msgLen =din.readInt(); 
		byte[] msg = new byte[msgLen];
		din.readFully(msg);
		return msg;
	}*/
	// three input arguments.
	public static void main(String[] args) {
		// String protocol = "tcp";
		String protocol = args[0];

		DomainName = args[2];

		InetAddress IPAddress = null;
		try {
			DNSServerIP = args[1]; // change DNSServerIP to InetAddress.
			IPAddress = InetAddress.getByName(DNSServerIP);
		} catch (UnknownHostException e) {

			e.printStackTrace();
		}

		/* Encapsulate segment to a datagram and send to a name server.
			 construct a DNS query message and pass it to TCP.*/
		int minimum = 1000;
		int maximum = 8999;
		int randomNum = minimum + (int)(Math.random()*maximum);
		String MessageID = String.valueOf(randomNum);

		QueryType = args[3];
		String QType = constructQueryClass(QueryType);
		String QClass = "0001"; // set the Qclass to be "IN":0001.
		String QName = ToHexString(args[2]);
		// Since the 3th and 4th byte of header is fixed size and this is
		// query so I set the first 2bit 01.
		String newS1=MessageID + "01000001000000000000" + QName + QType+ QClass;

		// this the original hex string of message length(not 2 bytes), 
		// TODO right shift to create 0000000000000020 from 20.
		//		String messageLengthHex =changeIntToHexString(hexStringToByteArray(newS1).length);
		//		System.out.println("the hex string of message length is "+messageLengthHex);
		//		System.out.println("the length of message is "+hexStringToByteArray(newS1).length);

		//String byteS = intToTwoBytes(hexStringToByteArray(newS1).length) + "";

		//since one hexString's length is 4 it, the length should be news1/2;
		String newS = "";
		if(protocol.equals("tcp"))
			newS = intToTwoBytesOfHexString(newS1.length()/2) + MessageID + "01000001000000000000" + QName + QType + QClass;
		else
			newS = MessageID + "01000001000000000000" + QName + QType + QClass;
		System.out.println("two bytes' hexString of int "+intToTwoBytesOfHexString(newS1.length()/2));
		System.out.println("the length of newS1 "+newS1.length()/2);
		byte[] sendData = hexStringToByteArray(newS);

		double totalTime = 0.000;
		int timeOfExp = 10;

		double[] timeArray = new double[10];  //keep all execution time in an array to get the max and min of the execution time.
		for(int t =0; t <timeOfExp; t++){
			for(int k=0; k< 0; k++){
				int _randomNum = minimum + (int)(Math.random()*maximum);
				String _MessageID = String.valueOf(_randomNum);
				String _newS = "";
				if(protocol.equals("tcp"))
					_newS = intToTwoBytesOfHexString(newS1.length()/2) + _MessageID + "01000001000000000000" + QName + QType + QClass;
				else
					_newS = _MessageID + "01000001000000000000" + QName + QType + QClass;
				byte[] _sendData = hexStringToByteArray(_newS);
				try {
					if(protocol.equals("tcp")) {
						Socket tcpclientSocket = new Socket(args[1], 53);
						DataOutputStream outToServer = new DataOutputStream(
								tcpclientSocket.getOutputStream());
						BufferedReader inFromServer =
								new BufferedReader(new InputStreamReader(
										tcpclientSocket.getInputStream()));
						outToServer.write(sendData);
					}
					else {
						DatagramSocket _clientSocket = new DatagramSocket();
						DatagramPacket _sendPacket = new DatagramPacket(_sendData,sendData.length, IPAddress, 53);
						_clientSocket.send(_sendPacket);
					}
				} catch (Exception e) {
				}
			}
			DatagramSocket clientSocket = null;
			byte[] receiveData;
			final double startTime = System.currentTimeMillis();
			try {
				if(protocol.equals("tcp")) {
					Socket tcpclientSocket = new Socket(args[1], 53);
					DataOutputStream outToServer = new DataOutputStream(
							tcpclientSocket.getOutputStream());

					//Response
					BufferedReader inFromServer =
							new BufferedReader(new InputStreamReader(
									tcpclientSocket.getInputStream()));
					outToServer.write(sendData);

					InputStream din =  tcpclientSocket.getInputStream();

					int	byteCount;
					byte inputBuffer[] = new byte[1024];
					System.out.println("xxxxxxxxxxxxxx");
					while ((byteCount = din.read(inputBuffer, 0, 1024)) != -1){
						// @SuppressWarnings("deprecation")
						// String	stuff = new String(inputBuffer, 0, 0, byteCount);
					}

					receiveData = inputBuffer;
				}
				else {
					clientSocket = new DatagramSocket();
					byte[] originalReceiveData = new byte[1024];
					DatagramPacket sendPacket = new DatagramPacket(sendData,sendData.length, IPAddress, 53);

					clientSocket.send(sendPacket);
					DatagramPacket receivePacket = new DatagramPacket(originalReceiveData, originalReceiveData.length);
					clientSocket.receive(receivePacket);

					receiveData = receivePacket.getData();
				}

				final double endTime = System.currentTimeMillis();
				double time = ((endTime-0.000)-(startTime-0.000))/1000;
				timeArray[t] = time;
				totalTime+=time;
				System.out.println("The "+(t+1)+"th execution time is "+time);
				try(PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter("output", true)))) {
					out.println("The "+(t+1)+"th execution time is "+time);
				}catch (IOException e) {
				}
				System.out.println("Until now the total execution time is "+totalTime+" seconds");

				if(protocol.equals("tcp")) {
					// Remove the first two bytes of receiveData and analysis the rest.
					byte receiveData1[] = Arrays.copyOfRange(receiveData, 2, receiveData.length-1);
					int lengthOfQuery = sendData.length - 2;
					int startPos = lengthOfQuery;
					getAnswer(receiveData1, DomainName, startPos);
				}
				else {
					int[] response = new int[receiveData.length];
					for (int i = 0; i < receiveData.length; ++i) {
						if (receiveData[i] >= 0)
							response[i] = receiveData[i];
						else
							response[i] = receiveData[i] + 256;
					}

					int lengthOfQuery = sendData.length;
					int lengthOfResponse = receiveData.length;
					int startPos = lengthOfQuery;

					getAnswer(receiveData, DomainName, startPos);
				}
			}catch (SocketException e) {
				System.out.println(e.toString());
			}
			catch (IOException e) {
				System.out.println(e.toString());
			} 
			if(protocol.equals("udp"))
				clientSocket.close();
		}
		double averageTime = (totalTime/timeOfExp);
		System.out.println("the average time of execution is "+averageTime);
		Arrays.sort(timeArray);
		System.out.println("the min time is "+timeArray[0]);
		System.out.println("the max time is "+timeArray[timeOfExp-1]);
	}

}
