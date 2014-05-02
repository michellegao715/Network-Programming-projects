import java.io.*;
import java.lang.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.zip.*;
 
public class Server extends RequestSender {
	private HashMap<String, RandomAccessFile> openedFiles;
 
    public Server(int port) {
		super("0.0.0.0", port);
		openedFiles = new HashMap<String, RandomAccessFile>();
    }
	
	private void handleFileSizeRequest(String requestHeader, InetAddress addr, int port) {
		String filename = requestHeader.substring(FILE_SIZE_REQUEST_HEADER.length());
		RandomAccessFile file = null;
		try {
			if(openedFiles.containsKey(filename))
				file = openedFiles.get(filename);
			else {
				file = new RandomAccessFile(filename, "r");
				openedFiles.put(filename, file);
			}
			long fileSize = file.length();
			sendResponse(FILE_SIZE_RESPONSE_HEADER, String.valueOf(fileSize).getBytes(), addr, port);
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	private void handleFileSegmentRequest(String requestHeader, InetAddress addr, int port) {
		String filenameAndSegmentId = requestHeader.substring(FILE_SEGMENT_REQUEST_HEADER.length());
		String[] parts = filenameAndSegmentId.split("\t");
		String filename = parts[0];
		int segmentId = Integer.parseInt(parts[1]);
		byte[] contentData = new byte[DATA_LENGTH];
		RandomAccessFile file = openedFiles.get(filename);
		try {
			file.read(contentData, segmentId * DATA_LENGTH, DATA_LENGTH);
		} catch(Exception e) {
			e.printStackTrace();
		}
		sendResponse(FILE_SEGMENT_RESPONSE_HEADER + segmentId, contentData, addr, port);
	}
	
	public void start() {
		byte[] receiveData = new byte[PACKET_LENGTH];
		String requestHeader = "";
		InetAddress addr = null;
		int port = 0;
		
		try {
			socket = new DatagramSocket(serverPort);
		} catch(Exception e) {
			e.printStackTrace();
		}
		
		while(true)
		{
			try {
				DatagramPacket receivePacket = new DatagramPacket(receiveData, PACKET_LENGTH);
				socket.receive(receivePacket);
				System.out.println("Received request");
				addr = receivePacket.getAddress();
				port = receivePacket.getPort();
				byte[] rawData = receivePacket.getData();
				System.out.println(rawData.length);
				if(!verifyCRC32(rawData))
					continue;
				requestHeader = getHeader(rawData);
			} catch(Exception e) {
				e.printStackTrace();
			}
			System.out.println("request header: " + requestHeader);
			if(requestHeader.startsWith(FILE_SIZE_REQUEST_HEADER)) {
				handleFileSizeRequest(requestHeader, addr, port);	
			} else if(requestHeader.startsWith(FILE_SEGMENT_REQUEST_HEADER)) {
				handleFileSegmentRequest(requestHeader, addr, port);	
			}
		}
	}
 
    public static void main(String[] args) {
		int port = Integer.parseInt(args[0]);
		Server server = new Server(port);
		server.start();
    }
}
