package ch.ethz.iks.r_osgi.http.acceptor;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import ch.ethz.iks.r_osgi.http.HttpRequest;

public class HttpBridge {
	private static final int R_OSGi_PORT = 9278;

	private final int bufferSize = 1024;

	private final DataInputStream remoteIn;

	private final DataOutputStream remoteOut;

	private final DataInputStream localIn;

	private final DataOutputStream localOut;

	private boolean running = true;

	private final String hostname;

	public HttpBridge(String hostname, DataInputStream remoteIn,
			DataOutputStream remoteOut) throws Exception {
		System.out.println("starting HTTP bridge for " + hostname);
		this.hostname = hostname;
		this.remoteIn = remoteIn;
		this.remoteOut = remoteOut;
		System.out.println("now opening local socket");
		Socket socket = new Socket("localhost", R_OSGi_PORT);
		this.localIn = new DataInputStream(socket.getInputStream());
		this.localOut = new DataOutputStream(socket.getOutputStream());
		new Incoming().start();
		System.out.println("bridge started.");
	}

	private class Incoming extends Thread {
		public void run() {
			while (running) {
				try {
					HttpRequest msg = new HttpRequest(remoteIn);
					if (msg instanceof HttpRequest) {
						localOut.write(msg.getContent());
					}
				} catch (Exception ioe) {
					ioe.printStackTrace();
				}
			}
		}
	}

}
