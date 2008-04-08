package ch.ethz.iks.r_osgi.transport.bluetooth;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import javax.bluetooth.DataElement;
import javax.bluetooth.DeviceClass;
import javax.bluetooth.DiscoveryAgent;
import javax.bluetooth.DiscoveryListener;
import javax.bluetooth.LocalDevice;
import javax.bluetooth.RemoteDevice;
import javax.bluetooth.ServiceRecord;
import javax.bluetooth.ServiceRegistrationException;
import javax.bluetooth.UUID;
import javax.microedition.io.Connector;
import javax.microedition.io.StreamConnection;
import javax.microedition.io.StreamConnectionNotifier;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import ch.ethz.iks.r_osgi.URI;
import ch.ethz.iks.r_osgi.messages.RemoteOSGiMessage;
import ch.ethz.iks.r_osgi.Remoting;
import ch.ethz.iks.r_osgi.channels.ChannelEndpoint;
import ch.ethz.iks.r_osgi.channels.NetworkChannel;
import ch.ethz.iks.r_osgi.channels.NetworkChannelFactory;
import ch.ethz.iks.r_osgi.service_discovery.ServiceDiscoveryHandler;

public class BluetoothNetworkChannelFactory implements NetworkChannelFactory,
		ServiceDiscoveryHandler {

	public static final String PROTOCOL = "btspp";
	private URI listeningAddress;
	private Remoting remoting;
	private LocalDevice local;
	private ServiceRecord record;
	private StreamConnectionNotifier service;
	private DiscoveryAgent discovery;
	private BluetoothThread thread;
	private Listener listener = new Listener();

	private int modTimestamp;
	private HashMap serviceMap = new HashMap();
	private ArrayList services = new ArrayList();

	private HashMap remoteServices = new HashMap();

	BluetoothNetworkChannelFactory() throws IOException {
		local = LocalDevice.getLocalDevice();
		local.setDiscoverable(DiscoveryAgent.GIAC);
		discovery = local.getDiscoveryAgent();
		service = (StreamConnectionNotifier) Connector.open("btspp://"
				+ "localhost:9" + ";name=" + local.getFriendlyName());

		// TODO: remove debug output
		System.out.println("BT Listening at: " + local.getBluetoothAddress());

		record = local.getRecord(service);
		record.setAttributeValue(0x0003, new DataElement(DataElement.UUID,
				new UUID(9278)));
	}

	public void activate(Remoting remoting) throws IOException {
		this.remoting = remoting;
		this.thread = new BluetoothThread();

		// TODO: remove debug output
		System.out.println("BT ACTIVATED");
		thread.start();
		// new DiscoveryThread().start();
	}

	public void deactivate(Remoting remoting) throws IOException {
		thread.interrupt();
		this.remoting = null;
	}

	public URI getURI() {
		// TODO: implement
		return null;
	}

	public NetworkChannel getConnection(ChannelEndpoint endpoint,
			URI endpointURI) throws IOException {
		return new BluetoothNetworkChannel(endpoint, endpointURI);
	}

	private class BluetoothNetworkChannel implements NetworkChannel {

		private URI remoteEndpointURI;

		private URI localEndpointURI;

		private StreamConnection con;

		private ObjectInputStream input;

		private ObjectOutputStream output;

		private ChannelEndpoint endpoint;

		private boolean connected = true;

		private BluetoothNetworkChannel(ChannelEndpoint endpoint,
				final URI remoteEndpointURI) throws IOException {
			this.remoteEndpointURI = remoteEndpointURI;
			System.out.println("##############################TRYING TO CONNECT TO " + remoteEndpointURI);
			this.con = (StreamConnection) Connector.open(remoteEndpointURI
					.toString());
			System.out.println("##############################HAVING CON " + con);
			this.endpoint = endpoint;
			open();
			new ReceiverThread().start();
		}

		private BluetoothNetworkChannel(StreamConnection con,
				URI remoteEndpointURI) throws IOException {
			this.remoteEndpointURI = remoteEndpointURI;
			this.con = con;
			open();
		}

		/**
		 * open the channel.
		 * 
		 * @param socket
		 *            the socket.
		 * @throws IOException
		 *             if something goes wrong.
		 */
		private void open() throws IOException {
			this.localEndpointURI = URI.create(getProtocol() + "://"
					+ local.getBluetoothAddress() + ":9");
			this.output = new ObjectOutputStream(new BufferedOutputStream(con
					.openOutputStream()));
			output.flush();
			input = new ObjectInputStream(new BufferedInputStream(con
					.openInputStream()));
			System.out.println("ENDPOINT OPEN");
		}

		public String getProtocol() {
			return PROTOCOL;
		}

		public void close() throws IOException {
			service.close();
			thread.interrupt();
		}

		public void sendMessage(RemoteOSGiMessage message) throws IOException {
			message.send(output);
		}

		public URI getLocalAddress() {
			return localEndpointURI;
		}

		public URI getRemoteAddress() {
			return remoteEndpointURI;
		}

		public void bind(ChannelEndpoint endpoint) {
			this.endpoint = endpoint;
			new ReceiverThread().start();
		}

		/**
		 * the receiver thread continuously tries to receive messages from the
		 * other endpoint.
		 * 
		 * @author Jan S. Rellermeyer, ETH Zurich
		 * @since 0.6
		 */
		private class ReceiverThread extends Thread {

			private ReceiverThread() {
				this.setName("BTChannel:ReceiverThread:" + getRemoteAddress());
				this.setDaemon(true);
			}

			public void run() {
				while (connected) {
					try {
						final RemoteOSGiMessage msg = RemoteOSGiMessage
								.parse(input);
						endpoint.receivedMessage(msg);
					} catch (Throwable t) {
						// TODO: remove debug output
						t.printStackTrace();
						connected = false;
						try {
							con.close();
						} catch (IOException e1) {
						}
						endpoint.receivedMessage(null);
						return;
					}
				}
			}
		}

	}

	private class BluetoothThread extends Thread {

		public void run() {
			while (!isInterrupted()) {
				try {
					final StreamConnection con = service.acceptAndOpen();
					final RemoteDevice remote = RemoteDevice
							.getRemoteDevice(con);
					final String listeningAddress = PROTOCOL + "://"
							+ remote.getBluetoothAddress() + ":9";
					BluetoothNetworkChannelFactory.this.listeningAddress = URI
							.create(listeningAddress);
					final URI uri = URI.create(listeningAddress);
					remoting.createEndpoint(new BluetoothNetworkChannel(con,
							uri));
				} catch (IOException ioe) {
					// TODO: to log
					ioe.printStackTrace();
				}
			}
		}

	}

	public void registerService(ServiceReference ref, Dictionary properties,
			URI uri) {
		System.out.println("NEW SERVICE " + uri);
		final String[] interfaces = (String[]) ref
				.getProperty(Constants.OBJECTCLASS);
		BluetoothServiceRegistration reg = new BluetoothServiceRegistration(
				interfaces, properties);
		serviceMap.put(ref, reg);
		services.add(reg);
		try {
			updateSDPRecord();
		} catch (ServiceRegistrationException e) {
			e.printStackTrace();
		}
	}

	public void unregisterService(ServiceReference ref) {
		// final String[] interfaces = (String[]) ref
		// .getProperty(Constants.OBJECTCLASS);
		services.remove(serviceMap.remove(ref));
		try {
			updateSDPRecord();
		} catch (ServiceRegistrationException e) {
			e.printStackTrace();
		}
	}

	private DataElement[] createRecord() {
		final ArrayList records = new ArrayList();
		DataElement uuidRecord = new DataElement(DataElement.DATALT);
		DataElement idRecord = new DataElement(DataElement.DATALT);
		DataElement interfaceRecord = new DataElement(DataElement.DATALT);
		// 0x0200
		records.add(uuidRecord);
		// 0x0201
		records.add(idRecord);
		// 0x0202
		records.add(interfaceRecord);
		// 0x0203
		records.add(new DataElement(DataElement.INT_4, services.hashCode()));
		final BluetoothServiceRegistration[] regs = (BluetoothServiceRegistration[]) services
				.toArray(new BluetoothServiceRegistration[services.size()]);
		final HashSet seen = new HashSet();
		for (int i = 0; i < regs.length; i++) {
			idRecord.addElement(new DataElement(DataElement.INT_8, regs[i]
					.getServiceID()));
			UUID[] uuids = regs[i].getUUIDs();
			for (int j = 0; j < uuids.length; j++) {
				if (!seen.contains(uuids[j])) {
					uuidRecord.addElement(new DataElement(DataElement.UUID,
							uuids[j]));
				}
			}
			interfaceRecord.addElement(regs[i].getInterfaceRecord());
			records.add(new DataElement(DataElement.INT_4, regs[i]
					.getTimestamp()));
			records.add(regs[i].getPropertyRecord());
		}
		return (DataElement[]) records.toArray(new DataElement[records.size()]);
	}

	private void updateSDPRecord() throws ServiceRegistrationException {
		final DataElement[] records = createRecord();
		final int id = 0x0200;
		for (int i = 0; i < records.length; i++) {
			record.setAttributeValue(id + i, records[i]);
			System.out.println("SETTING RECORD " + Integer.toHexString(id + i)
					+ " WITH " + records[i]);
		}
		local.updateRecord(record);
	}

	private void search() {
		try {
			discovery.startInquiry(DiscoveryAgent.GIAC, listener);
			synchronized (listener) {
				listener.wait();
			}
			RemoteDevice[] devices = listener.getDevices();

			if (devices.length == 0) {
				System.out.println("NOTHING FOUND...");
			}

			for (int i = 0; i < devices.length; i++) {
				 discovery.searchServices(new int[] { 0x0003 },
						new UUID[] { new UUID(9278) }, devices[i], listener);
//				discovery.searchServices(null, new UUID[] { new UUID(9278) }, devices[i], listener);
				synchronized (listener) {
					listener.wait();
				}
				ServiceRecord[] records = listener.getRecords();
				for (int j = 0; j < records.length; j++) {
					final URI uri = URI.create(records[j].getConnectionURL(
							ServiceRecord.NOAUTHENTICATE_NOENCRYPT, false));
					System.out.println("FOUND " + uri);
					ArrayList srvs = (ArrayList) remoteServices.get(uri);
					if (srvs == null) {
						// we don't have any service information from this peer
						// get the ids and the interfaces
						srvs = new ArrayList();
						records[j].populateRecord(new int[] { 0x201, 0x0202,
								0x203 });
						// TODO: per URI
						modTimestamp = (int) records[j]
								.getAttributeValue(0x203).getLong();
						DataElement idRecord = records[j]
								.getAttributeValue(0x0201);
						DataElement ifaceRecord = records[j]
								.getAttributeValue(0x0202);
						if (idRecord != null) {
							Enumeration idEnum = (Enumeration) idRecord
									.getValue();
							Enumeration ifaceEnum = (Enumeration) ifaceRecord
									.getValue();
							int c = 0;
							while (idEnum.hasMoreElements()) {
								srvs.add(new BluetoothServiceRecord(records[j],
										c, ((DataElement) idEnum.nextElement())
												.getLong(),
										(Enumeration) ((DataElement) ifaceEnum
												.nextElement()).getValue()));
								c++;
							}
							remoteServices.put(uri, srvs);
							System.out.println("REMOTE SERVICES " + srvs);
						}
						System.out.println(((BluetoothServiceRecord) srvs
								.get(0)).getProperties());
					} else {
						records[j].populateRecord(new int[] { 0x0203 });
						int ts = (int) records[j].getAttributeValue(0x203)
								.getLong();
						if (ts != modTimestamp) {
							System.out.println("MODIFICATION DETECTED");
						}

						// TODO: calculate hash over the items.
						// refresh if necessary
						System.out.println("REMOTE SERVICES " + srvs);
						System.out.println(((BluetoothServiceRecord) srvs
								.get(0)).getProperties());
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private class Listener implements DiscoveryListener {

		private ArrayList devices = new ArrayList();

		private ArrayList services = new ArrayList();

		public void deviceDiscovered(RemoteDevice btDevice, DeviceClass cod) {
			try {
				System.out.println("FOUND DEVICE ... "
						+ btDevice.getFriendlyName(false) + " - " + btDevice.getBluetoothAddress());
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
			devices.add(btDevice);
		}

		public void inquiryCompleted(int discType) {
			synchronized (this) {
				notifyAll();
			}
		}

		RemoteDevice[] getDevices() {
			RemoteDevice[] result = (RemoteDevice[]) devices
					.toArray(new RemoteDevice[devices.size()]);
			devices.clear();
			return result;
		}

		public void servicesDiscovered(int transID, ServiceRecord[] servRecord) {
			System.out.println("FOUND SERVICE RECORDs " + servRecord.length);
			services.addAll(Arrays.asList(servRecord));
		}

		public void serviceSearchCompleted(int transID, int respCode) {
			System.out.println("NO MORE SERVICES");
			synchronized (this) {
				notifyAll();
			}
		}

		ServiceRecord[] getRecords() {
			ServiceRecord[] result = (ServiceRecord[]) services
					.toArray(new ServiceRecord[services.size()]);
			services.clear();
			return result;
		}
	}

	private class DiscoveryThread extends Thread {
		public void run() {
			try {
				while (!isInterrupted()) {
					synchronized (this) {
						search();
						wait(20000);
					}
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	public URI getListeningAddress(String protocol) {
		return listeningAddress;
	}
}
