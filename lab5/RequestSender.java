import java.io.*;
import java.lang.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.zip.*;

class RequestSender {
	static final int HEADER_LENGTH = 64;
	static final int DATA_LENGTH = 1024;
	static final int CONTENT_LENGTH = HEADER_LENGTH + DATA_LENGTH;
	static final int CHECKSUM_LENGTH = 4;
	static final int PACKET_LENGTH = CONTENT_LENGTH + CHECKSUM_LENGTH;
	static final String FILE_SIZE_REQUEST_HEADER = "REQUESTING FILE ";
	static final String FILE_SIZE_RESPONSE_HEADER = "FILE SIZE";
	static final String FILE_SEGMENT_REQUEST_HEADER = "REQUESTING SEGMENT ";
	static final String FILE_SEGMENT_RESPONSE_HEADER = "SEGMENT ";
	
	protected String serverIP;
	protected int serverPort;
	private Checksum errorDetector;
	protected DatagramSocket socket;
	
	public RequestSender(String ip, int port) {
		this.serverIP = ip;
		this.serverPort = port;
		this.errorDetector = new CRC32();
	}
	
	protected String byteArray2String(byte[] arr) {
		int i;
		for (i = 0; i < arr.length && arr[i] != 0; i++) {
		}
		return new String(arr, 0, i);
	}
	
	private void fillHeader(String header, byte[] sendData) {
		byte[] tmp;
		tmp = header.getBytes();
		System.arraycopy(tmp, 0, sendData, 0, tmp.length);
	}
	
	private void fillContent(byte[] content, byte[] sendData) {
		System.arraycopy(content, 0, sendData, HEADER_LENGTH, content.length);
	}
	
	private void fillPayload(String header, byte[] sendData) {
		fillHeader(header, sendData);
		calculateCRC32(sendData);
	}
	
	private void fillPayload(String header, byte[] content, byte[] sendData) {
		fillHeader(header, sendData);
		fillContent(content, sendData);
		calculateCRC32(sendData);
	}
	
	protected String getHeader(byte[] rawData) {
		byte[] headerData = new byte[HEADER_LENGTH];
		System.arraycopy(rawData, 0, headerData, 0, HEADER_LENGTH);
		return byteArray2String(headerData);
	}
	
	protected void getContent(byte[] rawData, byte[] contentData) {
		System.arraycopy(rawData, HEADER_LENGTH, contentData, 0, DATA_LENGTH);
	}
	
	private boolean verifyResponseHeader(byte[] rawData, String expectedResponsePrefix) {
		byte[] headerData = new byte[HEADER_LENGTH];
		System.arraycopy(rawData, 0, headerData, 0, HEADER_LENGTH);
		String header = new String(headerData);
		return header.startsWith(expectedResponsePrefix);
	}
	
	private void calculateCRC32(byte[] data) {
		errorDetector.reset();
		errorDetector.update(data, 0, CONTENT_LENGTH);
		long checksum = errorDetector.getValue();
		byte[] checksumBytes = ByteBuffer.allocate(4).putInt((int)(checksum + Integer.MIN_VALUE)).array();
		for(int i = 0;i < CHECKSUM_LENGTH;++i) {
			data[CONTENT_LENGTH + i] = checksumBytes[i];
		}
	}
	
	protected boolean verifyCRC32(byte[] data) {
		byte[] checksumBytes = new byte[CHECKSUM_LENGTH];
		for(int i = 0;i < CHECKSUM_LENGTH;++i)
			checksumBytes[i] = data[CONTENT_LENGTH + i];
		calculateCRC32(data);
		for(int i = 0;i < CHECKSUM_LENGTH;++i)
			if(checksumBytes[i] != data[CONTENT_LENGTH + i])
				return false;
		return true;
	}
	
	protected void sendRequest(String request, String expectedResponsePrefix, byte[] contentData) {
		byte[] sendData = new byte[PACKET_LENGTH];
		byte[] receiveData = new byte[PACKET_LENGTH];
		String response;
		while(true) {
			try {
				fillPayload(request, sendData);
				DatagramPacket sendPacket = new DatagramPacket(sendData, PACKET_LENGTH);
				socket.send(sendPacket);
				DatagramPacket receivePacket = new DatagramPacket(receiveData, PACKET_LENGTH);
				socket.receive(receivePacket);
				byte[] rawData = receivePacket.getData();
				if(!verifyCRC32(rawData))
					continue;
				if(!verifyResponseHeader(rawData, expectedResponsePrefix))
					continue;
				getContent(rawData, contentData);
			} catch(Exception e) {
				e.printStackTrace();
			}
			break;
		}
	}
	
	protected void sendResponse(String response, byte[] content, InetAddress addr, int port) {
		byte[] sendData = new byte[PACKET_LENGTH];
		try {
			fillPayload(response, content, sendData);
			DatagramPacket sendPacket = new DatagramPacket(sendData, PACKET_LENGTH, addr, port);
			socket.send(sendPacket);
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public void connect() {
		try {
			socket = new DatagramSocket();
			InetAddress addr = InetAddress.getByName(serverIP);
			socket.connect(addr, serverPort);
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public void close() {
		socket.close();
	}
}
