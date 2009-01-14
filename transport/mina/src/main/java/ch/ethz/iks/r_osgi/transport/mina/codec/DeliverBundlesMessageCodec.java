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

import ch.ethz.iks.r_osgi.messages.DeliverBundlesMessage;
import ch.ethz.iks.r_osgi.messages.RemoteOSGiMessage;

public class DeliverBundlesMessageCodec extends RemoteOSGiMessageCodec {

	private static final Set MESSAGE_TYPE;

	static {
		MESSAGE_TYPE = Collections.unmodifiableSet(new HashSet(Arrays
				.asList(new Class[] { DeliverBundlesMessage.class })));
	}

	public DeliverBundlesMessageCodec() {
		super(RemoteOSGiMessage.DELIVER_BUNDLES);
	}

	public MessageDecoderResult decodeBody(IoSession session, ByteBuffer in,
			ProtocolDecoderOutput out) throws Exception {
		final DeliverBundlesMessage msg = new DeliverBundlesMessage();
		msg.setXID(((Integer) session.getAttribute("xid")).intValue());

		final int bundles = in.getInt();
		final byte[][] bytes = new byte[bundles][];
		for (int i = 0; i < bundles; i++) {
			bytes[i] = decodeBytes(in);
		}
		msg.setDependencies(bytes);
		out.write(msg);

		return MessageDecoderResult.OK;
	}

	public void encodeBody(IoSession session, RemoteOSGiMessage message,
			ByteBuffer buf) throws IOException {
		final DeliverBundlesMessage msg = (DeliverBundlesMessage) message;
		final byte[][] bytes = msg.getDependencies();
		final int bundles = bytes.length;
		for (int i = 0; i < bundles; i++) {
			encodeBytes(buf, bytes[i]);
		}
	}

	public Set getMessageTypes() {
		return MESSAGE_TYPE;
	}

}
