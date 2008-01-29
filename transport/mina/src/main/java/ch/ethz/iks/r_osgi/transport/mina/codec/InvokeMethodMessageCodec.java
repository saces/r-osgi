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

import ch.ethz.iks.r_osgi.messages.InvokeMethodMessage;
import ch.ethz.iks.r_osgi.messages.RemoteOSGiMessage;

/**
 * <pre>
 *       0                   1                   2                   3
 *       0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 *      +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *      |       R-OSGi header (function = InvokeMsg = 3)                |
 *      +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *      |   length of &lt;serviceID&gt;     |    &lt;serviceID&gt; String       \
 *      +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *      |    length of &lt;MethodSignature&gt;     |     &lt;MethodSignature&gt; String       \
 *      +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *      |   number of param blocks      |     Param blocks (if any)     \
 *      +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * </pre>.
 * 
 * @author rjan
 * 
 */
public class InvokeMethodMessageCodec extends RemoteOSGiMessageCodec {

	private static final Set MESSAGE_TYPE;

	static {
		MESSAGE_TYPE = Collections.unmodifiableSet(new HashSet(Arrays
				.asList(new Class[] { InvokeMethodMessage.class })));
	}

	public InvokeMethodMessageCodec() {
		super(RemoteOSGiMessage.INVOKE_METHOD);
	}

	public MessageDecoderResult decodeBody(IoSession session, ByteBuffer in,
			ProtocolDecoderOutput out) throws Exception {
		final InvokeMethodMessage msg = new InvokeMethodMessage();
		msg.setXID(((Short) session.getAttribute("xid")).shortValue());
		msg.setServiceID(decodeString(in));
		msg.setMethodSignature(decodeString(in));

		final short argLength = in.getShort();
		final Object[] arguments = new Object[argLength];
		for (short i = 0; i < argLength; i++) {
			arguments[i] = in.getObject();
		}
		msg.setArgs(arguments);
		out.write(msg);
		return MessageDecoderResult.OK;
	}

	public void encodeBody(IoSession session, RemoteOSGiMessage message,
			ByteBuffer buf) throws IOException {
		final InvokeMethodMessage msg = (InvokeMethodMessage) message;
		encodeString(buf, msg.getServiceID());
		encodeString(buf, msg.getMethodSignature());

		final Object[] arguments = msg.getArgs();
		buf.putShort((short) arguments.length);
		for (int i = 0; i < arguments.length; i++) {
			buf.putObject(arguments[i]);
		}
	}

	public Set getMessageTypes() {
		return MESSAGE_TYPE;
	}

}
