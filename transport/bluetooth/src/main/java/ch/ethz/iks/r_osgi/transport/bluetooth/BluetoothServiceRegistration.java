package ch.ethz.iks.r_osgi.transport.bluetooth;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.Dictionary;

import javax.bluetooth.DataElement;
import javax.bluetooth.UUID;

import org.osgi.framework.Constants;

public class BluetoothServiceRegistration {

	private String[] interfaces;
	private Dictionary properties;

	BluetoothServiceRegistration(final String[] interfaces,
			final Dictionary properties) {
		this.interfaces = interfaces;
		this.properties = properties;
	}

	DataElement getInterfaceRecord() {
		final DataElement ifaceRecord = new DataElement(DataElement.DATSEQ);
		for (int i = 0; i < interfaces.length; i++) {
			ifaceRecord.addElement(new DataElement(DataElement.STRING,
					interfaces[i]));
		}
		return ifaceRecord;
	}

	/**
	 * 
	 * @return
	 */
	DataElement getPropertyRecord() {
		final DataElement propRecord = new DataElement(DataElement.DATSEQ);
		final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		try {
			final ObjectOutputStream out = new ObjectOutputStream(bytes);
			out.writeObject(properties);
			out.close();
			byte[] b = bytes.toByteArray();
			propRecord
					.addElement(new DataElement(DataElement.U_INT_4, b.length));
			for (int i = 0; i < b.length; i += 16) {
				propRecord.addElement(new DataElement(DataElement.U_INT_16,
						copyArray(b, i, 16)));
			}
			return propRecord;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	private byte[] copyArray(byte[] b, int offset, int length) {
		byte[] result = new byte[length];
		for (int i = 0; i < length; i++) {
			if (offset + i < b.length) {
				result[i] = b[offset + i];
			} else {
				result[i] = 0;
			}
		}
		return result;
	}

	public int getTimestamp() {
		return 1;
	}

	public long getServiceID() {
		return ((Long) properties.get(Constants.SERVICE_ID)).longValue();
	}

	public UUID[] getUUIDs() {
		final UUID[] uuids = new UUID[interfaces.length];
		for (int i = 0; i < interfaces.length; i++) {
			long h = interfaces[i].hashCode() + 0x7fffffff;
			uuids[i] = new UUID(h);
		}
		return uuids;
	}
}
