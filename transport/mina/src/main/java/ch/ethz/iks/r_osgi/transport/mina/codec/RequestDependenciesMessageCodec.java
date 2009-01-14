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
import ch.ethz.iks.r_osgi.messages.RequestDependenciesMessage;

public class RequestDependenciesMessageCodec extends RemoteOSGiMessageCodec {

	private static final Set MESSAGE_TYPE;

	static {
		MESSAGE_TYPE = Collections.unmodifiableSet(new HashSet(Arrays
				.asList(new Class[] { RequestDependenciesMessage.class })));
	}

	public RequestDependenciesMessageCodec() {
		super(RemoteOSGiMessage.DELIVER_BUNDLES);
	}

	public MessageDecoderResult decodeBody(IoSession session, ByteBuffer in,
			ProtocolDecoderOutput out) throws Exception {
		final RequestDependenciesMessage msg = new RequestDependenciesMessage();
		msg.setXID(((Integer) session.getAttribute("xid")).intValue());
		msg.setPackages(decodeStringArray(in));
		out.write(msg);

		return MessageDecoderResult.OK;
	}

	public void encodeBody(IoSession session, RemoteOSGiMessage message,
			ByteBuffer buf) throws IOException {
		final RequestDependenciesMessage msg = (RequestDependenciesMessage) message;
		encodeStringArray(buf, msg.getPackages());
	}

	public Set getMessageTypes() {
		return MESSAGE_TYPE;
	}

}
