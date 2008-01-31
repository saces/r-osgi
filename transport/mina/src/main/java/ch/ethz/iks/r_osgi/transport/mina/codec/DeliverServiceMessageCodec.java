package ch.ethz.iks.r_osgi.transport.mina.codec;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.IoSession;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.apache.mina.filter.codec.demux.MessageDecoderResult;

import ch.ethz.iks.r_osgi.messages.DeliverServiceMessage;
import ch.ethz.iks.r_osgi.messages.RemoteOSGiMessage;

/**
 * <pre>
 *         0                   1                   2                   3
 *         0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 *        +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *        |       R-OSGi header (function = Service = 2)                  |
 *        +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *        |   length of &lt;ServiceURL&gt;     |    &lt;ServiceURL&gt; String       \
 *        +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *        |          imports                                              \ 
 *        +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *        |          exports                                              \ 
 *        +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *        |          interface names                                      \ 
 *        +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *        | length of &lt;ProxyClassName&gt;    |    &lt;ProxyClassName&gt; String    \
 *        +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *        |    number of injection blocks   |   class inj blocks          \
 *        +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * </pre>
 * 
 * @author rjan
 * 
 */
public class DeliverServiceMessageCodec extends RemoteOSGiMessageCodec {

	private static final Set MESSAGE_TYPE;

	static {
		MESSAGE_TYPE = Collections.unmodifiableSet(new HashSet(Arrays
				.asList(new Class[] { DeliverServiceMessage.class })));
	}

	public DeliverServiceMessageCodec() {
		super(RemoteOSGiMessage.DELIVER_SERVICE);
	}

	public MessageDecoderResult decodeBody(IoSession session, ByteBuffer in,
			ProtocolDecoderOutput out) throws Exception {
		final DeliverServiceMessage msg = new DeliverServiceMessage();
		msg.setXID(((Short) session.getAttribute("xid")).shortValue());
		msg.setServiceID(decodeString(in));
		msg.setImports(decodeString(in));
		msg.setExports(decodeString(in));
		msg.setInterfaceNames(decodeStringArray(in));
		msg.setSmartProxyName(decodeString(in));

		final short blocks = in.getShort();
		final HashMap injections = new HashMap(blocks);
		for (short i = 0; i < blocks; i++) {
			if (!in.prefixedDataAvailable(2, Short.MAX_VALUE)) {
				return MessageDecoderResult.NEED_DATA;
			}
			final String name = decodeString(in);
			if (!in.prefixedDataAvailable(4, Integer.MAX_VALUE)) {
				return MessageDecoderResult.NEED_DATA;
			}
			injections.put(name, decodeBytes(in));
		}
		msg.setInjections(injections);
		out.write(msg);

		return MessageDecoderResult.OK;
	}

	public void encodeBody(IoSession session, RemoteOSGiMessage message,
			ByteBuffer buf) throws IOException {
		final DeliverServiceMessage msg = (DeliverServiceMessage) message;
		encodeString(buf, msg.getServiceID());
		encodeString(buf, msg.getImports());
		encodeString(buf, msg.getExports());
		encodeStringArray(buf, msg.getInterfaceNames());
		encodeString(buf, msg.getSmartProxyName());

		final Map injections = msg.getInjections();
		final short blocks = (short) injections.size();
		buf.putShort(blocks);
		final String[] injectionNames = (String[]) injections.keySet().toArray(
				new String[blocks]);
		for (int i = 0; i < blocks; i++) {
			encodeString(buf, injectionNames[i]);
			encodeBytes(buf, (byte[]) injections.get(injectionNames[i]));
		}
	}

	public Set getMessageTypes() {
		return MESSAGE_TYPE;
	}

}
