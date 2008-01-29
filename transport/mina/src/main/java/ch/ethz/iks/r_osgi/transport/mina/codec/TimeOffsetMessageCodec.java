package ch.ethz.iks.r_osgi.transport.mina.codec;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.IoSession;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.apache.mina.filter.codec.demux.MessageDecoderResult;

import ch.ethz.iks.r_osgi.messages.RemoteOSGiMessage;
import ch.ethz.iks.r_osgi.messages.TimeOffsetMessage;

/**
 * <pre>
 *        0                   1                   2                   3
 *        0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 *       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *       |       R-OSGi header (function = TimeOffset = 7)               |
 *       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *       |                   Marshalled Long[]                           \
 *       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * </pre>.
 * 
 * @author rjan
 * 
 */
public class TimeOffsetMessageCodec extends RemoteOSGiMessageCodec {

	private static final Set MESSAGE_TYPE;

	static {
		MESSAGE_TYPE = Collections.unmodifiableSet(new HashSet(Arrays
				.asList(new Class[] { TimeOffsetMessage.class })));
	}

	public TimeOffsetMessageCodec() {
		super(RemoteOSGiMessage.TIME_OFFSET);
	}

	public MessageDecoderResult decodeBody(IoSession session, ByteBuffer in,
			ProtocolDecoderOutput out) throws Exception {
		final TimeOffsetMessage msg = new TimeOffsetMessage();
		msg.setXID(((Short) session.getAttribute("xid")).shortValue());
		msg.setTimeSeries((long[]) in.getObject());
		out.write(msg);
		return MessageDecoderResult.OK;
	}

	public void encodeBody(IoSession session, RemoteOSGiMessage message,
			ByteBuffer buf) {
		final TimeOffsetMessage msg = (TimeOffsetMessage) message;
		buf.putObject(msg.getTimeSeries());
	}

	public Set getMessageTypes() {
		return MESSAGE_TYPE;
	}

}
