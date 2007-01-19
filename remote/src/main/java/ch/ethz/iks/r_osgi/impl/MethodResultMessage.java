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
package ch.ethz.iks.r_osgi.impl;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;

import ch.ethz.iks.util.SmartSerializer;

/**
 * <p>
 * MethodResultMessage is used to return the result of a method invocation to
 * the invoking remote peer.
 * </p>
 * 
 * @author Jan S. Rellermeyer, ETH Zurich
 * @since 0.1
 */
class MethodResultMessage extends RemoteOSGiMessageImpl {

	/**
	 * the error flag.
	 */
	private final byte errorFlag;

	/**
	 * the return value.
	 */
	private final Object result;

	/**
	 * the exception.
	 */
	private final Throwable exception;

	/**
	 * creates a new MethodResultMessage from InvokeMethodMessage and set the
	 * exception.
	 * 
	 * @param inv
	 *            the <code>InvokeMethodMessage</code>.
	 * @param exception
	 *            the exception.
	 */
	MethodResultMessage(final InvokeMethodMessage inv, final Throwable exception) {
		funcID = METHOD_RESULT;
		xid = inv.getXID();
		this.errorFlag = 1;
		this.exception = exception;
		this.result = null;
	}

	/**
	 * creates a new MethodResultMessage from InvokeMethodMessage and set the
	 * return value.
	 * 
	 * @param inv
	 *            the <code>InvokeMethodMessage</code>.
	 * @param resultValue
	 *            the return value of the invoked method.
	 */
	MethodResultMessage(final InvokeMethodMessage inv, final Object resultValue) {
		funcID = METHOD_RESULT;
		xid = inv.getXID();
		this.errorFlag = 0;
		this.result = resultValue;
		this.exception = null;
	}

	/**
	 * creates a new MethodResultMessage from network packet:
	 * 
	 * <pre>
	 *       0                   1                   2                   3
	 *       0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
	 *      +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	 *      |       R-OSGi header (function = Service = 2)                  |
	 *      +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	 *      |  error flag   | result or Exception                           \
	 *      +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	 * </pre>.
	 * 
	 * @param input
	 *            an <code>ObjectInputStream</code> that provides the body of
	 *            a R-OSGi network packet.
	 * @throws IOException
	 *             in case of IO failures.
	 */
	MethodResultMessage(final ObjectInputStream input) throws IOException {
		funcID = METHOD_RESULT;
		errorFlag = input.readByte();
		if (errorFlag == 0) {
			result = SmartSerializer.deserialize(input);
			exception = null;
		} else {
			exception = (Throwable) SmartSerializer.deserialize(input);
			result = null;
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
		if (exception == null) {
			out.writeByte(0);
			SmartSerializer.serialize(result, out);
		} else {
			out.writeByte(1);
			SmartSerializer.serialize(exception, out);
		}
	}

	/**
	 * did the method invocation cause an exception ?
	 * 
	 * @return <code>true</code>, if an exception has been thrown on the
	 *         remote side. In this case, the exception can be retrieved through
	 *         the <code>getException</code> method.
	 */
	final boolean causedException() {
		return (errorFlag == 1);
	}

	/**
	 * get the result object.
	 * 
	 * @return the return value of the invoked message.
	 */
	final Object getResult() {
		return result;
	}

	/**
	 * get the exception.
	 * 
	 * @return the exception or <code>null</code> if non was thrown.
	 */
	final Throwable getException() {
		return exception;
	}
	
	/**
	 * has no effect for this type.
	 * 
	 * @param protocol
	 *            the protocol.
	 * @param host
	 *            the host.
	 * @param port
	 *            the port.
	 * @see ch.ethz.iks.r_osgi.RemoteOSGiMessage#restamp(java.lang.String,
	 *      java.lang.String, int)
	 */
	public void restamp(final String protocol, final String host, final int port) {

	}

	/**
	 * String representation for debug outputs.
	 * 
	 * @return a string representation.
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("[METHOD_RESULT] - XID: ");
		buffer.append(xid);
		buffer.append(", errorFlag: ");
		buffer.append(errorFlag);
		if (causedException()) {
			buffer.append(", exception: ");
			buffer.append(exception.getMessage());
		} else {
			buffer.append(", result: ");
			buffer.append(result);
		}
		return buffer.toString();
	}
}
