package ch.ethz.iks.r_osgi.http.acceptor;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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

	private static ObjectInputStream localIn;

	private static ObjectOutputStream localOut;

	private static Socket socket;

	static {
		try {
			System.out.println("now opening local socket");
			socket = new Socket("localhost", R_OSGi_PORT);
			localIn = new ObjectInputStream(new BufferedInputStream(socket
					.getInputStream()));
			localOut = new ObjectOutputStream(new BufferedOutputStream(socket
					.getOutputStream()));
			localOut.flush();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}

	}

	protected void service(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		System.out.println();
		System.out.println("Servlet called");
		super.service(req, resp);
	}

	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		System.out.println("GOT GET REQUEST");
	}

	/**
	 * 
	 * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest,
	 *      javax.servlet.http.HttpServletResponse)
	 */
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		System.out.println("GOT POST REQUEST");
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

			for (; localIn.available() == 0 && !socket.isInputShutdown(); Thread
					.sleep(100L)) {
			}
			System.out
					.println("NOW sending back (" + localIn.available() + ")");
			int available = localIn.available();
			byte buffer[] = new byte[1024];
			int len;
			for (; available > 0
					&& (len = localIn.read(buffer, 0, available >= 1024 ? 1024
							: available)) > -1; available = localIn.available()) {
				remoteOut.write(buffer, 0, len);
			}
			System.out.println("finished sending back");
			resp.setStatus(HttpServletResponse.SC_OK);
			resp.flushBuffer();
		} catch (Throwable t) {
			System.err.println("oops, caught an exception.");
			t.printStackTrace();
		}
	}
}
