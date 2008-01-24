/* Copyright (c) 2006-2008 Jan S. Rellermeyer
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
package ch.ethz.iks.r_osgi.impl;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import org.osgi.service.log.LogService;
import ch.ethz.iks.r_osgi.URI;
import ch.ethz.iks.r_osgi.Remoting;
import ch.ethz.iks.r_osgi.channels.ChannelEndpoint;
import ch.ethz.iks.r_osgi.channels.NetworkChannel;
import ch.ethz.iks.r_osgi.channels.NetworkChannelFactory;
import ch.ethz.iks.r_osgi.messages.RemoteOSGiMessage;

/**
 * channel factory for (persistent) TCP transport. This is the default protocol.
 * 
 * @author Jan S. Rellermeyer, ETH Zurich
 * @since 0.6
 */
final class TCPChannelFactory implements NetworkChannelFactory {

	static final String PROTOCOL = "r-osgi";
	private Remoting remoting;
	private TCPThread thread;
	private URI listeningAddress;

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
	 * @see ch.ethz.iks.r_osgi.channels.NetworkChannelFactory#getConnection(java.net.InetAddress,
	 *      int, java.lang.String)
	 */
	public NetworkChannel getConnection(final ChannelEndpoint endpoint,
			final URI endpointURI) throws IOException {
		return new TCPChannel(endpoint, endpointURI);
	}

	/**
	 * bind the factory to the R-OSGi Remoting instance.
	 * 
	 * @param socket
	 *            the socket.
	 * @return the wrapping transport channel.
	 * @throws IOException
	 *             in case of IO errors.
	 */
	public void activate(final Remoting remoting) throws IOException {
		this.remoting = remoting;
		thread = new TCPThread();
		thread.start();
	}

	public void deactivate(final Remoting remoting) throws IOException {
		thread.interrupt();
		this.remoting = null;
	}

	public URI getListeningAddress(String protocol) {
		return listeningAddress;
	}

	/**
	 * the inner class representing a channel with TCP transport. The TCP
	 * connection uses the TCP keepAlive option to reduce reconnection overhead.
	 * 
	 * @author Jan S. Rellermeyer, ETH Zurich
	 */
	private static final class TCPChannel implements NetworkChannel {
		/**
		 * the socket.
		 */
		private Socket socket;

		/**
		 * the remote endpoint
		 */
		private URI remoteEndpointAddress;

		/**
		 * the local endpoint
		 */
		private URI localEndpointAddress;

		/**
		 * the input stream.
		 */
		private ObjectInputStream input;

		/**
		 * the output stream.
		 */
		private ObjectOutputStream output;

		/**
		 * the endpoint.
		 */
		private ChannelEndpoint endpoint;

		/**
		 * connected ?
		 */
		private boolean connected = true;

		/**
		 * create a new TCPChannel
		 * 
		 * @param endpoint
		 *            the channel endpoint.
		 * @param host
		 *            the host address.
		 * @param port
		 *            the port.
		 * @throws IOException
		 *             in case of IO errors.
		 */
		TCPChannel(final ChannelEndpoint endpoint, final URI endpointAddress)
				throws IOException {
			int port = endpointAddress.getPort();
			if (port == -1) {
				port = 9278;
			}
			this.endpoint = endpoint;
			this.remoteEndpointAddress = endpointAddress;
			open(new Socket(endpointAddress.getHostName(), port));
			new ReceiverThread().start();
		}

		/**
		 * create a new TCPChannel from an existing socket.
		 * 
		 * @param socket
		 *            the socket.
		 * @throws IOException
		 *             in case of IO errors.
		 */
		public TCPChannel(final Socket socket) throws IOException {
			this.remoteEndpointAddress = URI.create(getProtocol() + "://"
					+ socket.getInetAddress().getHostName() + ":"
					+ socket.getPort());
			open(socket);
		}

		public void bind(ChannelEndpoint endpoint) {
			this.endpoint = endpoint;
			new ReceiverThread().start();
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
			this.socket = socket;
			this.localEndpointAddress = URI.create(getProtocol() + "://"
					+ socket.getLocalAddress().getHostName() + ":"
					+ socket.getLocalPort());
			try {
				this.socket.setKeepAlive(true);
			} catch (Throwable t) {
				// for 1.2 VMs that do not support the setKeepAlive
			}
			this.socket.setTcpNoDelay(true);
			this.output = new ObjectOutputStream(new BufferedOutputStream(
					socket.getOutputStream()));
			output.flush();
			input = new ObjectInputStream(new BufferedInputStream(socket
					.getInputStream()));
		}

		/**
		 * get the String representation of the channel.
		 * 
		 * @return the ID. *
		 * @see java.lang.Object#toString()
		 */
		public String toString() {
			return "TCPChannel (" + getRemoteAddress() + ")";
		}

		/**
		 * reconnect the channel.
		 * 
		 * @throws IOException
		 *             if the connection attempt fails.
		 * @see ch.ethz.iks.r_osgi.channels.NetworkChannel#reconnect()
		 */
		public void reconnect() throws IOException {
			connected = false;
			try {
				if (socket != null) {
					socket.close();
				}
			} catch (Exception e) {
				socket = null;
			}

			open(new Socket(remoteEndpointAddress.getHostName(), remoteEndpointAddress
					.getPort()));
			this.connected = true;
			new ReceiverThread().start();
		}

		/**
		 * get the protocol that is implemented by the channel.
		 * 
		 * @return the protocol.
		 * @see ch.ethz.iks.r_osgi.channels.NetworkChannel#getProtocol()
		 */
		public String getProtocol() {
			return PROTOCOL;
		}

		/**
		 * get the (unique) ID of the channel.
		 * 
		 * @return the ID.
		 * @see ch.ethz.iks.r_osgi.channels.NetworkChannel#getRemoteURL()
		 */
		public URI getRemoteAddress() {
			return remoteEndpointAddress;
		}

		public URI getLocalAddress() {
			return localEndpointAddress;
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
			if (RemoteOSGiServiceImpl.MSG_DEBUG) {
				RemoteOSGiServiceImpl.log.log(LogService.LOG_DEBUG,
						"{TCP Channel} sending " + message);
			}
			message.send(output);
		}

		/**
		 * the receiver thread continuously tries to receive messages from the
		 * other endpoint.
		 * 
		 * @author Jan S. Rellermeyer, ETH Zurich
		 * @since 0.6
		 */
		private class ReceiverThread extends Thread {
			private ReceiverThread() {
				this.setName("TCPChannel:ReceiverThread:" + getRemoteAddress());
				this.setDaemon(true);
			}

			public void run() {
				while (connected) {
					try {
						final RemoteOSGiMessage msg = RemoteOSGiMessage
								.parse(input);
						if (RemoteOSGiServiceImpl.MSG_DEBUG) {
							RemoteOSGiServiceImpl.log.log(LogService.LOG_DEBUG,
									"{TCP Channel} received " + msg);
						}
						endpoint.receivedMessage(msg);
					} catch (Throwable t) {
						t.printStackTrace();
						connected = false;
						try {
							socket.close();
						} catch (IOException e1) {
						}
						endpoint.receivedMessage(null);
						return;
					}
				}
			}
		}
	}

	/**
	 * TCPThread, handles incoming tcp messages.
	 */
	private final class TCPThread extends Thread {
		/**
		 * the socket.
		 */
		private final ServerSocket socket;

		/**
		 * creates and starts a new TCPThread.
		 * 
		 * @throws IOException
		 *             if the server socket cannot be opened.
		 */
		private TCPThread() throws IOException {
			socket = new ServerSocket(RemoteOSGiServiceImpl.R_OSGI_PORT);
			listeningAddress = URI.create(PROTOCOL + "://"
					+ socket.getInetAddress().getHostName() + ":"
					+ socket.getLocalPort());
		}

		/**
		 * thread loop.
		 * 
		 * @see java.lang.Thread#run()
		 */
		public void run() {
			while (!isInterrupted()) {
				try {
					// accept incoming connections and build channel endpoints
					// for them
					remoting.createEndpoint(new TCPChannel(socket.accept()));
				} catch (IOException ioe) {
					ioe.printStackTrace();
				}
			}
		}
	}

}
