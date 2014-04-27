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

public class NewProxyServer implements Runnable {
	private static int port;
	/** Socket for client connections */
	private static ServerSocket socket;
	
	private static int peerPort = 7654;

	// Save request and response in proxy as hashmap. 
	private static HashMap<String, HttpResponse> cache = new HashMap<String, HttpResponse>();
	
	private static NewAnnounceReceiver receiver;
	
	public NewProxyServer(NewAnnounceReceiver receiver, HashMap<String, HttpResponse> cache) {
		this.receiver = receiver;
		this.cache = cache;
	}
	
	public HashMap<String, HttpResponse> getCache() {
		return cache;
	}

	/** Create the ProxyCache object and the socket */
	public static void init(int p) {
		port = p;
		try {
			socket = new ServerSocket(port);
		} catch (IOException e) {
			System.out.println("NewProxyServer: Error creating socket: " + e);
			System.exit(-1);
		}
	}
	
	public static void handle(Socket client) {
		Socket server = null;
		HttpRequest request = null;
		HttpResponse response = null;

		System.out.println("NewProxyServer: In handle()");

		/* Process request. If there are any exceptions, then simply
			* return and end this request. This unfortunately means the
			* client will hang for a while, until it timeouts. */

			/* Read request */
		try {
			System.out.println("NewProxyServer: read request");
			BufferedReader fromClient = new BufferedReader(new InputStreamReader(client.getInputStream()));
			System.out.println("NewProxyServer: BufferedReader constructed");
			request = new HttpRequest(fromClient);
			System.out.println("NewProxyServer: request constructed");
		} catch (IOException e) {
			System.out.println("NewProxyServer: Error reading request from client: " + e);
			return;
		}

		// If the request and response have been cached, then proxy send response back to client.
		System.out.println("NewProxyServer: hashcode: " + request.URI.hashCode());
		if(cache.containsKey(request.URI)) {
			final double startLocalCache = System.currentTimeMillis();
			response =  cache.get(request.URI);
			DataOutputStream toClient;
			try {
				toClient = new DataOutputStream(client.getOutputStream());
				toClient.writeBytes(response.toString());
				toClient.write(response.body);
			} catch (IOException e1) {
				System.out.println("NewProxyServer: Fail t sent request to client, error: "+e1.toString());
			}
			final double endLocalCache = System.currentTimeMillis();
			double time0 = ((endLocalCache-0.000)-(startLocalCache-0.000))/1000;
			System.out.println("time to return an object cached locally is "+time0);
		}
		/* If not be cached, Send request to server */
		else{
			/* First try to ask content from peers */
			boolean hasContentFromPeer = false;
			final double startSiblingCache = System.currentTimeMillis();
			try {
				HashSet<InetAddress> ips = receiver.getAllPeerIps();
				for(InetAddress addr : ips) {
					System.out.println("NewProxyServer: send request to peer: " + addr.toString());
					Socket peer = new Socket(addr, peerPort);
					DataOutputStream toPeer = new DataOutputStream(peer.getOutputStream());
					toPeer.writeBytes(request.toString());
					DataInputStream fromPeer = new DataInputStream(peer.getInputStream());
					System.out.println("NewProxyServer: parsing response from peer: " + addr.toString());
					response = new HttpResponse(fromPeer);
					System.out.println("NewProxyServer: finished parsing response from peer: " + addr.toString());

					final double endSiblingCache = System.currentTimeMillis();
					double time1 = ((endSiblingCache-0.000)-(startSiblingCache-0.000))/1000;
					System.out.println("time to return an object from a sibling cache is "+time1);

					if(response.validResponse) {
						System.out.println("NewProxyServer: response from peer is valid");
						hasContentFromPeer = true;
						break;
					}
					peer.close();
				}
			} catch (Exception e) {
			}

			/* If none of the peer has the requested content, ask from server */
			if(!hasContentFromPeer) {
			final double startFromServer = System.currentTimeMillis();
				try {
					System.out.println("NewProxyServer: send request to server");
					/* Open socket and write request to socket */
					System.out.println("NewProxyServer: host: " + request.getHost() + ", port: " + request.getPort());
					server = new Socket(request.getHost(), request.getPort());  
					DataOutputStream toServer = new DataOutputStream(server.getOutputStream());
					toServer.writeBytes(request.toString());
					System.out.println("NewProxyServer: read response from server");
					DataInputStream fromServer = new DataInputStream(server.getInputStream());
					response = new HttpResponse(fromServer);
				} catch (UnknownHostException e) {
					System.out.println("NewProxyServer: Unknown host: " + request.getHost());
					System.out.println(e);
					return;
				} catch (IOException e) {
					System.out.println("NewProxyServer: Error writing request to server: " + e);
					return;
				}
			final double endFromServer = System.currentTimeMillis();
				double time2 = ((endFromServer-0.000)-(startFromServer-0.000))/1000;
				System.out.println("time to return an object from a server is "+time2);
			}
		
			/* Forward response to client, */
			try {
				DataOutputStream toClient = new DataOutputStream(client.getOutputStream());

				/* Write response to client. First headers, then body */  
				toClient.writeBytes(response.toString()); 
				toClient.write(response.body);

				/* Insert object into the cache */
				cache.put(request.URI, response);

				client.close();
				server.close();

			}catch (IOException e) {
				System.out.println("NewProxyServer: Error writing response to client: " + e);
			}
		}
	}
		
	@Override
	public void run() {
		System.out.println("NewProxyServer");
		
		int myPort = 5678;
		
		init(myPort);

		/** Main loop. Listen for incoming connections and spawn a new
			* thread for handling them */
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
