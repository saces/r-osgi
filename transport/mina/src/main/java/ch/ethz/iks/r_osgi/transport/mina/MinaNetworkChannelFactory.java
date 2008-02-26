package ch.ethz.iks.r_osgi.transport.mina;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

import org.apache.mina.common.ConnectFuture;
import org.apache.mina.common.IdleStatus;
import org.apache.mina.common.IoHandler;
import org.apache.mina.common.IoSession;
import org.apache.mina.filter.LoggingFilter;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.transport.socket.nio.SocketAcceptor;
import org.apache.mina.transport.socket.nio.SocketConnector;

import ch.ethz.iks.r_osgi.Remoting;
import ch.ethz.iks.r_osgi.URI;
import ch.ethz.iks.r_osgi.channels.ChannelEndpoint;
import ch.ethz.iks.r_osgi.channels.NetworkChannel;
import ch.ethz.iks.r_osgi.channels.NetworkChannelFactory;
import ch.ethz.iks.r_osgi.messages.RemoteOSGiMessage;
import ch.ethz.iks.r_osgi.transport.mina.codec.RemoteOSGiProtocolCodecFactory;

public class MinaNetworkChannelFactory implements NetworkChannelFactory {

	public static final String PROTOCOL_SCHEME = "r-osgi+mina";

	private SocketAcceptor acceptor;

	public void activate(final Remoting remoting) throws IOException {
		acceptor = new SocketAcceptor();
		acceptor.getFilterChain().addLast("logger", new LoggingFilter());
		acceptor.getFilterChain().addLast("protocol",
				new ProtocolCodecFilter(new RemoteOSGiProtocolCodecFactory()));
		final InetSocketAddress address = new InetSocketAddress(9279);
		acceptor.bind(address, new IoHandler() {

			private MinaNetworkChannel channel;

			public void sessionCreated(IoSession session) throws Exception {
				channel = new MinaNetworkChannel(session);
				remoting.createEndpoint(channel);
			}

			public void exceptionCaught(IoSession session, Throwable cause)
					throws Exception {
				channel.exceptionCaught(session, cause);
			}

			public void messageReceived(IoSession session, Object message)
					throws Exception {
				channel.messageReceived(session, message);
			}

			public void messageSent(IoSession session, Object message)
					throws Exception {
				channel.messageSent(session, message);
			}

			public void sessionClosed(IoSession session) throws Exception {
				channel.sessionClosed(session);
			}

			public void sessionIdle(IoSession session, IdleStatus status)
					throws Exception {
				channel.sessionIdle(session, status);
			}

			public void sessionOpened(IoSession session) throws Exception {
				channel.sessionOpened(session);
			}

		});
	}

	public void deactivate(Remoting remoting) throws IOException {

	}

	public NetworkChannel getConnection(ChannelEndpoint endpoint,
			URI endpointURI) throws IOException {
		return new MinaNetworkChannel(endpoint, endpointURI);
	}

	private class MinaNetworkChannel implements NetworkChannel, IoHandler {

		private URI remoteAddress;
		private ChannelEndpoint endpoint;
		private SocketConnector connector;
		private IoSession session;
		private URI localAddress;

		private MinaNetworkChannel(ChannelEndpoint endpoint, URI address) {
			this.remoteAddress = address;
			this.endpoint = endpoint;

			connector = new SocketConnector();
			connector.getFilterChain().addLast("logger", new LoggingFilter());
			connector.getFilterChain().addLast(
					"protocol",
					new ProtocolCodecFilter(
							new RemoteOSGiProtocolCodecFactory()));
			connect();
		}

		private MinaNetworkChannel(IoSession session) {

			this.session = session;
			this.remoteAddress = uriFromSocketAddress(session
					.getRemoteAddress());
			this.localAddress = uriFromSocketAddress(session.getLocalAddress());
		}

		private void connect() {
			final ConnectFuture connectFuture = connector.connect(
					new InetSocketAddress(remoteAddress.getHost(),
							remoteAddress.getPort()), this);
			/*
			 * connectFuture.addListener(new IoFutureListener() { public void
			 * operationComplete(IoFuture future) { session =
			 * future.getSession(); MinaNetworkChannel.this.localAddress =
			 * uriFromSocketAddress(session .getLocalAddress()); } });
			 */
			while (connectFuture.getSession() == null) {

			}
			session = connectFuture.getSession();
		}

		private URI uriFromSocketAddress(final SocketAddress s) {
			final InetSocketAddress a = (InetSocketAddress) s;
			return URI.create(PROTOCOL_SCHEME + "://" + a.getHostName() + ":"
					+ a.getPort());
		}

		public void bind(ChannelEndpoint endpoint) {
			this.endpoint = endpoint;
			// now receiving
		}

		public URI getLocalAddress() {
			return localAddress;
		}

		public String getProtocol() {
			return PROTOCOL_SCHEME;
		}

		public URI getRemoteAddress() {
			return remoteAddress;
		}

		public void sendMessage(RemoteOSGiMessage message) throws IOException {
			session.write(message);
		}

		/*
		 * 
		 * @see org.apache.mina.common.IoHandler#exceptionCaught(org.apache.mina.common.IoSession,
		 *      java.lang.Throwable)
		 */
		public void exceptionCaught(IoSession session, Throwable cause)
				throws Exception {
			// TODO Auto-generated method stub

		}

		/*
		 * 
		 * @see org.apache.mina.common.IoHandler#messageReceived(org.apache.mina.common.IoSession,
		 *      java.lang.Object)
		 */
		public void messageReceived(IoSession session, Object message)
				throws Exception {
			endpoint.receivedMessage((RemoteOSGiMessage) message);
		}

		/*
		 * 
		 * @see org.apache.mina.common.IoHandler#messageSent(org.apache.mina.common.IoSession,
		 *      java.lang.Object)
		 */
		public void messageSent(IoSession session, Object message)
				throws Exception {
			// TODO Auto-generated method stub

		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.apache.mina.common.IoHandler#sessionClosed(org.apache.mina.common.IoSession)
		 */
		public void sessionClosed(IoSession session) throws Exception {
			// TODO Auto-generated method stub

		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.apache.mina.common.IoHandler#sessionCreated(org.apache.mina.common.IoSession)
		 */
		public void sessionCreated(IoSession session) throws Exception {
			// TODO Auto-generated method stub

		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.apache.mina.common.IoHandler#sessionIdle(org.apache.mina.common.IoSession,
		 *      org.apache.mina.common.IdleStatus)
		 */
		public void sessionIdle(IoSession session, IdleStatus status)
				throws Exception {
			// TODO Auto-generated method stub

		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.apache.mina.common.IoHandler#sessionOpened(org.apache.mina.common.IoSession)
		 */
		public void sessionOpened(IoSession session) throws Exception {
			// TODO Auto-generated method stub

		}

		public void close() throws IOException {
			session.close();
		}

	}
}
