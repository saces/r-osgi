package ch.ethz.iks.clock.internal;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Label;
import java.awt.List;
import java.awt.Panel;
import java.awt.TextField;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.osgi.framework.BundleContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;

import ch.ethz.iks.clock.Alarm;
import ch.ethz.iks.clock.Clock;
import ch.ethz.iks.r_osgi.types.ServiceUIComponent;

public class ClockUI extends Panel implements ServiceUIComponent {

	private Clock clock;

	private static final SimpleDateFormat hhmm = new SimpleDateFormat("HH:mm");

	private static final SimpleDateFormat ddmm = new SimpleDateFormat(
			"dd.MM. HH:mm");

	private static final SimpleDateFormat ddmmyy = new SimpleDateFormat(
			"dd.MM.yy HH:mm");

	private static final SimpleDateFormat full = new SimpleDateFormat(
			"dd.MM.yyyy HH:mm");

	private static final SimpleDateFormat[] patterns = new SimpleDateFormat[] {
			hhmm, ddmm, ddmmyy, full };

	private final static SimpleDateFormat shortTime = new SimpleDateFormat(
			"HH:mm:ss");

	private final Map entries = new HashMap(0);

	private final Label label;

	private final List alarmList;

	public static void main(String[] args) {
		java.awt.Frame frame = new java.awt.Frame();
		Dimension size = Toolkit.getDefaultToolkit().getScreenSize();
		int width = size.width < 250 ? size.width : 250;
		int heigth = size.height < 300 ? size.height - 10 : 300;
		frame.setSize(width, heigth);

		frame.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				System.exit(0);
			}
		});
		final ClockUI ui = new ClockUI();
		ui.initComponent(null, null);
		frame.add(ui);
		frame.show();
	}

	public ClockUI() {
		final GridBagLayout gbl = new GridBagLayout();
		setLayout(gbl);
		final GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		c.anchor = GridBagConstraints.CENTER;
		c.insets = new Insets(2, 2, 2, 2);
		c.gridx = 0;
		c.gridy = 0;
		c.gridwidth = 2;
		c.gridheight = 1;
		label = new Label("00:00:00");
		label.setFont(new Font("Arial", Font.BOLD, 45));
		gbl.setConstraints(label, c);
		add(label);

		alarmList = new List();

		c.gridx = 0;
		c.gridy = 1;
		c.gridwidth = 1;
		Label l = new Label("Time:");
		gbl.setConstraints(l, c);
		add(l);

		c.gridx = 1;
		c.gridy = 1;
		final TextField alarmTime = new TextField(8);
		gbl.setConstraints(alarmTime, c);
		add(alarmTime);

		c.gridx = 0;
		c.gridy = 2;
		l = new Label("Notification:");
		gbl.setConstraints(l, c);
		add(l);

		c.gridx = 1;
		c.gridy = 2;
		final TextField notification = new TextField(10);
		gbl.setConstraints(notification, c);
		add(notification);

		c.gridx = 0;
		c.gridy = 3;
		final Button add = new Button("Add");
		add.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					Alarm a = new Alarm(parse(alarmTime.getText()),
							notification.getText());
					clock.addAlarm(a);
				} catch (ParseException e1) {
					// here: show a box
					e1.printStackTrace();
				}
				alarmTime.setText("");
				notification.setText("");
			}
		});
		gbl.setConstraints(add, c);
		add(add);

		c.gridx = 0;
		c.gridy = 4;
		c.gridwidth = 2;
		gbl.setConstraints(alarmList, c);
		add(alarmList);

		c.gridx = 0;
		c.gridy = 5;
		c.gridwidth = 1;
		final Button remove = new Button("Remove");
		remove.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				final int pos = alarmList.getSelectedIndex();
				if (pos > -1) {
					final Alarm a = (Alarm) entries.get(alarmList.getItem(pos));
					clock.removeAlarm(a);
				}
			}
		});
		gbl.setConstraints(remove, c);
		add(remove);
	}

	public static long parse(String date) throws ParseException {
		Date d;
		for (int i = 0; i < 3; i++) {
			try {
				d = patterns[i].parse(date);
				Calendar c = Calendar.getInstance();
				switch (i) {
				case 0:
					if (d.getHours() < c.get(Calendar.HOUR_OF_DAY)) {
						c.roll(Calendar.DATE, true);
					}
					d.setDate(c.get(Calendar.DATE));
					d.setMonth(c.get(Calendar.MONTH));
				case 1:
					d.setYear(c.get(Calendar.YEAR) - 1900);
				case 2:
				case 3:
					return d.getTime();
				}
			} catch (ParseException pe) {
			}
		}

		throw new ParseException("Cannot parse " + date, 0);
	}

	public void initComponent(Object clockService, BundleContext context) {
		this.clock = (Clock) clockService;
		Dictionary properties = new Hashtable();
		properties.put(EventConstants.EVENT_TOPIC,
				new String[] { "ch/ethz/iks/clock/*" });
		context.registerService(EventHandler.class.getName(),
				new EventHandler() {
					public void handleEvent(Event event) {
						String topic = event.getTopic().intern();
						if (topic == "ch/ethz/iks/clock/TICK") {
							label.setText(shortTime.format(new Date(
									((Long) event.getProperty("time"))
											.longValue())));
						} else if (topic == "ch/ethz/iks/clock/ADD_ALARM") {
							final Alarm a = (Alarm) event.getProperty("alarm");
							final String s = a.toString();
							entries.put(s, a);
							alarmList.add(s);
						} else if (topic == "ch/ethz/iks/clock/DEL_ALARM") {
							final Alarm a = (Alarm) event.getProperty("alarm");
							final String s = a.toString();
							entries.put(s, a);
							alarmList.remove(s);
						} else if (topic == "ch/ethz/iks/clock/ALARM") {
							final Alarm a = (Alarm) event.getProperty("alarm");
							new Dialog(new Frame(), a.getNotification()) {
								public void show() {
									setLayout(new BorderLayout());
									
									add(BorderLayout.CENTER, new Label(a.toString()));
									final Button ok = new Button("OK");
									ok.addActionListener(new ActionListener() {
										public void actionPerformed(
												ActionEvent e) {
											setVisible(false);
										}
									});
									add(BorderLayout.SOUTH, ok);
									pack();
									super.show();
								}
							}.show();
						}
					}
				}, properties);
		final Alarm[] alarms = clock.getAlarms();
		for (int i = 0; i < alarms.length; i++) {
			final String s = alarms[i].toString();
			entries.put(s, alarms[i]);
			alarmList.add(s);
		}
	}

	public Panel getPanel() {
		return this;
	}

}
