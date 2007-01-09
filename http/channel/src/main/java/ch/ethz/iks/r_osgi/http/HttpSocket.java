package ch.ethz.iks.r_osgi.http;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;

public class HttpSocket {

	private Socket socket;

	private OutputStream out;

	private InputStream in;

	HttpSocket(InetAddress address, int port) throws IOException {
		socket = new Socket(address, port);		
		out = socket.getOutputStream();
		in = socket.getInputStream();
	}

	void post(HttpRequest req) throws IOException {
		out.write(req.getBytes(socket.getInetAddress().getHostName(), HttpRequest.POST));
		out.flush();
	}

	void get(HttpRequest req) throws IOException {
		out.write(req.getBytes(socket.getInetAddress().getHostName(), HttpRequest.GET));
		out.flush();
	}
	
	DataInput getInput() {
		return new DataInputStream(in);
	}

}
