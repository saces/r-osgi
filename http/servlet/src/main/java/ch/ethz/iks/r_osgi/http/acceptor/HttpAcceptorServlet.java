package ch.ethz.iks.r_osgi.http.acceptor;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
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

	private static HashMap channelInputs = new HashMap();

	private static HashMap channelOutputs = new HashMap();

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

	static void openChannel(final String host) {
		try {
			System.out.println("now opening local socket");
			socket = new Socket("localhost", R_OSGi_PORT);
			final ObjectInputStream localIn = new ObjectInputStream(
					new BufferedInputStream(socket.getInputStream()));

			final ObjectOutputStream localOut = new ObjectOutputStream(
					new BufferedOutputStream(socket.getOutputStream()));
			localOut.flush();
			channelInputs.put(host, localIn);
			channelOutputs.put(host, localOut);
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
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
		ObjectInputStream localIn = (ObjectInputStream) channelInputs.get(host);
		if (localIn == null) {
			openChannel(host);
			localIn = (ObjectInputStream) channelInputs
					.get(req.getRemoteAddr());
		}
		ObjectOutputStream localOut = (ObjectOutputStream) channelOutputs
				.get(host);

		try {
			System.out.println("Expecting " + req.getContentLength()
					+ " bytes of content");
			DataInputStream remoteIn = new DataInputStream(req.getInputStream());
			DataOutputStream remoteOut = new DataOutputStream(resp
					.getOutputStream());

			System.out.println("remotein available: " + remoteIn.available());

			byte content[] = new byte[req.getContentLength()];
			remoteIn.readFully(content);
			localOut.write(content);
			localOut.flush();

			System.out
					.println("NOW sending back (" + localIn.available() + ")");
			final ObjectOutputStream oout = new ObjectOutputStream(remoteOut);

			RemoteOSGiMessage.parse(localIn).send(oout);
			oout.flush();

			System.out.println("finished sending back");
			remoteOut.flush();
			resp.setStatus(HttpServletResponse.SC_OK);
		} catch (Throwable t) {
			System.err.println("oops, caught an exception.");
			t.printStackTrace();
		}
	}
}
