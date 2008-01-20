package ch.ethz.iks.clock;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Alarm implements Serializable {

	static final long serialVersionUID = 1L;

	private final static SimpleDateFormat tf = new SimpleDateFormat("MM-dd HH:mm:ss");

	private long time;

	private String notification;

	public Alarm(long time, String notification) {
		this.time = time;
		this.notification = notification;
	}

	public long getTime() {
		return time;
	}

	public String getNotification() {
		return notification;
	}

	public String toString() {
		return tf.format(new Date(time)) + " - " + notification;
	}

}
