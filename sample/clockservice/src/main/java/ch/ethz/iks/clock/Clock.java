package ch.ethz.iks.clock;

public interface Clock {

	long getTime();

	Alarm[] getAlarms();

	void addAlarm(Alarm alarm);

	void removeAlarm(Alarm alarm);
}
