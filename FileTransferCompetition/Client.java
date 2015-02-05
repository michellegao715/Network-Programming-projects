import java.io.*;
import java.lang.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.zip.*;

class FileFetcher extends RequestSender implements Runnable {
	private Thread thread;
	private String name;
	private String filename;
	private int fetcherId;
	private int numFetchers;
	private long numSegments;
	private RandomAccessFile file;
   
	public FileFetcher(String name, String serverIP, int serverPort, String filename, int fetcherId, int numFetchers, long numSegments, RandomAccessFile file){
		super(serverIP, serverPort);
		this.name = name;
		this.filename = filename;
		this.fetcherId = fetcherId;
		this.numFetchers = numFetchers;
		this.numSegments = numSegments;
		this.file = file;
	}
	
	private synchronized void writeToFile(long segmentId, byte[] contentData) {
		try {
			file.seek(segmentId * DATA_LENGTH);
			file.write(contentData);
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
   
	public void run() {
		try {
			connect();
			
			byte[] contentData = new byte[DATA_LENGTH];
			
			for(long i = fetcherId;i < numSegments;i += numFetchers) {
				sendRequest(Client.FILE_SEGMENT_REQUEST_HEADER + filename + "\t" + i, FILE_SEGMENT_RESPONSE_HEADER + i, contentData);
				writeToFile(i, contentData);
			}
			
			close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
 
public class Client extends RequestSender {
	static final int NUM_THREADS = 8;
 
	public Client(String ip, int port) {
		super(ip, port);
	}
	
	private RandomAccessFile createFile(String filename, long size) {
		RandomAccessFile file = null;
		try {
			file = new RandomAccessFile(filename, "rw");
			file.setLength(size);
		} catch(Exception e) {
			e.printStackTrace();
		}
		return file;
	}
	
	public void getFile(String filename) {
		try {
			byte[] contentData = new byte[DATA_LENGTH];
		
			// 1. Get file size
			System.out.println("Getting file size");
			long totalFileSize = -1;
			sendRequest(FILE_SIZE_REQUEST_HEADER + filename, FILE_SIZE_RESPONSE_HEADER, contentData);
			totalFileSize = Long.parseLong(byteArray2String(contentData));
			long numSegments = (totalFileSize - 1) / DATA_LENGTH + 1;
			System.out.println("file size: " + numSegments);
		
			// 2. Create file to save data
			RandomAccessFile file = createFile(filename, totalFileSize);
		
			// 3. Get all file segments
			List<Thread> threads = new ArrayList<Thread>();
			for(int i = 0;i < NUM_THREADS;++i) {
				FileFetcher fetcher = new FileFetcher("", serverIP, serverPort, filename, i, NUM_THREADS, numSegments, file);
				Thread thread = new Thread(fetcher);
				thread.start();
				threads.add(thread);
			}
			for (Thread thread: threads) {
				thread.join();
			}
		
			file.close();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
 
	public static void main(String[] args) {
		String serverIP = args[0];
		int serverPort = Integer.parseInt(args[1]);
		String requestedFileName = args[2];
		Client client = new Client(serverIP, serverPort);
		client.connect();
		client.getFile(requestedFileName);
		client.close();
	}
}
