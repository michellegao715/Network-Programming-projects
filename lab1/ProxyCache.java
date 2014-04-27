import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;

public class ProxyCache {
	/** Port for the proxy */
	private static int port;
	/** Socket for client connections */
	private static ServerSocket socket;

	// Save request and response in proxy as hashmap. 
	private static HashMap<HttpRequest, HttpResponse> cache = new HashMap<HttpRequest, HttpResponse>();

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

		System.out.println("In handle()");

		/* Process request. If there are any exceptions, then simply
		 * return and end this request. This unfortunately means the
		 * client will hang for a while, until it timeouts. */

		/* Read request */
		try {
			System.out.println("read request");
			BufferedReader fromClient = new BufferedReader(new InputStreamReader(client.getInputStream()));
			System.out.println("BufferedReader constructed");
			request = new HttpRequest(fromClient);
			System.out.println("request constructed");
		} catch (IOException e) {
			System.out.println("Error reading request from client: " + e);
			return;
		}

		// If the request and response have been cached, then proxy send response back to client.
		System.out.println("hashcode: " + request.hashCode());
		if(cache.containsKey(request)) {
			System.out.println("find request in cache");
			response =  cache.get(request);
			System.out.println("return response from cache");
			DataOutputStream toClient;
			System.out.println("Before sending to client.");
			try {
				toClient = new DataOutputStream(client.getOutputStream());
				System.out.println("outputstream");
				toClient.writeBytes(response.toString());
				toClient.write(response.body);
				client.close();
			} catch (IOException e1) {
				System.out.println("Fail t sent request to client, error: "+e1.toString());
			}		
		}
		/* If not be cached, Send request to server */
		else{
			try {
				System.out.println("send request to server");
				/* Open socket and write request to socket */
				System.out.println("host: " + request.getHost() + ", port: " + request.getPort());
				server = new Socket(request.getHost(), request.getPort());  
				DataOutputStream toServer = new DataOutputStream(server.getOutputStream());
				toServer.writeBytes(request.toString());

			} catch (UnknownHostException e) {
				System.out.println("Unknown host: " + request.getHost());
				System.out.println(e);
				return;
			} catch (IOException e) {
				System.out.println("Error writing request to server: " + e);
				return;
			}
			/* Read response and forward it to client, */
			try {
				System.out.println("read response from server");
				DataInputStream fromServer = new DataInputStream(server.getInputStream());
				response = new HttpResponse(fromServer);
				DataOutputStream toClient = new DataOutputStream(client.getOutputStream());

				/* Write response to client. First headers, then body */  
				toClient.writeBytes(response.toString()); 
				toClient.write(response.body); 

				/* Insert object into the cache */
				cache.put(request, response);

				client.close();
				server.close();

			}catch (IOException e) {
				System.out.println("Error writing response to client: " + e);
			}
		}
	}

	/** Read command line arguments and start proxy */
	public static void main(String args[]) {
		int myPort = 0;

		try {
			myPort = Integer.parseInt(args[0]);
		} catch (ArrayIndexOutOfBoundsException e) {
			System.out.println("Need port number as argument");
			System.exit(-1);
		} catch (NumberFormatException e) {
			System.out.println("Please give port number as integer.");
			System.exit(-1);
		}

		init(myPort);

		/** Main loop. Listen for incoming connections and spawn a new
		 * thread for handling them */
		Socket client = null;

		while (true) {
			try {
				client = socket.accept();
				handle(client);
			} catch (IOException e) {
				System.out.println("Error reading request from client: " + e);
				/* Definitely cannot continue processing this request,
				 * so skip to next iteration of while loop. */
				continue;
			}
		}

	}
}
