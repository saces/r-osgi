package ch.ethz.iks.r_osgi.http.acceptor;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.HashMap;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import ch.ethz.iks.r_osgi.RemoteOSGiMessage;

/**
 * 
 * @author Jan S. Rellermeyer, ETH Zurich
 */
public class HttpAcceptorServlet extends HttpServlet {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static final int R_OSGi_PORT = 9278;

	private static Socket socket;

	private static HashMap bridges = new HashMap();

	protected void service(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		System.out.println();
		System.out.println("Servlet called");
		super.service(req, resp);
	}

	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		// System.out.println("GOT GET REQUEST");
		// Writer writer = resp.getWriter();
		// writer.write("<h1>R-OSGi HTTP Channel Acceptor Servlet</h1>");
		// resp.setStatus(HttpServletResponse.SC_OK);
		doPost(req, resp);
	}

	/**
	 * 
	 * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest,
	 *      javax.servlet.http.HttpServletResponse)
	 */
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {

		System.out.println("GOT POST REQUEST FROM " + req.getRemoteAddr());

		final String host = req.getRemoteAddr();

		ChannelBridge bridge = (ChannelBridge) bridges.get(host);
		if (bridge == null) {
			bridge = new ChannelBridge();
			bridges.put(host, bridge);
		}

		bridge.forwardRequest(req, resp);
	}

	private static class ChannelBridge extends Thread {
		private final ObjectInputStream localIn;

		private final ObjectOutputStream localOut;

		private ChannelBridge() throws IOException {
			System.out.println("now opening local socket");
			socket = new Socket("localhost", R_OSGi_PORT);
			localIn = new ObjectInputStream(new BufferedInputStream(socket
					.getInputStream()));

			localOut = new ObjectOutputStream(new BufferedOutputStream(socket
					.getOutputStream()));
			localOut.flush();
		}

		private void forwardRequest(HttpServletRequest req,
				HttpServletResponse resp) throws IOException {
			ObjectInputStream remoteIn = new ObjectInputStream(req
					.getInputStream());
			ObjectOutputStream remoteOut = new ObjectOutputStream(resp
					.getOutputStream());

			System.out.println("Expecting " + remoteIn.available()
					+ " bytes of content");

			RemoteOSGiMessage msg = RemoteOSGiMessage.parse(remoteIn);
			System.out.println("{REMOTE -> LOCAL}: " + msg);
			System.out.println(msg.getClass().getName());
			if (msg.getFuncID() == RemoteOSGiMessage.LEASE) {
				System.out.println();
				System.out.println("YYYYYYY DETECTED LEASE YYYYYYY");
				System.out.println();
			}

			msg.send(localOut);

			msg = RemoteOSGiMessage.parse(localIn);
			System.out.println("{LOCAL -> REMOTE}: " + msg);
			msg.send(remoteOut);
			remoteOut.flush();
		}
	}

}
