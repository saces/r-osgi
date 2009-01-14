package ch.ethz.iks.r_osgi.transport.mina.codec;

import org.apache.mina.filter.codec.demux.DemuxingProtocolCodecFactory;

public class RemoteOSGiProtocolCodecFactory extends
		DemuxingProtocolCodecFactory {

	public RemoteOSGiProtocolCodecFactory() {
		register(DeliverBundlesMessageCodec.class);
		register(DeliverServiceMessageCodec.class);
		register(LeaseMessageCodec.class);
		register(LeaseUpdateMessageCodec.class);
		register(RemoteCallMessageCodec.class);
		register(RemoteCallResultMessageCodec.class);
		register(RequestBundleMessageCodec.class);
		register(RequestDependenciesMessageCodec.class);
		register(RequestServiceMessageCodec.class);
		register(RemoteEventMessageCodec.class);
		register(TimeOffsetMessageCodec.class);
	}

}
