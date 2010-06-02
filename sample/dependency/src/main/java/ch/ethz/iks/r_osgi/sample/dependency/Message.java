package ch.ethz.iks.r_osgi.sample.dependency;

public class Message {

	private String msg;
	
	public Message(String msg) {
		this.msg = msg;
	}
	
	public String toString() {
		return msg;
	}

	public String reverse() {
		return new StringBuffer().append(msg).reverse().toString();
	}
	
}