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

import ch.ethz.iks.r_osgi.messages.RemoteEventMessage;
import ch.ethz.iks.r_osgi.messages.RemoteOSGiMessage;

/**
 * <pre>
 *          0                   1                   2                   3
 *          0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 *         +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *         |       R-OSGi header (function = RemoteEvent = 5)              |
 *         +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *         | length of &lt;topic&gt;           |   &lt;topic&gt; String                \
 *         +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *         | Properties Dictionary MarshalledObject                        \
 *         +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * </pre>.
 * 
 * @author rjan
 * 
 */
public class RemoteEventMessageCodec extends RemoteOSGiMessageCodec {

	private static final Set MESSAGE_TYPE;

	static {
		MESSAGE_TYPE = Collections.unmodifiableSet(new HashSet(Arrays
				.asList(new Class[] { RemoteEventMessage.class })));
	}

	public RemoteEventMessageCodec() {
		super(RemoteOSGiMessage.REMOTE_EVENT);
	}

	public MessageDecoderResult decodeBody(IoSession session, ByteBuffer in,
			ProtocolDecoderOutput out) throws Exception {
		final RemoteEventMessage msg = new RemoteEventMessage();
		msg.setXID(((Integer) session.getAttribute("xid")).intValue());
		msg.setTopic(decodeString(in));
		msg.setProperties((Dictionary) in.getObject());
		out.write(msg);
		return MessageDecoderResult.OK;
	}

	public void encodeBody(IoSession session, RemoteOSGiMessage message,
			ByteBuffer buf) throws IOException {
		final RemoteEventMessage msg = (RemoteEventMessage) message;
		encodeString(buf, msg.getTopic());
		buf.putObject(msg.getProperties());
	}

	public Set getMessageTypes() {
		return MESSAGE_TYPE;
	}

}
