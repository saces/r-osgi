/* Copyright (c) 2006-2007 Jan S. Rellermeyer
 * Information and Communication Systems Research Group (IKS),
 * Institute for Pervasive Computing, ETH Zurich.
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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import ch.ethz.iks.r_osgi.RemoteOSGiException;

/**
 * <p>
 * DeliverServiceMessage is used to bring an OSGi service to a remote machine.
 * The service interface is transferred and if specified, also an abstract class
 * that can contain code that behaves like a smart proxy and moves some methods
 * to the client machine.
 * </p>
 * <p>
 * Currently, R-OSGi does not check for class dependencies so if an interface
 * method uses custom objects as parameter and it can not be assumed that the
 * client already has these custom objects, they have to be defined as class
 * injections. However, all inner classes of injections and possible inner
 * classes of the abstract class provided as smart proxy object are
 * automatically added to the class injections.
 * </p>
 * 
 * @author Jan S. Rellermeyer, ETH Zurich.
 * @since 0.1
 */
final class DeliverServiceMessage extends RemoteOSGiMessageImpl {

	/**
	 * The class name of the interface that describes the service.
	 */
	private final String[] serviceInterfaceNames;

	/**
	 * Optionally, the class name of a smart proxy class.
	 */
	private final String smartProxyName;

	/**
	 * the injections.
	 */
	private final HashMap injections;

	/**
	 * the imports.
	 */
	private final String imports;

	/**
	 * the exports.
	 */
	private final String exports;
	
	private String serviceID;

	/**
	 * Create a new DeliverServiceMessage for a proxied service.
	 * 
	 * @param serviceInterfaceName
	 *            the name of the interface class.
	 * @param smartProxyName
	 *            the name of the smart proxy class or <code>null</code>.
	 * @param injections
	 *            the injections as a <code>HashMap</code> with name/bytes of
	 *            the injections.
	 * @param imports
	 *            the imports statement for the proxy bundle.
	 * @param exports
	 *            the export statement for the proxy bundle.
	 * @throws RemoteOSGiException
	 *             in case of Exceptions during class serialization.
	 */
	DeliverServiceMessage(final String serviceInterfaceNames[],
			final String smartProxyName, final HashMap injections,
			final String imports, final String exports)
			throws RemoteOSGiException {
		funcID = DELIVER_SERVICE;		
		this.serviceInterfaceNames = serviceInterfaceNames;
		this.smartProxyName = smartProxyName;
		this.injections = injections;
		this.imports = imports;
		this.exports = exports;		
	}

	/**
	 * 
	 * @param fetchReq
	 * @param attributes
	 */
	void init(final long serviceID, final FetchServiceMessage fetchReq) {
		this.serviceID = String.valueOf(serviceID);
		this.xid = fetchReq.xid;
	}

	/**
	 * Create a new DeliverServiceMessage from a network packet.
	 * 
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
	 *        |          interface name                                       \ 
	 *        +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	 *        | length of &lt;ProxyClassName&gt;    |    &lt;ProxyClassName&gt; String    \
	 *        +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	 *        |    number of injection blocks   |   class inj blocks          \
	 *        +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	 * </pre>
	 * 
	 * @param input
	 *            a <code>DataInput</code> that provides the body of a R-OSGi
	 *            network packet.
	 * @throws IOException
	 *             in case of parse errors.
	 */
	DeliverServiceMessage(final ObjectInputStream input) throws IOException {
		// the fragment that describes the service
		serviceID = input.readUTF();
		// imports
		imports = input.readUTF();
		// exports
		exports = input.readUTF();
		// interface names
		serviceInterfaceNames = readStringArray(input);
		// smart proxy name, if defined.
		String p = input.readUTF();
		smartProxyName = "".equals(p) ? null : p;
		// process all class injections
		final short blocks = input.readShort();
		injections = new HashMap(blocks);
		for (short i = 0; i < blocks; i++) {
			injections.put(input.readUTF(), readBytes(input));
		}
	}

	/**
	 * write the message body to a stream.
	 * 
	 * @param out
	 *            the output stream.
	 * @throws IOException
	 *             in case of parse errors.
	 */
	public void writeBody(final ObjectOutputStream out) throws IOException {
		out.writeUTF(serviceID);
		out.writeUTF(imports);
		out.writeUTF(exports);
		writeStringArray(out, serviceInterfaceNames);
		out.writeUTF(smartProxyName == null ? "" : smartProxyName);
		final short blocks = (short) injections.size();
		out.writeShort(blocks);
		final String[] injectionNames = (String[]) injections.keySet().toArray(
				new String[blocks]);
		for (short i = 0; i < blocks; i++) {
			out.writeUTF(injectionNames[i]);
			writeBytes(out, (byte[]) injections.get(injectionNames[i]));
		}
	}

	String getServiceID() {
		return serviceID;
	}
	
	/**
	 * get the interface name of the delivered service.
	 * 
	 * @return the class name of the interface.
	 */
	String[] getInterfaceNames() {
		return serviceInterfaceNames;
	}

	/**
	 * get the interface class.
	 * 
	 * @return the interface class.
	 */
	byte[] getInterfaceClass() {
		return (byte[]) injections.get(serviceInterfaceNames[0].replace('.',
				'/')
				+ ".class");
	}

	/**
	 * get the smart proxy class name.
	 * 
	 * @return the class name of the smart proxy or null of undefined.
	 */
	String getProxyName() {
		return smartProxyName;
	}

	/**
	 * get the smart proxy class.
	 * 
	 * @return the class or null if undefined.
	 */
	byte[] getProxyClass() {
		return (byte[]) injections.get(smartProxyName.replace('.', '/')
				+ ".class");
	}

	/**
	 * get the list of class injection.
	 * 
	 * @return a <code>List</code> of class names.
	 */
	Map getInjections() {
		return injections;
	}

	/**
	 * get the imports.
	 * 
	 * @return the imports.
	 */
	String getImports() {
		return imports;
	}

	/**
	 * get the exports.
	 * 
	 * @return the exports.
	 */
	String getExports() {
		return exports;
	}

	/**
	 * String representation for debug outputs.
	 * 
	 * @return a string representation.
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("[DELIVER_SERVICE] - XID: ");
		buffer.append(xid);
		buffer.append(", serviceInterfaceName: ");
		buffer.append(Arrays.asList(serviceInterfaceNames));
		if (smartProxyName != null) {
			buffer.append(" smartProxy: ");
			buffer.append(smartProxyName);
		}
		if (injections.size() > 0) {
			buffer.append(", classInjections ");
			buffer.append(injections.keySet());
		}
		return buffer.toString();
	}
}
