/* Copyright (c) 2006-2008 Jan S. Rellermeyer
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
import java.net.SocketException;
import ch.ethz.iks.r_osgi.RemoteOSGiException;

/**
 * <p>
 * Abstract base class for all Messages.
 * </p>
 * 
 * @author Jan S. Rellermeyer, ETH Zurich
 * @since 0.1
 */
public abstract class RemoteOSGiMessage {

	/**
	 * type code for lease messages.
	 */
	public static final short LEASE = 1;

	/**
	 * type code for fetch service messages.
	 */
	public static final short FETCH_SERVICE = 2;

	/**
	 * type code for deliver service messages.
	 */
	public static final short DELIVER_SERVICE = 3;

	/**
	 * type code for deliver bundle messages.
	 * 
	 * @deprecated
	 */
	public static final short DELIVER_BUNDLE = 4;

	/**
	 * type code for invoke method messages.
	 */
	public static final short INVOKE_METHOD = 5;

	/**
	 * type code for method result messages.
	 */
	public static final short METHOD_RESULT = 6;

	/**
	 * type code for remote event messages.
	 */
	public static final short REMOTE_EVENT = 7;

	/**
	 * type code for time offset messages.
	 */
	public static final short TIME_OFFSET = 8;

	/**
	 * type code for service attribute updates.
	 */
	public static final short LEASE_UPDATE = 9;

	/**
	 * type code for stream request messages.
	 */
	public static final short STREAM_REQUEST = 10;

	/**
	 * type code for stream result messages.
	 */
	public static final short STREAM_RESULT = 11;

	/**
	 * the type code or functionID in SLP notation.
	 */
	private short funcID;

	/**
	 * the transaction id.
	 */
	protected short xid;

	/**
	 * hides the default constructor.
	 */
	RemoteOSGiMessage(final short funcID) {
		this.funcID = funcID;
	}

	/**
	 * get the transaction ID.
	 * 
	 * @return the xid.
	 * @see ch.ethz.iks.r_osgi.RemoteOSGiMessage#getXID()
	 * @since 0.6
	 */
	public final short getXID() {
		return xid;
	}

	/**
	 * set the xid.
	 * 
	 * @param xid
	 *            set the xid.
	 */
	public void setXID(short xid) {
		this.xid = xid;
	}

	/**
	 * Get the function ID (type code) of the message.
	 * 
	 * @see ch.ethz.iks.r_osgi.RemoteOSGiMessage#getFuncID()
	 * @return the type code.
	 * @since 0.6
	 */
	public final short getFuncID() {
		return funcID;
	}

	/**
	 * reads in a network packet and constructs the corresponding subtype of
	 * RemoteOSGiMessage from it. The header is:
	 * 
	 * <pre>
	 *   0                   1                   2                   3
	 *   0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
	 *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	 *  |    Version    |         Function-ID           |     XID       |
	 *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	 *  |    XID cntd.  | 
	 *  +-+-+-+-+-+-+-+-+
	 * </pre>
	 * 
	 * the body is processed by the subtype class.
	 * 
	 * @param input
	 *            the DataInput providing the network packet.
	 * @return the RemoteOSGiMessage.
	 * @throws SocketException
	 *             if something goes wrong.
	 */
	public static RemoteOSGiMessage parse(final ObjectInputStream input)
			throws IOException {
		input.readByte(); // version, currently unused
		short funcID = input.readByte();
		short xid = input.readShort();
		RemoteOSGiMessage msg;
		switch (funcID) {
		case LEASE:
			msg = new LeaseMessage(input);
			break;
		case FETCH_SERVICE:
			msg = new FetchServiceMessage(input);
			break;
		case DELIVER_SERVICE:
			msg = new DeliverServiceMessage(input);
			break;
		case INVOKE_METHOD:
			msg = new InvokeMethodMessage(input);
			break;
		case METHOD_RESULT:
			msg = new MethodResultMessage(input);
			break;
		case REMOTE_EVENT:
			msg = new RemoteEventMessage(input);
			break;
		case TIME_OFFSET:
			msg = new TimeOffsetMessage(input);
			break;
		case LEASE_UPDATE:
			msg = new LeaseUpdateMessage(input);
			break;
		case STREAM_REQUEST:
			msg = new StreamRequestMessage(input);
			break;
		case STREAM_RESULT:
			msg = new StreamResultMessage(input);
			break;
		default:
			throw new RemoteOSGiException("funcID " + funcID
					+ " not supported."); //$NON-NLS-1$ //$NON-NLS-2$
		}
		msg.funcID = funcID;
		msg.xid = xid;
		return msg;
	}

	/**
	 * write the RemoteOSGiMessage to an output stream.
	 * 
	 * @param out
	 *            the ObjectOutputStream.
	 * @throws IOException
	 *             in case of IO failures.
	 */
	public final void send(final ObjectOutputStream out) throws IOException {
		synchronized (out) {
			out.write(1);
			out.write(funcID);
			out.writeShort(xid);
			writeBody(out);
			out.flush();
			out.reset();
		}
	}

	/**
	 * write the body of a RemoteOSGiMessage.
	 * 
	 * @param output
	 *            the output stream.
	 * @throws IOException
	 *             in case of IO failures.
	 */
	protected abstract void writeBody(final ObjectOutputStream output)
			throws IOException;

	/**
	 * reads the bytes encoded as SLP string.
	 * 
	 * @param input
	 *            the DataInput.
	 * @return the byte array.
	 * @throws IOException
	 *             in case of IO failures.
	 */
	protected static byte[] readBytes(final ObjectInputStream input)
			throws IOException {
		int length = input.readInt();
		byte[] buffer = new byte[length];
		input.readFully(buffer);
		return buffer;
	}

	/**
	 * writes a byte array.
	 * 
	 * @param out
	 *            the output stream.
	 * @param bytes
	 *            the bytes.
	 * @throws IOException
	 *             in case of IO failures.
	 */
	protected static void writeBytes(final ObjectOutputStream out,
			final byte[] bytes) throws IOException {
		out.writeInt(bytes.length);
		if (bytes.length > 0) {
			out.write(bytes);
		}
	}

	/**
	 * write a string array.
	 * 
	 * @param out
	 *            the output stream.
	 * @param strings
	 *            the string array.
	 * @throws IOException
	 *             in case of IO failures.
	 */
	protected static void writeStringArray(final ObjectOutputStream out,
			final String[] strings) throws IOException {
		final short length = (short) strings.length;
		out.writeShort(length);
		for (short i = 0; i < length; i++) {
			out.writeUTF(strings[i]);
		}
	}

	/**
	 * read a string array.
	 * 
	 * @param in
	 *            the input stream
	 * @return the read string array.
	 * @throws IOException
	 *             in case of IO failures.
	 */
	protected static String[] readStringArray(final ObjectInputStream in)
			throws IOException {
		final short length = in.readShort();
		final String[] result = new String[length];
		for (short i = 0; i < length; i++) {
			result[i] = in.readUTF();
		}
		return result;
	}

}
