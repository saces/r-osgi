package ch.ethz.iks.r_osgi.transport.bluetooth;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Enumeration;

import javax.bluetooth.DataElement;
import javax.bluetooth.ServiceRecord;

public class BluetoothServiceRecord {

	private int offset;
	private long id;
	private String[] interfaces;
	private ServiceRecord serviceRecord;
	private int modTimestamp;
	private Dictionary properties;

	public BluetoothServiceRecord(ServiceRecord serviceRecord, int offset,
			long id, Enumeration ifaces) {
		this.serviceRecord = serviceRecord;
		this.offset = offset;
		this.id = id;
		final ArrayList ifaceList = new ArrayList();
		while (ifaces.hasMoreElements()) {
			ifaceList.add(((DataElement) ifaces.nextElement()).getValue());
		}
		this.interfaces = (String[]) ifaceList.toArray(new String[ifaceList
				.size()]);
	}

	public String toString() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("ServiceRecord{");
		buffer.append("id=");
		buffer.append(id);
		buffer.append(",interfaces=");
		buffer.append(Arrays.asList(interfaces));
		buffer.append("}");
		return buffer.toString();
	}

	private String toHex(int[] a) {
		StringBuffer buffer = new StringBuffer();
		for (int i = 0; i < a.length; i++) {
			buffer.append(Integer.toHexString(a[i]));
			buffer.append(" ");
		}
		return buffer.toString();
	}

	public Dictionary getProperties() throws IOException {
		serviceRecord.populateRecord(new int[] { (0x204 + 2 * offset) });
		int timestamp = (int) serviceRecord.getAttributeValue(
				0x204 + 2 * offset).getLong();
		if (properties == null || timestamp != modTimestamp) {
			// update the properties
			serviceRecord.populateRecord(new int[] { 0x205 + 2 * offset });
			DataElement seq = serviceRecord
					.getAttributeValue(0x205 + 2 * offset);
			Enumeration seqEnum = (Enumeration) seq.getValue();
			int count = (int) ((DataElement) seqEnum.nextElement()).getLong();
			final byte[] bytes = new byte[count];
			for (int i = 0; i < count; i += 16) {
				int len = i + 16 < count ? 16 : count % 16;
				byte[] fragment = (byte[]) ((DataElement) seqEnum.nextElement())
						.getValue();
				System.arraycopy(fragment, 0, bytes, i, len);
			}
			try {
				properties = (Dictionary) new ObjectInputStream(
						new ByteArrayInputStream(bytes)).readObject();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
			modTimestamp = timestamp;
		}
		return properties;
	}

}
