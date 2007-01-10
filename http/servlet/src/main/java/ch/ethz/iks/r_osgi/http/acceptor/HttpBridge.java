package ch.ethz.iks.r_osgi.http.acceptor;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import ch.ethz.iks.r_osgi.http.HttpRequest;

public class HttpBridge {
	private static final int R_OSGi_PORT = 9278;

	private final int bufferSize = 1024;

	private static final ObjectInputStream localIn;

	private static final ObjectOutputStream localOut;

	private boolean running = true;

	private final String hostname;

	static {
		try {
			System.out.println("now opening local socket");
			Socket socket = new Socket("localhost", R_OSGi_PORT);
			localIn = new ObjectInputStream(socket.getInputStream());
			localOut = new ObjectOutputStream(socket.getOutputStream());
			localOut.flush();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}

	public HttpBridge(String hostname, DataInputStream remoteIn,
			DataOutputStream remoteOut) throws Exception {
		System.out.println("bridge started.");
	}

}
