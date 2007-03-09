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
package ch.ethz.iks.r_osgi.impl;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;

import org.osgi.service.log.LogService;

import ch.ethz.iks.r_osgi.ChannelEndpoint;
import ch.ethz.iks.r_osgi.RemoteOSGiMessage;
import ch.ethz.iks.r_osgi.NetworkChannel;
import ch.ethz.iks.r_osgi.NetworkChannelFactory;

/**
 * channel factory for (persistent) TCP transport. This is the default protocol.
 * 
 * @author Jan S. Rellermeyer, ETH Zurich
 * @since 0.6
 */
final class TCPChannelFactory implements NetworkChannelFactory {

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
	public NetworkChannel getConnection(final ChannelEndpoint endpoint,
			final InetAddress host, final int port, final String protocol)
			throws IOException {
		return new TCPChannel(endpoint, host, port);
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
		return new TCPChannel(endpoint, socket);
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
		TCPChannel(final ChannelEndpoint endpoint, final InetAddress host,
				final int port) throws IOException {
			this.host = host;
			this.port = port;
			this.endpoint = endpoint;
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
		public TCPChannel(final ChannelEndpoint endpoint, final Socket socket)
				throws IOException {
			this.host = socket.getInetAddress();
			this.port = socket.getPort();
			this.endpoint = endpoint;
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
			this.socket = socket;
			this.socket.setKeepAlive(true);
			this.output = new ObjectOutputStream(new BufferedOutputStream(
					socket.getOutputStream()));
			output.flush();
			input = new ObjectInputStream(new BufferedInputStream(socket
					.getInputStream()));
			new ReceiverThread().start();
		}

		/**
		 * get the String representation of the channel.
		 * 
		 * @return the ID. *
		 * @see java.lang.Object#toString()
		 */
		public String toString() {
			return "TCPChannel (" + getID() + ")";
		}

		/**
		 * reconnect the channel.
		 * 
		 * @throws IOException
		 *             if the connection attempt fails.
		 * @see ch.ethz.iks.r_osgi.NetworkChannel#reconnect()
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
			open(new Socket(host, port));
			this.connected = true;
			new ReceiverThread().start();
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
			return "r-osgi";
		}

		/**
		 * get the (unique) ID of the channel.
		 * 
		 * @return the ID.
		 * @see ch.ethz.iks.r_osgi.NetworkChannel#getID()
		 */
		public String getID() {
			return "r-osgi" + "://" + host.getHostAddress() + ":" + port;
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
				this.setName("TCPChannel:ReceiverThread:" + getID());
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
}
