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
import java.util.Arrays;

import ch.ethz.iks.util.SmartSerializer;

/**
 * <p>
 * InvokeMethodMessage is used to invoke a method of a remote service.
 * </p>
 * 
 * @author Jan S. Rellermeyer, ETH Zurich
 * @since 0.1
 */
class InvokeMethodMessage extends RemoteOSGiMessageImpl {

	/**
	 * the serviceURL string of the remote service.
	 */
	private String serviceURL;

	/**
	 * the signature of the method that is requested to be invoked.
	 */
	private String methodSignature;

	/**
	 * the argument array of the method call.
	 */
	private Object[] arguments;

	/**
	 * creates a new InvokeMethodMessage.
	 * 
	 * @param service
	 *            the serviceURL of the service.
	 * @param methodSignature
	 *            the method signature.
	 * @param params
	 *            the parameter that are passed to the method.
	 */
	InvokeMethodMessage(final String service, final String methodSignature, final Object[] params) {
		funcID = INVOKE_METHOD;
		this.serviceURL = service;
		this.methodSignature = methodSignature;
		this.arguments = params;
	}

	/**
	 * creates a new InvokeMethodMessage from network packet:
	 * 
	 * <pre>
	 *    0                   1                   2                   3
	 *    0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
	 *   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	 *   |       R-OSGi header (function = InvokeMsg = 3)                |
	 *   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	 *   |   length of &lt;ServiceURL&gt;     |    &lt;ServiceURL&gt; String       \
	 *   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	 *   |    length of &lt;MethodSignature&gt;     |     &lt;MethodSignature&gt; String       \
	 *   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	 *   |   number of param blocks      |     Param blocks (if any)     \
	 *   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	 * </pre>.
	 * 
	 * @param input
	 *            an <code>ObjectInputStream</code> that provides the body of
	 *            a R-OSGi network packet.
	 * @throws IOException
	 *             in case of IO failures.
	 */
	InvokeMethodMessage(final ObjectInputStream input) throws IOException {
		funcID = INVOKE_METHOD;
		serviceURL = input.readUTF();
		methodSignature = input.readUTF();
		final short argLength = input.readShort();
		arguments = new Object[argLength];
		for (int i = 0; i < argLength; i++) {
			arguments[i] = SmartSerializer.deserialize(input);
		}
	}

	/**
	 * write the body of the message to a stream.
	 * 
	 * @param out the ObjectOutputStream.
	 * @throws IOException
	 *             in case of IO failures.
	 * @see ch.ethz.iks.r_osgi.impl.RemoteOSGiMessageImpl#getBody()
	 */
	public void writeBody(final ObjectOutputStream out) throws IOException {
		out.writeUTF(serviceURL);
		out.writeUTF(methodSignature);
		out.writeShort(arguments.length);
		for (int i = 0; i < arguments.length; i++) {
			SmartSerializer.serialize(arguments[i], out);
		}
	}

	/**
	 * get the parameters for the invoked method.
	 * 
	 * @return the parameters.
	 */
	Object[] getArgs() {
		return arguments;
	}

	/**
	 * get the service url of the service.
	 * 
	 * @return the service url as string.
	 */
	String getServiceURL() {
		return serviceURL;
	}

	/**
	 * get the method signature.
	 * 
	 * @return the method signature.
	 */
	String getMethodSignature() {
		return methodSignature;
	}

	/**
	 * String representation for debug outputs.
	 * 
	 * @return a string representation.
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("[INVOKE_METHOD] - XID: ");
		buffer.append(xid);
		buffer.append(", serviceURL ");
		buffer.append(serviceURL);
		buffer.append(", methodName ");
		buffer.append("methodName");
		buffer.append(", params: ");
		buffer.append(Arrays.asList(getArgs()));
		return buffer.toString();
	}
}