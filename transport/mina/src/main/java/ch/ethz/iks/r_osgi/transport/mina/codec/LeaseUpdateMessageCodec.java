package ch.ethz.iks.r_osgi.transport.mina.codec;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.IoSession;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.apache.mina.filter.codec.demux.MessageDecoderResult;

import ch.ethz.iks.r_osgi.messages.LeaseUpdateMessage;
import ch.ethz.iks.r_osgi.messages.RemoteOSGiMessage;

/**
 * <pre>
 *     0                   1                   2                   3
 *     0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 *    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *    |       R-OSGi header (function = InvokeMsg = 3)                |
 *    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *    |   update type  |  length of &lt;url&gt;   |  &lt;url&gt; String  \
 *    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *    |   service information or url or topic array                      \
 *    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * </pre>.
 * 
 * @author rjan
 * 
 */
public class LeaseUpdateMessageCodec extends RemoteOSGiMessageCodec {

	private static final Set MESSAGE_TYPE;

	static {
		MESSAGE_TYPE = Collections.unmodifiableSet(new HashSet(Arrays
				.asList(new Class[] { LeaseUpdateMessage.class })));
	}

	public LeaseUpdateMessageCodec() {
		super(RemoteOSGiMessage.LEASE_UPDATE);
	}

	public MessageDecoderResult decodeBody(IoSession session, ByteBuffer in,
			ProtocolDecoderOutput out) throws Exception {
		final LeaseUpdateMessage msg = new LeaseUpdateMessage();
		msg.setXID(((Short) session.getAttribute("xid")).shortValue());

		msg.setType(in.getShort());
		msg.setServiceID(decodeString(in));
		msg.setPayload((Object[]) in.getObject());
		out.write(msg);
		return MessageDecoderResult.OK;
	}

	public void encodeBody(IoSession session, RemoteOSGiMessage message,
			ByteBuffer buf) throws IOException {
		final LeaseUpdateMessage msg = (LeaseUpdateMessage) message;
		buf.putShort(msg.getType());
		encodeString(buf, msg.getServiceID());
		buf.putObject(msg.getPayload());
	}

	public Set getMessageTypes() {
		return MESSAGE_TYPE;
	}

}
