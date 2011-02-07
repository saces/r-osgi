package ch.ethz.iks.r_osgi.test.exported;

import java.io.Serializable;

public class AccessPoint implements Serializable {
	private static final long serialVersionUID = 1L;
	private String _ssid = null;
	private String _bssid = null;
	private int _rssi = 0;

	public AccessPoint() {
		_ssid = "";
		_bssid = "";
		_rssi = 0;
	}

	public AccessPoint(String SSID, String BSSID, int RSSI) {
		_ssid = "";
		_bssid = "";
		_rssi = 0;
	}

	public String getSSID() {
		return _ssid;
	}

	public void setSSID(String SSID) {
		this._ssid = SSID;
	}

	public String getBSSID() {
		return _bssid;
	}

	public void setBSSID(String BSSID) {
		this._bssid = BSSID;
	}

	public int getRSSI() {
		return _rssi;
	}

	public void setRSSI(int RSSI) {
		this._rssi = RSSI;
	}
}
