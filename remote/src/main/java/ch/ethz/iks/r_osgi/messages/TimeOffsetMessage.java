/* Copyright (c) 2006-2007 Jan S. Rellermeyer
 * Information and Communication Systems Research Group (IKS),
 * Department of Computer Science, ETH Zurich.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *    - Redistributions of source code must retain the above copyright notice,
 *      this list of conditions and the following disclaimer.
 *    - Redistributions in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in the
 *      documentation and/or other materials provided with the distribution.
 *    - Neither the name of ETH Zurich nor the names of its contributors may be
 *      used to endorse or promote products derived from this software without
 *      specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package ch.ethz.iks.r_osgi.messages;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;
import java.util.Arrays;

import ch.ethz.iks.util.SmartSerializer;

/**
 * <p>
 * TimeSyncMessage measures the time offset between two peers.
 * </p>
 * 
 * @author Jan S. Rellermeyer, ETH Zurich
 * @since 0.2
 */
public class TimeOffsetMessage extends RemoteOSGiMessage {
	/**
	 * the time series. Both peers append their timestamps and the series is
	 * then evaluated to determine the offset
	 */
	private Long[] timeSeries;

	/**
	 * creates a new empty TimeSyncMessage.
	 */
	public TimeOffsetMessage() {
		super(TIME_OFFSET);
		timeSeries = new Long[0];
	}

	/**
	 * creates a new TimeSyncMessage from network packet:
	 * 
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
	 * @param input
	 *            an <code>ObjectInputStream</code> that provides the body of
	 *            a R-OSGi network packet.
	 * @throws IOException
	 *             in case of IO failures.
	 */
	public TimeOffsetMessage(final ObjectInputStream input) throws IOException {
		super(TIME_OFFSET);
		timeSeries = (Long[]) SmartSerializer.deserialize(input);
	}

	/**
	 * write the body of the message to a stream.
	 * 
	 * @param out
	 *            the ObjectOutputStream.
	 * @throws IOException
	 *             in case of IO failures.
	 * @see ch.ethz.iks.r_osgi.messages.RemoteOSGiMessage#getBody()
	 */
	public void writeBody(final ObjectOutputStream out) throws IOException {
		SmartSerializer.serialize(timeSeries, out);
	}

	/**
	 * add the current time to the time series.
	 */
	public void timestamp() {
		int len = timeSeries.length;
		final Long[] newSeries = new Long[len + 1];
		System.arraycopy(timeSeries, 0, newSeries, 0, len);
		newSeries[len] = new Long(System.currentTimeMillis());
		timeSeries = newSeries;
	}

	/**
	 * for retransmissions: replace the last timestamp with the current one. The
	 * sending method must increase the XID to signal that this is a "new"
	 * message rather than a strict retransmission.
	 */
	public void restamp(short newXID) {
		this.xid = newXID;
		timeSeries[timeSeries.length - 1] = new Long(System.currentTimeMillis());
	}

	/**
	 * returns the time series.
	 * 
	 * @return the time series as <code>Long</code> array.
	 */
	public final Long[] getTimeSeries() {
		return timeSeries;
	}

	public final void setTimeSeries(final Long[] series) {
		this.timeSeries = series;
	}

	/**
	 * String representation for debug outputs.
	 * 
	 * @return a string representation.
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("[TIME_OFFSET, ");
		buffer.append("] - XID: ");
		buffer.append(xid);
		buffer.append("timeSeries: ");
		buffer.append(Arrays.asList(timeSeries));
		return buffer.toString();
	}
}