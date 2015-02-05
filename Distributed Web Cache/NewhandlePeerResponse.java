import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.*;
import java.net.InetAddress;

public class NewhandlePeerResponse implements Runnable {
	private static int port;
	/** Socket for client connections */
	private static ServerSocket socket;
	
	private static int peerPort = 7654;
	
	private static HashMap<String, HttpResponse> cache = new HashMap<String, HttpResponse>();

	public NewhandlePeerResponse(HashMap<String, HttpResponse> cache) {
		this.cache = cache;
	}

	/** Create the ProxyCache object and the socket */
	public static void init(int p) {
		port = p;
		try {
			socket = new ServerSocket(port);
		} catch (IOException e) {
			System.out.println("Error creating socket: " + e);
			System.exit(-1);
		}
	}
	
	public static void handle(Socket client) {
		Socket server = null;
		HttpRequest request = null;
		HttpResponse response = null;

		System.out.println("NewhandlePeerResponse: In handle()");

		/* Process request. If there are any exceptions, then simply
			* return and end this request. This unfortunately means the
			* client will hang for a while, until it timeouts. */

			/* Read request */
		try {
			System.out.println("NewhandlePeerResponse: read request");
			BufferedReader fromClient = new BufferedReader(new InputStreamReader(client.getInputStream()));
			System.out.println("NewhandlePeerResponse: BufferedReader constructed");
			request = new HttpRequest(fromClient);
			System.out.println("NewhandlePeerResponse: request constructed");
		} catch (IOException e) {
			System.out.println("NewhandlePeerResponse: Error reading request from client: " + e);
			return;
		}

		// If the request and response have been cached, then proxy send response back to client.
		System.out.println("NewhandlePeerResponse: hashcode: " + request.URI.hashCode());
		if(cache.containsKey(request.URI)) {
			System.out.println("NewhandlePeerResponse: response found in cache");
			response =  cache.get(request.URI);
			DataOutputStream toClient;
			try {
				toClient = new DataOutputStream(client.getOutputStream());
				toClient.writeBytes(response.toString());
				toClient.write(response.body);
			} catch (IOException e1) {
				System.out.println("NewhandlePeerResponse: Fail t sent request to client, error: "+e1.toString());
			}
		}
		else {
			System.out.println("NewhandlePeerResponse: response unavailable in cache");
			DataOutputStream toClient;
			try {
				toClient = new DataOutputStream(client.getOutputStream());
				toClient.writeBytes("DATA UNAVAILABLE IN CACHE\n");
			} catch (IOException e1) {
				System.out.println("NewhandlePeerResponse: Fail t sent request to client, error: "+e1.toString());
			}
		}
	}
		
	@Override
	public void run() {
		System.out.println("NewhandlePeerResponse");
		
		init(peerPort);

		/** Main loop. Listen for incoming connections and spawn a new thread for handling them */
		Socket client = null;

		while (true) {
			try {
				client = socket.accept();
				handle(client);
				System.out.println("check proxy is working.");
			} catch (IOException e) {
				System.out.println("Error reading request from client: " + e);
				/* Definitely cannot continue processing this request,
					* so skip to next iteration of while loop. */
					continue;
			}
		}
	}
}
