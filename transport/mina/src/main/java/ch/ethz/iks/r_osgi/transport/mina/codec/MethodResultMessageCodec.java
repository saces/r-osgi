package ch.ethz.iks.r_osgi.transport.mina.codec;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.IoSession;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.apache.mina.filter.codec.demux.MessageDecoderResult;

import ch.ethz.iks.r_osgi.messages.RemoteCallResultMessage;
import ch.ethz.iks.r_osgi.messages.RemoteOSGiMessage;

/**
 * <pre>
 *       0                   1                   2                   3
 *       0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 *      +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *      |       R-OSGi header (function = Service = 2)                  |
 *      +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *      |  error flag   | result or Exception                           \
 *      +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * </pre>
 * 
 * .
 * 
 * @author rjan
 * 
 */
public class MethodResultMessageCodec extends RemoteOSGiMessageCodec {

	private static final Set MESSAGE_TYPE;

	static {
		MESSAGE_TYPE = Collections.unmodifiableSet(new HashSet(Arrays
				.asList(new Class[] { RemoteCallResultMessage.class })));
	}

	public MethodResultMessageCodec() {
		super(RemoteOSGiMessage.REMOTE_CALL_RESULT);
	}

	public MessageDecoderResult decodeBody(IoSession session, ByteBuffer in,
			ProtocolDecoderOutput out) throws Exception {
		final RemoteCallResultMessage msg = new RemoteCallResultMessage();
		msg.setXID(((Integer) session.getAttribute("xid")).intValue());
		if (in.get() == 0) {
			msg.setResult(in.getObject());
		} else {
			msg.setException((Throwable) in.getObject());
		}
		out.write(msg);
		return MessageDecoderResult.OK;
	}

	public void encodeBody(IoSession session, RemoteOSGiMessage message,
			ByteBuffer buf) {
		final RemoteCallResultMessage msg = (RemoteCallResultMessage) message;
		if (msg.causedException()) {
			buf.put((byte) 1);
			buf.putObject(msg.getException());
		} else {
			buf.put((byte) 0);
			buf.putObject(msg.getResult());
		}
	}

	public Set getMessageTypes() {
		return MESSAGE_TYPE;
	}

}
