import java.io.*;
import java.net.Socket;
 
public class Client {
    private DatagramSocket clientsocket = null;
    private ObjectOutputStream outputStream = null;
    private boolean isConnected = false;
    private String sourceFilePath = "/Users/richardchen331/testfile.txt";
    private FileEvent fileEvent = null;
    private String destinationPath = "/home/richardchen/workspace/cs621/lab5";
 	private byte[] sendData = new byte[1024];
	private byte[] receiveData = new byte[1024];

    public Client() {
 
    }
 
    /**
     * Connect with server code running in local host or in any other host
     */
    public void connect() {
        while (!isConnected) {
            try {
                socket = new Socket("10.0.0.25", 4445);
				
				DatagramPacket sendPacket = new DatagramPacket(sendData,sendData.length,"10.0.0.25",4445);
				clientsocket = new DatagramSocket();
				clientSocket.send(sendPacket());
				DatagramPacket receivePacket = new Datagra,Packet(receiveData,receiveData.length);
				clientSocket.receive(receivePacket);

                outputStream = new ObjectOutputStream(socket.getOutputStream());
                isConnected = true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
 
    /**
     * Sending FileEvent object.
     */
    public void sendFile() {
        fileEvent = new FileEvent();
        String fileName = sourceFilePath.substring(sourceFilePath.lastIndexOf("/") + 1, sourceFilePath.length());
        String path = sourceFilePath.substring(0, sourceFilePath.lastIndexOf("/") + 1);
        fileEvent.setDestinationDirectory(destinationPath);
        fileEvent.setFilename(fileName);
        fileEvent.setSourceDirectory(sourceFilePath);
        File file = new File(sourceFilePath);
        if (file.isFile()) {
            try {
                DataInputStream diStream = new DataInputStream(new FileInputStream(file));
                long len = (int) file.length();
                byte[] fileBytes = new byte[(int) len];
                int read = 0;
                int numRead = 0;
                while (read < fileBytes.length && (numRead = diStream.read(fileBytes, read,
                        fileBytes.length - read)) >= 0) {
                    read = read + numRead;
                }
                fileEvent.setFileSize(len);
                fileEvent.setFileData(fileBytes);
                fileEvent.setStatus("Success");
            } catch (Exception e) {
                e.printStackTrace();
                fileEvent.setStatus("Error");
            }
        } else {
            System.out.println("path specified is not pointing to a file");
            fileEvent.setStatus("Error");
        }
        //Now writing the FileEvent object to socket
        try {
            outputStream.writeObject(fileEvent);
            System.out.println("Done...Going to exit");
            Thread.sleep(3000);
            System.exit(0);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
 
    }
 
    public static void main(String[] args) {
        Client client = new Client();
        client.connect();
        client.sendFile();
    }
}
