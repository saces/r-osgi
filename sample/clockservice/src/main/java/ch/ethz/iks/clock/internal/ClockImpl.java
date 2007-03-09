package ch.ethz.iks.clock.internal;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

import ch.ethz.iks.clock.Alarm;
import ch.ethz.iks.clock.Clock;
import ch.ethz.iks.util.ScheduleListener;
import ch.ethz.iks.util.Scheduler;

class ClockImpl implements Clock, ScheduleListener {

	private final EventAdmin eventAdmin;

	private List alarms = new ArrayList();

	private final Scheduler scheduler;

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
		scheduler = new Scheduler(this);
	}

	public void addAlarm(Alarm alarm) {
		alarms.add(alarm);
		Dictionary properties = new Hashtable();
		properties.put("alarm", alarm);
		eventAdmin.postEvent(new Event("ch/ethz/iks/clock/ADD_ALARM",
				properties));
		System.out.println("ADDIND ALARM " + alarm.getTime());
		System.out.println("CURRENT TIME " + System.currentTimeMillis());
		scheduler.schedule(alarm, alarm.getTime());
	}

	public void removeAlarm(Alarm alarm) {
		alarms.remove(alarm);
		Dictionary properties = new Hashtable();
		properties.put("alarm", alarm);
		eventAdmin.postEvent(new Event("ch/ethz/iks/clock/DEL_ALARM",
				properties));
		scheduler.unschedule(alarm);
	}

	public Alarm[] getAlarms() {
		return (Alarm[]) alarms.toArray(new Alarm[alarms.size()]);
	}

	public long getTime() {
		return System.currentTimeMillis();
	}

	public void due(Scheduler s, long time, Object obj) {
		final Alarm alarm = (Alarm) obj;
		Dictionary properties = new Hashtable();
		properties.put("alarm", alarm);
		eventAdmin.postEvent(new Event("ch/ethz/iks/clock/ALARM", properties));
		removeAlarm(alarm);
	}

}
