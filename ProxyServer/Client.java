import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;


public class Client {
	public static void main(String[] args) throws UnknownHostException, IOException {
		HttpRequest request = null;
		HttpResponse response = null;
		
		Socket proxy = new Socket(InetAddress.getLocalHost(), 5678);
		/* Send request to server */
		try {
			System.out.println("send request to proxy");
			/* Open socket and write request to socket */
			DataOutputStream toServer = new DataOutputStream(proxy.getOutputStream());
			System.out.println("writing to socket");
			toServer.writeBytes("GET " + args[0] + " HTTP/1.1\nHost: " + args[1] + ":80\nAccept: text/html\nContent-Type: text/html; charset=UTF-8\n\n");
			toServer.flush();
			System.out.println("write finished");
			 
		} catch (UnknownHostException e) {
			System.out.println("Unknown host: " + request.getHost());
			System.out.println(e);
			return;
		} catch (IOException e) {
			System.out.println("Error writing request to server: " + e);
			return;
		}
		
		try {
			System.out.println("read from proxy");
			BufferedReader inFromServer =
					new BufferedReader(new InputStreamReader(
							proxy.getInputStream()));
			System.out.println("BufferedReader constructed");
			System.out.println("FROM SERVER: ");
			String sentence = inFromServer.readLine();
			while(sentence != null) {
				System.out.println(sentence);
				sentence = inFromServer.readLine();
			}
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
}
