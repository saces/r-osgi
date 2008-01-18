package ch.ethz.iks.r_osgi.messages;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import ch.ethz.iks.util.SmartSerializer;

public class StreamRequestMessage extends RemoteOSGiMessage {

	/**
	 * operation identifier for simple read operation on stream
	 */
	public static final byte READ = 0;

	/**
	 * operation identifier for read operation reading more than one byte at
	 * once
	 */
	public static final byte READ_ARRAY = 1;

	/**
	 * operation identifier for simple write operation on stream
	 */
	public static final byte WRITE = 2;

	/**
	 * operation identifier for write operation writing more than one byte at
	 * once
	 */
	public static final byte WRITE_ARRAY = 3;

	/**
	 * stream ID of the target stream
	 */
	private short streamID;

	/**
	 * operation on the target stream
	 */
	private byte op;

	/**
	 * length argument (read) or value (write)
	 */
	private int lenOrVal;

	/**
	 * array containing data to write
	 */
	private byte[] b;

	/**
	 * creates a new StreamRequestMessage.
	 */
	public StreamRequestMessage() {
		super(STREAM_REQUEST);
	}

	/**
	 * creates a new StreamRequestMessage from network packet:
	 * 
	 * <pre>
	 *       0                   1                   2                   3
	 *       0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
	 *      +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	 *      |    R-OSGi header (function = StreamRequestMsg = 10)           |
	 *      +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	 *      |              short            |       op      |      len      |
	 *      +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	 *      |                   len (ctd.)                  |       b       \
	 *      +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	 * </pre>.
	 * 
	 * @param input
	 *            an <code>ObjectInputStream</code> that provides the body of
	 *            a R-OSGi network packet.
	 * @throws IOException
	 *             in case of IO failures.
	 */
	StreamRequestMessage(final ObjectInputStream input) throws IOException {
		super(STREAM_REQUEST);
		this.streamID = input.readShort();
		this.op = input.readByte();
		switch (op) {
		case READ:
			this.lenOrVal = 1;
			this.b = null;
			break;
		case READ_ARRAY:
		case WRITE:
			this.lenOrVal = input.readInt();
			this.b = null;
			break;
		case WRITE_ARRAY:
			this.lenOrVal = input.readInt();
			this.b = (byte[]) SmartSerializer.deserialize(input);
			break;
		default:
			throw new IllegalArgumentException(
					"op code not within valid range: " + op);
		}
	}

	/**
	 * write the body of the message to a stream.
	 * 
	 * @param out
	 *            the ObjectOutputStream.
	 * @throws IOException
	 *             in case of IO failures.
	 * @see ch.ethz.iks.r_osgi.impl.RemoteOSGiMessageImpl#getBody()
	 */
	public void writeBody(final ObjectOutputStream out) throws IOException {
		out.writeShort(streamID);
		out.writeByte(op);
		if (op != READ)  {
			out.writeInt(lenOrVal);
			if (op == WRITE_ARRAY) {
				SmartSerializer.serialize(b, out);
			}
		}
	}

	/**
	 * get the ID of the stream.
	 * 
	 * @return the ID of the stream.
	 */
	public short getStreamID() {
		return streamID;
	}
	
	/**
	 * set the ID of the stream.
	 * 
	 * @param streamID the ID of the stream.
	 */
	public void setStreamID(final short streamID) {
		this.streamID = streamID;
	}

	/**
	 * get the operation code.
	 * 
	 * @return the operation code.
	 */
	public byte getOp() {
		return op;
	}

	/**
	 * set the operation code.
	 * 
	 * @param op the operation code.
	 */
	public void setOp(final byte op) {
		this.op = op;
	}
	
	/**
	 * get the length (read op) or value (write op) field.
	 * 
	 * @return the length or value.
	 */
	public int getLenOrVal() {
		return lenOrVal;
	}

	/**
	 * set the length (read op) or value (write op) field.
	 * 
	 * @param lenOrVal the length or value.
	 */
	public void setLenOrVal(final int lenOrVal) {
		this.lenOrVal = lenOrVal;
	}
	
	/**
	 * get the data array.
	 * 
	 * @return the data array.
	 */
	public byte[] getData() {
		return b;
	}
	
	/**
	 * set the data array.
	 * 
	 * @param b the data array to store.
	 */
	public void setData(final byte[] b) {
		this.b = b;
	}
	
	/**
	 * String representation for debug outputs.
	 * 
	 * @return a string representation.
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("[STREAM_REQUEST] - XID: ");
		buffer.append(xid);
		buffer.append(", streamID: ");
		buffer.append(streamID);
		buffer.append(", op: ");
		buffer.append(op);
		buffer.append(", len: ");
		buffer.append(lenOrVal);
		return buffer.toString();
	}

}
