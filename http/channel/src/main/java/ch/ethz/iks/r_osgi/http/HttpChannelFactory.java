package ch.ethz.iks.r_osgi.http;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import ch.ethz.iks.r_osgi.ChannelEndpoint;
import ch.ethz.iks.r_osgi.RemoteOSGiMessage;
import ch.ethz.iks.r_osgi.NetworkChannel;
import ch.ethz.iks.r_osgi.NetworkChannelFactory;

/**
 * channel factory for HTTP transport.
 * 
 * @author Jan S. Rellermeyer, ETH Zurich
 */
final class HttpChannelFactory implements NetworkChannelFactory {

	static final String PROTOCOL = "http";

	/**
	 * get a new connection.
	 * 
	 * @param host
	 *            the host to connect to.
	 * @param port
	 *            the port to connect to.
	 * @param protocol
	 *            ignored.
	 * @return the transport channel.
	 * @see ch.ethz.iks.r_osgi.NetworkChannelFactory#getConnection(java.net.InetAddress,
	 *      int, java.lang.String)
	 */
	public NetworkChannel getConnection(final InetAddress host, final int port,
			final String protocol) throws IOException {
		return new HttpChannel(host, port);
	}

	/**
	 * bind an existing socket into a channel. Used to create a channel from an
	 * accepted socket connection.
	 * 
	 * @param socket
	 *            the socket.
	 * @return the wrapping transport channel.
	 * @throws IOException
	 *             in case of IO errors.
	 */
	public NetworkChannel bind(final Socket socket) throws IOException {
		return new HttpChannel(socket);
	}

	/**
	 * the inner class representing a channel with HTTP transport. Useful for
	 * accessing R-OSGi services inside of firewalled networks.
	 * 
	 * @author Jan S. Rellermeyer, ETH Zurich
	 */
	private static final class HttpChannel implements NetworkChannel {
		/**
		 * the socket.
		 */
		private Socket socket;

		/**
		 * the host address.
		 */
		private InetAddress host;

		/**
		 * the port.
		 */
		private int port;

		/**
		 * the input stream.
		 */
		private DataInputStream input;

		/**
		 * the output stream.
		 */
		private DataOutputStream output;

		/**
		 * the endpoint.
		 */
		private ChannelEndpoint endpoint;

		/**
		 * connected ?
		 */
		private boolean connected = true;

		/**
		 * create a new TCPChannel.
		 * 
		 * @param host
		 *            the host address.
		 * @param port
		 *            the port.
		 * @throws IOException
		 *             in case of IO errors.
		 */
		HttpChannel(final InetAddress host, final int port) throws IOException {
			this.host = host;
			this.port = port;
			open(new Socket(host, port));
		}

		/**
		 * create a new TCPChannel from an existing socket.
		 * 
		 * @param socket
		 *            the socket.
		 * @throws IOException
		 *             in case of IO errors.
		 */
		public HttpChannel(final Socket socket) throws IOException {
			this.host = socket.getInetAddress();
			this.port = socket.getPort();
			open(socket);
		}

		/**
		 * open the channel.
		 * 
		 * @param socket
		 *            the socket.
		 * @throws IOException
		 *             if something goes wrong.
		 */
		private void open(final Socket socket) throws IOException {
			System.out.println();
			System.out.println("REQUESTING HTTP CHANNEL TO " + host + ":"
					+ port);
			System.out.println();
			this.socket = socket;
			this.socket.setKeepAlive(true);
			this.output = new DataOutputStream(socket.getOutputStream());
			input = new DataInputStream(socket.getInputStream());
		}

		public void bind(ChannelEndpoint endpoint) throws IOException {
			if (this.endpoint != null) {
				throw new IOException("Channel already bound to endpoint.");
			}
			this.endpoint = endpoint;
		}

		/**
		 * reconnect the channel.
		 * 
		 * @throws IOException
		 *             if the connection attempt fails.
		 * @see ch.ethz.iks.r_osgi.NetworkChannel#reconnect()
		 */
		public void reconnect() throws IOException {
			open(new Socket(host, port));
			this.connected = true;
		}

		/**
		 * get the address of the channel endpoint.
		 * 
		 * @return the address of the channel endpoint.
		 * @see ch.ethz.iks.r_osgi.NetworkChannel#getInetAddress()
		 */
		public InetAddress getInetAddress() {
			return host;
		}

		/**
		 * get the port of the channel endpoint.
		 * 
		 * @return the port of the channel endpoint.
		 * @see ch.ethz.iks.r_osgi.NetworkChannel#getPort()
		 */
		public int getPort() {
			return port;
		}

		/**
		 * get the protocol that is implemented by the channel.
		 * 
		 * @return the protocol.
		 * @see ch.ethz.iks.r_osgi.NetworkChannel#getProtocol()
		 */
		public String getProtocol() {
			return PROTOCOL;
		}

		/**
		 * send a message through the channel.
		 * 
		 * @param message
		 *            the message.
		 * @throws IOException
		 *             in case of IO errors.
		 * @see ch.ethz.iks.r_osgi.NetworkChannel#sendMessage(ch.ethz.iks.r_osgi.RemoteOSGiMessage)
		 */
		public void sendMessage(final RemoteOSGiMessage message)
				throws IOException {
			reconnect();
			HttpRequest request = new HttpRequest("/r-osgi");
			message.send(request.getOutputStream());
			request.send(HttpRequest.POST, host.toString(), output);
			final HttpResponse resp = new HttpResponse(input);
			endpoint.receivedMessage(RemoteOSGiMessage.parse(resp
					.getInputStream()));
		}
	}

}
