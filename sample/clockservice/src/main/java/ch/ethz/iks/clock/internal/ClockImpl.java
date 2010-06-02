package ch.ethz.iks.clock.internal;

import java.util.Date;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

import ch.ethz.iks.clock.Alarm;
import ch.ethz.iks.clock.Clock;

class ClockImpl implements Clock {

	private final EventAdmin eventAdmin;

	private final Timer timer = new Timer();

	private Map alarms = new HashMap/* Alarm,AlarmTask */();

	ClockImpl(final EventAdmin eventAdmin) {
		this.eventAdmin = eventAdmin;
		final Dictionary properties = new Hashtable();

		new Thread() {
			public void run() {
				try {
					while (true) {
						properties.put("time", new Long(System
								.currentTimeMillis()));
						final Event event = new Event("ch/ethz/iks/clock/TICK",
								properties);
						eventAdmin.postEvent(event);
						sleep(1000);
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}.start();
	}

	public void addAlarm(final Alarm alarm) {
		final TimerTask alarmTask = new AlarmTask(alarm);
		alarms.put(alarm, alarmTask);
		Dictionary properties = new Hashtable();
		properties.put("alarm", alarm);
		eventAdmin.postEvent(new Event("ch/ethz/iks/clock/ADD_ALARM",
				properties));
		timer.schedule(alarmTask, new Date(alarm.getTime()));
	}

	public void removeAlarm(Alarm alarm) {
		Dictionary properties = new Hashtable();
		properties.put("alarm", alarm);
		eventAdmin.postEvent(new Event("ch/ethz/iks/clock/DEL_ALARM",
				properties));
		final AlarmTask task = (AlarmTask) alarms.remove(alarm);
		task.cancel();
	}

	public Alarm[] getAlarms() {
		return (Alarm[]) alarms.keySet().toArray(new Alarm[alarms.size()]);
	}

	public long getTime() {
		return System.currentTimeMillis();
	}

	class AlarmTask extends TimerTask {

		final Event event;

		AlarmTask(final Alarm alarm) {
			final Dictionary properties = new Hashtable();
			properties.put("alarm", alarm);
			event = new Event("ch/ethz/iks/clock/ALARM", properties);
		}

		public void run() {
			eventAdmin.postEvent(event);
		}

	}

}
