/* Copyright (c) 2006-2007 Jan S. Rellermeyer
 * Information and Communication Systems Research Group (IKS),
 * Department of Computer Science, ETH Zurich.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *    - Redistributions of source code must retain the above copyright notice,
 *      this list of conditions and the following disclaimer.
 *    - Redistributions in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in the
 *      documentation and/or other materials provided with the distribution.
 *    - Neither the name of ETH Zurich nor the names of its contributors may be
 *      used to endorse or promote products derived from this software without
 *      specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package ch.ethz.iks.r_osgi.http;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URL;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;

import ch.ethz.iks.r_osgi.RemoteOSGiMessage;
import ch.ethz.iks.r_osgi.channels.ChannelEndpoint;
import ch.ethz.iks.r_osgi.channels.NetworkChannel;
import ch.ethz.iks.r_osgi.channels.NetworkChannelFactory;

/**
 * channel factory for HTTP transport.
 * 
 * @author Jan S. Rellermeyer, ETH Zurich
 */
final class HttpChannelFactory implements NetworkChannelFactory {

	/**
	 * 
	 */
	static final String[] PROTOCOLS = { "http", "https" };

	static {
		HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {

			public boolean verify(String hostname, SSLSession session) {
				return true;
			}

		});
	}

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
	 * @throws IOException
	 *             if something goes wrong.
	 * @see ch.ethz.iks.r_osgi.channels.NetworkChannelFactory#getConnection(java.net.InetAddress,
	 *      int, java.lang.String)
	 */
	public NetworkChannel getConnection(final ChannelEndpoint endpoint,
			final InetAddress host, final int port, final String protocol)
			throws IOException {
		return new HttpChannel(endpoint, host, port, protocol);
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
	public NetworkChannel bind(final ChannelEndpoint endpoint,
			final Socket socket) throws IOException {
		return new HttpChannel(endpoint, socket);
	}

	/**
	 * the inner class representing a channel with HTTP transport. Useful for
	 * accessing R-OSGi services inside of firewalled networks.
	 * 
	 * @author Jan S. Rellermeyer, ETH Zurich
	 */
	private static final class HttpChannel implements NetworkChannel {
		/**
		 * the url.
		 */
		private URL url;

		/**
		 * the host address.
		 */
		private InetAddress host;

		/**
		 * the port.
		 */
		private int port;

		/**
		 * the protocol which is used for this channel instance. Either http or
		 * https
		 */
		private String protocol;

		/**
		 * the endpoint.
		 */
		private ChannelEndpoint endpoint;

		private String channelUID; 
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
		HttpChannel(final ChannelEndpoint endpoint, final InetAddress host,
				final int port, final String protocol) throws IOException {
			this.host = host;
			this.port = port;
			this.protocol = protocol;
			this.endpoint = endpoint;
			this.channelUID = new Double(Math.random()).toString();
			init();
		}

		/**
		 * create a new TCPChannel from an existing socket.
		 * 
		 * @param socket
		 *            the socket.
		 * @throws IOException
		 *             in case of IO errors.
		 */
		public HttpChannel(final ChannelEndpoint endpoint, final Socket socket)
				throws IOException {
			this.host = socket.getInetAddress();
			this.port = socket.getPort();
			this.endpoint = endpoint;
			init();
		}

		/**
		 * initialize the channel by setting a new URL.
		 * 
		 * @throws IOException
		 *             if something goes wrong.
		 */
		private void init() throws IOException {
			url = new URL(protocol, host.getHostName(), port, "/r-osgi");
		}

		/**
		 * reconnect the channel.
		 * 
		 * @throws IOException
		 *             if the connection attempt fails.
		 * @see ch.ethz.iks.r_osgi.channels.NetworkChannel#reconnect()
		 */
		public void reconnect() throws IOException {
			init();
		}

		/**
		 * get the address of the channel endpoint.
		 * 
		 * @return the address of the channel endpoint.
		 * @see ch.ethz.iks.r_osgi.channels.NetworkChannel#getInetAddress()
		 */
		public InetAddress getInetAddress() {
			return host;
		}

		/**
		 * get the port of the channel endpoint.
		 * 
		 * @return the port of the channel endpoint.
		 * @see ch.ethz.iks.r_osgi.channels.NetworkChannel#getPort()
		 */
		public int getPort() {
			return port;
		}

		/**
		 * get the protocol that is implemented by the channel.
		 * 
		 * @return the protocol.
		 * @see ch.ethz.iks.r_osgi.channels.NetworkChannel#getProtocol()
		 */
		public String getProtocol() {
			return protocol;
		}

		/**
		 * get the (unique) ID of the channel.
		 * 
		 * @return the ID.
		 * @see ch.ethz.iks.r_osgi.channels.NetworkChannel#getID()
		 */
		public String getID() {
			return protocol + "://" + host.getHostAddress() + ":" + port;
		}

		/**
		 * send a message through the channel.
		 * 
		 * @param message
		 *            the message.
		 * @throws IOException
		 *             in case of IO errors.
		 * @see ch.ethz.iks.r_osgi.channels.NetworkChannel#sendMessage(ch.ethz.iks.r_osgi.RemoteOSGiMessage)
		 */
		public void sendMessage(final RemoteOSGiMessage message)
				throws IOException {
			// open a new connection
			HttpURLConnection connection = (HttpURLConnection) url
					.openConnection();

			connection.setRequestMethod("POST");
			connection.setRequestProperty("Content-Type",
					"application/x-r_osgi");
			connection.setRequestProperty("channel", channelUID);
			connection.setUseCaches(false);
			connection.setDoInput(true);
			connection.setDoOutput(true);

			// TODO: bind to the log service and to a debug property
			// System.out.println("{HTTP Channel} sending " + message);
			message.send(new ObjectOutputStream(connection.getOutputStream()));

			final ObjectInputStream in = new ObjectInputStream(connection
					.getInputStream());

			if (message.getFuncID() == RemoteOSGiMessage.LEASE) {
				new CallbackThread(in).start();
				return;
			}

			final RemoteOSGiMessage msg = RemoteOSGiMessage.parse(in);
			// TODO: bind to the log service and to a debug property
			// System.out.println("{HTTP Channel} received " + msg);
			endpoint.receivedMessage(msg);
		}

		private class CallbackThread extends Thread {
			private ObjectInputStream input;

			private CallbackThread(final ObjectInputStream in) {
				this.input = in;
				this.setDaemon(true);
				this.setName("HTTPChannel:CallbackThread:" + getID());
			}

			public void run() {
				try {
					while (!Thread.interrupted()) {
						RemoteOSGiMessage msg = RemoteOSGiMessage.parse(input);
						// TODO: bind to the log service and to a debug property
						// System.out
						// .println("{HTTP Channel} asynchronously received "
						// + msg);
						endpoint.receivedMessage(msg);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}
}
