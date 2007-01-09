package ch.ethz.iks.r_osgi.http.acceptor;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

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

	protected void service(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		System.out.println();
		System.out.println("Servlet called");
		super.service(req, resp);
	}

	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		System.out.println("GOT GET REQUEST");
		try {
			new HttpBridge(req.getRemoteHost(), new DataInputStream(req
					.getInputStream()), new DataOutputStream(resp
					.getOutputStream()));

			while (!resp.isCommitted()) {
				Thread.sleep(60000);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
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
			new HttpBridge(req.getRemoteHost(), new DataInputStream(req
					.getInputStream()), new DataOutputStream(resp
					.getOutputStream()));
			resp.setStatus(HttpServletResponse.SC_OK);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
