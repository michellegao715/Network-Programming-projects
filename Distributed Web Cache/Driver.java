import java.util.*;

public class Driver {
	public static void main(String[] args) {
		HashMap<String, HttpResponse> cache = new HashMap<String, HttpResponse>();
		Thread serviceAnnounce = new Thread(new NewAnnounceService());
		NewAnnounceReceiver receiver = new NewAnnounceReceiver();
		Thread serviceDiscov = new Thread(receiver);
		System.out.println("before new proxy server");
		Thread mycache = new Thread(new NewProxyServer(receiver, cache));
		System.out.println("after proxy server");
		Thread peercache = new Thread(new NewhandlePeerResponse(cache));
		
		serviceAnnounce.start();
		serviceDiscov.start();
		mycache.start();
		peercache.start();
	}
}
