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
import ch.ethz.iks.r_osgi.messages.RemoteOSGiMessage;
import ch.ethz.iks.r_osgi.messages.RequestServiceMessage;

/**
 * <pre>
 *     0                   1                   2                   3
 *     0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 *    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *    |       R-OSGi header (function = REQUEST_SERVICE = 1)          |
 *    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *    |   length of &lt;serviceID&gt;     |     &lt;serviceID&gt; String      \
 *    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * </pre>
 * 
 * @author Jan S. Rellermeyer, ETH Zurich
 * 
 */
public class RequestServiceMessageCodec extends RemoteOSGiMessageCodec {

	private static final Set MESSAGE_TYPE;

	static {
		MESSAGE_TYPE = Collections.unmodifiableSet(new HashSet(Arrays
				.asList(new Class[] { RequestServiceMessage.class })));
	}

	public RequestServiceMessageCodec() {
		super(RemoteOSGiMessage.REQUEST_SERVICE);
	}

	public MessageDecoderResult decodeBody(IoSession session, ByteBuffer in,
			ProtocolDecoderOutput out) throws Exception {
		final RequestServiceMessage msg = new RequestServiceMessage();
		msg.setXID(((Integer) session.getAttribute("xid")).intValue());
		msg.setServiceID(decodeString(in));
		out.write(msg);
		return MessageDecoderResult.OK;
	}

	public void encodeBody(IoSession session, RemoteOSGiMessage message,
			ByteBuffer buf) throws IOException {
		final RequestServiceMessage msg = (RequestServiceMessage) message;
		encodeString(buf, msg.getServiceID());
	}

	public Set getMessageTypes() {
		return MESSAGE_TYPE;
	}

}
