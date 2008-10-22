package ch.ethz.iks.r_osgi.transport.mina.codec;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Set;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.IoSession;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.apache.mina.filter.codec.demux.MessageDecoderResult;

import ch.ethz.iks.r_osgi.messages.LeaseMessage;
import ch.ethz.iks.r_osgi.messages.RemoteOSGiMessage;

/**
 * <pre>
 *           0                   1                   2                   3
 *           0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 *          +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *          |       R-OSGi header (function = Lease = 1)                    |
 *          +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *          |  Array of service info (Fragment#, Interface[], properties    \
 *          +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *          |  Array of topic strings                                       \
 *          +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * </pre>
 * 
 * @author rjan
 * 
 */
public class LeaseMessageCodec extends RemoteOSGiMessageCodec {

	private static final Set MESSAGE_TYPE;

	static {
		MESSAGE_TYPE = Collections.unmodifiableSet(new HashSet(Arrays
				.asList(new Class[] { LeaseMessage.class })));
	}

	public LeaseMessageCodec() {
		super(RemoteOSGiMessage.LEASE);
	}

	public MessageDecoderResult decodeBody(IoSession session, ByteBuffer in,
			ProtocolDecoderOutput out) throws Exception {
		final LeaseMessage msg = new LeaseMessage();
		msg.setXID(((Integer) session.getAttribute("xid")).intValue());

		final short blocks = in.getShort();
		final String[] serviceIDs = new String[blocks];
		final String[][] serviceInterfaces = new String[blocks][];
		final Dictionary[] serviceProperties = new Dictionary[blocks];

		for (short i = 0; i < blocks; i++) {
			serviceIDs[i] = decodeString(in);
			serviceInterfaces[i] = decodeStringArray(in);
			serviceProperties[i] = (Dictionary) in.getObject();
		}

		msg.setServiceIDs(serviceIDs);
		msg.setServiceInterfaces(serviceInterfaces);
		msg.setServiceProperties(serviceProperties);
		msg.setTopics(decodeStringArray(in));
		out.write(msg);

		return MessageDecoderResult.OK;
	}

	public void encodeBody(IoSession session, RemoteOSGiMessage message,
			ByteBuffer buf) throws IOException {
		final LeaseMessage msg = (LeaseMessage) message;

		final String[] serviceIDs = msg.getServiceIDs();
		final String[][] serviceInterfaces = msg.getServiceInterfaces();
		final Dictionary[] serviceProperties = msg.getServiceProperties();

		final short blocks = (short) serviceIDs.length;
		buf.putShort(blocks);
		for (short i = 0; i < blocks; i++) {
			encodeString(buf, serviceIDs[i]);
			encodeStringArray(buf, serviceInterfaces[i]);
			buf.putObject(serviceProperties[i]);
		}
		encodeStringArray(buf, msg.getTopics());
	}

	public Set getMessageTypes() {
		return MESSAGE_TYPE;
	}

}
