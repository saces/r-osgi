package ch.ethz.iks.r_osgi.http.acceptor;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import ch.ethz.iks.r_osgi.http.HttpMessage;
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
			DataOutputStream remoteOut) throws UnknownHostException,
			IOException {
		this.hostname = hostname;
		this.remoteIn = remoteIn;
		this.remoteOut = remoteOut;
		Socket socket = new Socket("localhost", R_OSGi_PORT);
		this.localIn = new DataInputStream(socket.getInputStream());
		this.localOut = new DataOutputStream(socket.getOutputStream());
		new Outgoing().start();
		new Incoming().start();
	}

	private class Outgoing extends Thread {
		public void run() {
			final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			final byte[] buffer = new byte[bufferSize];
			while (running) {
				try {
					int len;
					int available = localIn.available();
					while (available > 0
							&& (len = localIn.read(buffer, 0,
									available < bufferSize ? available
											: bufferSize)) > -1) {
						bytes.write(buffer, 0, len);
						System.out
								.println("read " + new String(buffer, 0, len));
						available = localIn.available();
					}
					HttpRequest req = new HttpRequest("/r-osgi");
					req.setContent(bytes.toByteArray());
					remoteOut.write(req.getBytes(hostname, HttpRequest.POST));
					bytes.reset();
				} catch (IOException ioe) {
					ioe.printStackTrace();
				}
			}
		}
	}

	private class Incoming extends Thread {
		public void run() {
			while (running) {
				try {
					HttpMessage msg = HttpMessage.getMessage(remoteIn);
					if (msg instanceof HttpRequest) {
						localOut.write(msg.getContent());
					}
				} catch (IOException ioe) {
					ioe.printStackTrace();
				}
			}
		}
	}

}
