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

	public static String changeStringToHexString(String s) {
		String completeHexString = "";
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			String oneHexString = changeIntToHexString(c);
			completeHexString += oneHexString;
		}
		return completeHexString;
	}

	// Call this method to change domain to hex string and then apply
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
		for (int i = 0; i < numOfAnswer; i++) { // each answer if 16 bytes long.
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
			if(type_s == "CNAME") {
				int sp = answerStartPos + 12;
				boolean first = true;
				while(true) {
					int l = response[sp];
				//	System.out.println("l = " + l);
					if(l == 0)
						break;
					if(l >= 192) {
						sp = response[sp] & 0xcf;
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

	// three input arguments.
	public static void main(String[] args) {
		DomainName = args[1];

		InetAddress IPAddress = null;
		try {
			DNSServerIP = args[0]; // change DNSServerIP to InetAddress.
			IPAddress = InetAddress.getByName(DNSServerIP);
		} catch (UnknownHostException e) {

			e.printStackTrace();
		}

		DatagramSocket clientSocket;
		try {
			clientSocket = new DatagramSocket();

			// Encapsulate segment to a datagram and send to a name server.
			// construct a DNS query message and pass it to UDP.
			String MessageID = "5778";
			QueryType = args[2];
			String QType = constructQueryClass(QueryType);
			String QClass = "0001"; // set the Qclass to be "IN":0001.
			String QName = ToHexString(args[1]);
			// Since the 3th and 4th byte of header is fixed size and this is
			// query so I set the first 2bit 01.
			String newS = MessageID + "01000001000000000000" + QName + QType
					+ QClass;
			byte[] sendData = hexStringToByteArray(newS);
			// byte[] sendData = UDPSegment.getBytes();

			byte[] originalReceiveData = new byte[200];
			DatagramPacket sendPacket = new DatagramPacket(sendData,
					sendData.length, IPAddress, 53);
			try {

				clientSocket.send(sendPacket);
				DatagramPacket receivePacket = new DatagramPacket(
						originalReceiveData, originalReceiveData.length);
				clientSocket.receive(receivePacket);

				// delete the UDP header. And use the DNS message format to
				// filter response.
				byte[] receiveData = receivePacket.getData();
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

				//System.out.println("startPos :" + startPos);
				//System.out.println("sendData--------");
				for (int i = 0; i < sendData.length; i++) {
					System.out.print(sendData[i] + "-");
				}
				//System.out.println("receiveData--------");
				for (int i = 0; i < receiveData.length; i++) {
					System.out.print(receiveData[i] + "-");
				}
				int numOfAnswer = ((response[6] & 0xffffffff) << 8)
						+ response[7];
				for (int i = 0; i < numOfAnswer; i++) { // each answer if 16
					// bytes long.
					// www.google.com.hk. 299 IN A 74.125.239.55

					int Rdlength = response[startPos + 10 + 16 * i] * 16
							+ response[startPos + 11 + 16 * i];
					//System.out.println("startPos" + startPos);
					//System.out.println("the length is " + Rdlength);
					/*System.out.println(response[startPos + 12 + 0 + 16 * i]
							+ "." + response[startPos + 12 + 1 + 16 * i] + "."
							+ response[startPos + 12 + 2 + 16 * i] + "."
							+ response[startPos + 12 + 3 + 16 * i]); */

				}
				// // Compare the receiveData's content with Wireshark's
				// captured packet from dig command.
				for (int i = 0; i < receiveData.length; i++) {
					String hex = Integer.toHexString(receiveData[i] & 0xFF);
					if (hex.length() == 1) {
						hex = "0" + hex;
					}
				//	System.out.print(hex.toUpperCase() + "-");
				}
				//System.out.println();

//				System.out.println("-------");
				getAnswer(receiveData, DomainName, startPos);
				
			} catch (IOException e) {
				System.out.println(e.toString());
			}
			clientSocket.close();
		} catch (SocketException e) {
			System.out.println(e.toString());
		}
	}
}


