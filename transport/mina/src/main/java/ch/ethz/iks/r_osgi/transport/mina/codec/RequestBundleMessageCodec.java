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
import ch.ethz.iks.r_osgi.messages.RequestBundleMessage;

public class RequestBundleMessageCodec extends RemoteOSGiMessageCodec {

	private static final Set MESSAGE_TYPE;

	static {
		MESSAGE_TYPE = Collections.unmodifiableSet(new HashSet(Arrays
				.asList(new Class[] { RequestBundleMessage.class })));
	}

	public RequestBundleMessageCodec() {
		super(RemoteOSGiMessage.DELIVER_BUNDLES);
	}

	public MessageDecoderResult decodeBody(IoSession session, ByteBuffer in,
			ProtocolDecoderOutput out) throws Exception {
		final RequestBundleMessage msg = new RequestBundleMessage();
		msg.setXID(((Integer) session.getAttribute("xid")).intValue());
		msg.setServiceID(decodeString(in));
		out.write(msg);

		return MessageDecoderResult.OK;
	}

	public void encodeBody(IoSession session, RemoteOSGiMessage message,
			ByteBuffer buf) throws IOException {
		final RequestBundleMessage msg = (RequestBundleMessage) message;
		encodeString(buf, msg.getServiceID());
	}

	public Set getMessageTypes() {
		return MESSAGE_TYPE;
	}

}
