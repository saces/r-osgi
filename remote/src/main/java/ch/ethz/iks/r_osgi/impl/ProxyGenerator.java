/* Copyright (c) 2006-2008 Jan S. Rellermeyer
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.CRC32;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodAdapter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.osgi.service.log.LogService;
import ch.ethz.iks.r_osgi.URI;
import ch.ethz.iks.r_osgi.RemoteOSGiService;
import ch.ethz.iks.r_osgi.Remoting;
import ch.ethz.iks.r_osgi.channels.ChannelEndpoint;
import ch.ethz.iks.r_osgi.messages.DeliverServiceMessage;
import ch.ethz.iks.r_osgi.types.ServiceUIComponent;

/**
 * Bytecode manipulation magic to build proxy bundles for interfaces and smart
 * proxy (abstract) classes.
 * 
 * @author Jan S. Rellermeyer, ETH Zurich.
 * @since 0.1
 */
class ProxyGenerator implements ClassVisitor, Opcodes {
	/**
	 * sourceID.
	 */
	private String sourceID;

	/**
	 * interface class name.
	 */
	private String[] interfaceClassNames;

	/**
	 * name of the implemented class.
	 */
	private String implName;

	/**
	 * the service uri.
	 */
	private String uri;

	/**
	 * the ASM class writer.
	 */
	private ClassWriter writer;

	/**
	 * the list of injections.
	 */
	private Map injections;

	/**
	 * set to determine if a method has already been implemented.
	 */
	private Set implemented;

	/**
	 * a list of super interfaces describing the whole interface hierarchy of
	 * the service interface.
	 */
	private List superInterfaces = new ArrayList();

	/**
	 * smart proxy class name.
	 */
	private String smartProxyClassName;

	/**
	 * the smart proxy name in dashed notation.
	 */
	private String smartProxyClassNameDashed;

	/**
	 * add lifecycle support to the smart proxy? Will be added if the abstract
	 * smart proxy class implements ch.ethz.iks.r_osgi.SmartProxy.
	 */
	private boolean addLifecycleSupport;

	/**
	 * the constants.
	 */
	private static final int[] ICONST = { ICONST_0, ICONST_1, ICONST_2,
			ICONST_3, ICONST_4, ICONST_5 };

	/**
	 * the boxed types.
	 */
	private static final String[] BOXED_TYPE = { "ERROR", "java/lang/Boolean",
			"java/lang/Character", "java/lang/Byte", "java/lang/Short",
			"java/lang/Integer", "java/lang/Float", "java/lang/Long",
			"java/lang/Double" };

	/**
	 * the unbox methods.
	 */
	private static final String[] UNBOX_METHOD = { "ERROR", "booleanValue",
			"charValue", "byteValue", "shortValue", "intValue", "floatValue",
			"longValue", "doubleValue" };

	/**
	 * the character table for generating a signature from an IP address.
	 */
	private static final char[] CHAR_TABLE = new char[] { 'a', 'b', 'c', 'd',
			'e', 'f', 'g', 'h', 'i', 'j' };

	/**
	 * remoting interface name.
	 */
	private static final String REMOTING_I = Remoting.class.getName().replace(
			'.', '/');

	/**
	 * channel endpoint interface name.
	 */
	private static final String ENDPOINT_I = ChannelEndpoint.class.getName()
			.replace('.', '/');

	/**
	 * ServiceUIComponent interface name.
	 */
	private static final String UICOMP_I = ServiceUIComponent.class.getName()
			.replace('.', '/');

	/**
	 * constructor.
	 */
	ProxyGenerator() {

	}

	/**
	 * 
	 * @param service
	 *            ServiceURL
	 * @param deliv
	 *            DeliverServiceMessage
	 * @return bundle location
	 * @throws IOException
	 *             in case of proxy generation error
	 */
	protected String generateProxyBundle(final URI service,
			final DeliverServiceMessage deliv) throws IOException {

		this.uri = service.toString();
		sourceID = generateSourceID(uri);
		implemented = new HashSet();
		injections = deliv.getInjections();
		final byte[] bytes = deliv.getSmartProxyName() == null ? generateProxyClass(
				deliv.getInterfaceNames(), deliv.getInterfaceClass())
				: generateProxyClass(deliv.getInterfaceNames(), deliv
						.getInterfaceClass(), deliv.getSmartProxyName(), deliv
						.getProxyClass());

		int pos = implName.lastIndexOf('/');
		final String fileName = pos > 0 ? implName.substring(pos) : implName;
		final String className = implName.replace('/', '.');
		JarEntry jarEntry;

		// generate Jar
		final Manifest mf = new Manifest();
		Attributes attr = mf.getMainAttributes();
		attr.putValue("Manifest-Version", "1.0");
		attr.putValue("Created-By", "R-OSGi Proxy Generator");
		attr.putValue("Bundle-Activator", className);
		attr.putValue("Bundle-Classpath", ".");
		attr
				.putValue(
						"Import-Package",
						"org.osgi.framework, ch.ethz.iks.r_osgi, ch.ethz.iks.r_osgi.types, ch.ethz.iks.r_osgi.channels"
								+ ("".equals(deliv.getImports()) ? "" : ", ")
								+ deliv.getImports());
		if (!"".equals(deliv.getExports())) {
			attr.putValue("Export-Package", deliv.getExports());
		}
		final File file = RemoteOSGiActivator.context.getDataFile(fileName
				+ "_" + sourceID + ".jar");
		final JarOutputStream out = new JarOutputStream(new FileOutputStream(
				file), mf);

		CRC32 crc = new CRC32();
		crc.update(bytes, 0, bytes.length);
		jarEntry = new JarEntry(implName + ".class");
		jarEntry.setSize(bytes.length);
		jarEntry.setCrc(crc.getValue());

		out.putNextEntry(jarEntry);
		out.write(bytes, 0, bytes.length);
		out.flush();
		out.closeEntry();

		final String[] injectionNames = (String[]) injections.keySet().toArray(
				new String[injections.size()]);
		// write the class injections
		for (int i = 0; i < injectionNames.length; i++) {

			String name = injectionNames[i];
			// the original smart proxy class is omitted
			if (name.equals(smartProxyClassNameDashed + ".class")) {
				continue;
			}
			final byte[] data = (byte[]) injections.get(name);
			final byte[] rewritten;

			// inner classes of the smart proxy have to be rewritten
			// so that references to the original smart proxy class
			// point to the generated proxy class
			if (smartProxyClassNameDashed != null
					&& name.startsWith(smartProxyClassNameDashed)) {
				final String rest = name.substring(smartProxyClassNameDashed
						.length());
				name = implName + rest;
				final ClassReader rewriterReader = new ClassReader(data);
				final ClassWriter rewriterWriter = new ClassWriter(0);
				rewriterReader.accept(new ClassRewriter(rewriterWriter),
						ClassReader.SKIP_DEBUG);
				rewritten = rewriterWriter.toByteArray();
			} else {
				rewritten = data;
			}

			crc = new CRC32();
			crc.update(rewritten, 0, rewritten.length);
			jarEntry = new JarEntry(name);
			jarEntry.setSize(rewritten.length);
			jarEntry.setCrc(crc.getValue());

			out.putNextEntry(jarEntry);
			out.write(rewritten, 0, rewritten.length);
			out.flush();
			out.closeEntry();
		}

		out.flush();
		out.finish();
		out.close();
		if (RemoteOSGiServiceImpl.PROXY_DEBUG) {
			RemoteOSGiServiceImpl.log.log(LogService.LOG_DEBUG,
					"Created Proxy Bundle " + file);
		}

		return file.getAbsolutePath();
	}

	/**
	 * 
	 * @param interfaceName
	 *            interface name
	 * @param interfaceClass
	 *            interface class
	 * @return bytes of the generated proxy class
	 * @throws IOException
	 *             in case of generation error
	 */
	private byte[] generateProxyClass(final String[] interfaceNames,
			final byte[] interfaceClass) throws IOException {
		interfaceClassNames = interfaceNames;
		implName = "proxy/" + sourceID + "/"
				+ interfaceNames[0].replace('.', '/') + "Impl";

		final ClassReader reader = new ClassReader(interfaceClass);
		writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
		reader.accept(this, null, ClassReader.SKIP_DEBUG);
		interfaceClassNames = null;
		final byte[] bytes = writer.toByteArray();
		return bytes;
	}

	/**
	 * 
	 * @param interfaceNames
	 *            interface names
	 * @param interfaceClass
	 *            interface class
	 * @param proxyName
	 *            smart proxy name
	 * @param proxyClass
	 *            smart proxy class
	 * @return bytes of the proxy class
	 * @throws IOException
	 *             in case of generation error
	 */
	private byte[] generateProxyClass(final String[] interfaceNames,
			final byte[] interfaceClass, final String proxyName,
			final byte[] proxyClass) throws IOException {
		interfaceClassNames = interfaceNames;
		implName = "proxy/" + sourceID + "/" + proxyName.replace('.', '/')
				+ "Impl";
		smartProxyClassName = proxyName;
		smartProxyClassNameDashed = smartProxyClassName.replace('.', '/');
		final ClassReader reader = new ClassReader(proxyClass);
		writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
		reader.accept(this, null, ClassReader.SKIP_DEBUG);
		recurseInterfaceHierarchy();
		interfaceClassNames = null;
		byte[] bytes = writer.toByteArray();
		return bytes;
	}

	private void recurseInterfaceHierarchy() throws IOException {
		// recurse over interface inheritance tree
		try {
			while (!superInterfaces.isEmpty()) {
				final String superIface = (String) superInterfaces.remove(0);
				final byte[] bytes = (byte[]) injections.get(superIface
						+ ".class");
				final ClassReader reader;
				if (bytes == null) {
					reader = new ClassReader(Class.forName(
							superIface.replace('/', '.')).getClassLoader()
							.getResourceAsStream(superIface + ".class"));
				} else {
					reader = new ClassReader(bytes);
				}

				reader.accept(this, null, 0);
			}
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

	/**
	 * @param version
	 *            version
	 * @param access
	 *            access
	 * @param name
	 *            name
	 * @param signature
	 *            signature
	 * @param superName
	 *            superName
	 * @param interfaces
	 *            interfaces
	 * @see org.objectweb.asm.ClassVisitor#visit(int, int, java.lang.String,
	 *      java.lang.String, java.lang.String, java.lang.String[])
	 */
	public void visit(final int version, final int access, final String name,
			final String signature, final String superName,
			final String[] interfaces) {
		MethodVisitor method;
		final FieldVisitor field;

		if (name.equals(smartProxyClassNameDashed)
				|| (smartProxyClassName == null && name
						.equals(interfaceClassNames[0].replace('.', '/')))) {

			if (RemoteOSGiServiceImpl.PROXY_DEBUG) {
				RemoteOSGiServiceImpl.log.log(LogService.LOG_DEBUG,
						"creating proxy class " + implName);
			}

			final String[] serviceInterfaces = new String[interfaceClassNames.length + 1];
			for (int i = 0; i < interfaceClassNames.length; i++) {
				serviceInterfaces[i] = interfaceClassNames[i].replace('.', '/');
			}
			serviceInterfaces[interfaceClassNames.length] = "org/osgi/framework/BundleActivator";

			if ((access & ACC_INTERFACE) == 0) {
				// we have a smart proxy
				final Set ifaces = new HashSet();
				ifaces.addAll(Arrays.asList(interfaces));
				ifaces.add("org/osgi/framework/BundleActivator");
				ifaces.addAll(Arrays.asList(serviceInterfaces));
				// V1_1
				writer.visit(version >= V1_5 ? V1_5 : V1_2, ACC_PUBLIC
						+ ACC_SUPER, implName, null, superName,
						(String[]) ifaces.toArray(new String[ifaces.size()]));

				if (java.util.Arrays.asList(interfaces).contains(
						"ch/ethz/iks/r_osgi/SmartProxy")) {
					addLifecycleSupport = true;
				}

			} else {
				// we have an interface
				writer.visit(version >= V1_5 ? V1_5 : V1_2, ACC_PUBLIC
						+ ACC_SUPER, implName, null, "java/lang/Object",
						serviceInterfaces);
				if (RemoteOSGiServiceImpl.PROXY_DEBUG) {
					RemoteOSGiServiceImpl.log.log(LogService.LOG_DEBUG,
							"Creating Proxy Bundle from Interfaces "
									+ Arrays.asList(interfaceClassNames));
				}

				// creates a MethodWriter for the (implicit) constructor
				method = writer.visitMethod(ACC_PUBLIC, "<init>", "()V", null,
						null);
				method.visitVarInsn(ALOAD, 0);
				method.visitMethodInsn(INVOKESPECIAL, "java/lang/Object",
						"<init>", "()V");
				method.visitInsn(RETURN);
				method.visitMaxs(2, 1);
				method.visitEnd();

			}

			field = writer.visitField(ACC_PRIVATE, "endpoint", "L" + ENDPOINT_I
					+ ";", null, null);
			field.visitEnd();

			{
				method = writer.visitMethod(ACC_PUBLIC, "start",
						"(Lorg/osgi/framework/BundleContext;)V", null,
						new String[] { "java/lang/Exception" });
				method.visitCode();
				method.visitVarInsn(ALOAD, 1);
				method.visitVarInsn(ALOAD, 1);
				method.visitLdcInsn(Remoting.class.getName());
				method
						.visitMethodInsn(INVOKEINTERFACE,
								"org/osgi/framework/BundleContext",
								"getServiceReference",
								"(Ljava/lang/String;)Lorg/osgi/framework/ServiceReference;");
				method
						.visitMethodInsn(INVOKEINTERFACE,
								"org/osgi/framework/BundleContext",
								"getService",
								"(Lorg/osgi/framework/ServiceReference;)Ljava/lang/Object;");
				method.visitTypeInsn(CHECKCAST, REMOTING_I);
				method.visitVarInsn(ASTORE, 2);
				method.visitVarInsn(ALOAD, 0);
				method.visitVarInsn(ALOAD, 2);
				method.visitLdcInsn(uri);
				method.visitMethodInsn(INVOKEINTERFACE, REMOTING_I,
						"getEndpoint", "(Ljava/lang/String;)L" + ENDPOINT_I
								+ ";");
				method.visitFieldInsn(PUTFIELD, implName, "endpoint", "L"
						+ ENDPOINT_I + ";");
				method.visitVarInsn(ALOAD, 0);
				method.visitFieldInsn(GETFIELD, implName, "endpoint", "L"
						+ ENDPOINT_I + ";");
				method.visitLdcInsn(uri);
				method.visitVarInsn(ALOAD, 1);

				final int len = interfaceClassNames.length;
				if (len < 6) {
					method.visitInsn(ICONST[interfaceClassNames.length]);
				} else {
					method.visitIntInsn(BIPUSH, len);
				}
				method.visitTypeInsn(ANEWARRAY, "java/lang/String");
				for (int i = 0; i < len && i < 6; i++) {
					method.visitInsn(DUP);
					method.visitInsn(ICONST[i]);
					method.visitLdcInsn(interfaceClassNames[i]);
					method.visitInsn(AASTORE);
				}
				for (int i = 6; i < len; i++) {
					method.visitInsn(DUP);
					method.visitIntInsn(BIPUSH, i);
					method.visitLdcInsn(interfaceClassNames[i]);
					method.visitInsn(AASTORE);
				}
				method.visitVarInsn(ALOAD, 0);
				method.visitVarInsn(ALOAD, 0);
				method.visitFieldInsn(GETFIELD, implName, "endpoint", "L"
						+ ENDPOINT_I + ";");
				method.visitLdcInsn(uri);
				method.visitMethodInsn(INVOKEINTERFACE, ENDPOINT_I,
						"getProperties",
						"(Ljava/lang/String;)Ljava/util/Dictionary;");
				method
						.visitMethodInsn(
								INVOKEINTERFACE,
								"org/osgi/framework/BundleContext",
								"registerService",
								"([Ljava/lang/String;Ljava/lang/Object;Ljava/util/Dictionary;)Lorg/osgi/framework/ServiceRegistration;");
				method
						.visitMethodInsn(INVOKEINTERFACE, ENDPOINT_I,
								"trackRegistration",
								"(Ljava/lang/String;Lorg/osgi/framework/ServiceRegistration;)V");
				method.visitVarInsn(ALOAD, 0);
				method.visitFieldInsn(GETFIELD, implName, "endpoint", "L"
						+ ENDPOINT_I + ";");
				method.visitLdcInsn(uri);
				method.visitMethodInsn(INVOKEINTERFACE, ENDPOINT_I,
						"getProperties",
						"(Ljava/lang/String;)Ljava/util/Dictionary;");
				method.visitLdcInsn(RemoteOSGiService.PRESENTATION);
				method.visitMethodInsn(INVOKEVIRTUAL, "java/util/Dictionary",
						"get", "(Ljava/lang/Object;)Ljava/lang/Object;");
				method.visitTypeInsn(CHECKCAST, "java/lang/String");
				method.visitVarInsn(ASTORE, 3);
				method.visitVarInsn(ALOAD, 3);
				Label l0 = new Label();
				method.visitJumpInsn(IFNULL, l0);
				method.visitVarInsn(ALOAD, 3);
				method.visitMethodInsn(INVOKESTATIC, "java/lang/Class",
						"forName", "(Ljava/lang/String;)Ljava/lang/Class;");
				method.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class",
						"newInstance", "()Ljava/lang/Object;");
				method.visitTypeInsn(CHECKCAST, UICOMP_I);
				method.visitVarInsn(ASTORE, 4);
				method.visitVarInsn(ALOAD, 4);
				method.visitVarInsn(ALOAD, 0);
				method.visitVarInsn(ALOAD, 1);
				method
						.visitMethodInsn(INVOKEINTERFACE, UICOMP_I,
								"initComponent",
								"(Ljava/lang/Object;Lorg/osgi/framework/BundleContext;)V");
				method.visitVarInsn(ALOAD, 1);
				method.visitLdcInsn(ServiceUIComponent.class.getName());
				method.visitVarInsn(ALOAD, 4);
				method.visitVarInsn(ALOAD, 0);
				method.visitFieldInsn(GETFIELD, implName, "endpoint", "L"
						+ ENDPOINT_I + ";");
				method.visitLdcInsn(uri);
				method.visitMethodInsn(INVOKEINTERFACE, ENDPOINT_I,
						"getPresentationProperties",
						"(Ljava/lang/String;)Ljava/util/Dictionary;");
				method
						.visitMethodInsn(
								INVOKEINTERFACE,
								"org/osgi/framework/BundleContext",
								"registerService",
								"(Ljava/lang/String;Ljava/lang/Object;Ljava/util/Dictionary;)Lorg/osgi/framework/ServiceRegistration;");
				method.visitInsn(POP);
				method.visitLabel(l0);
				if (addLifecycleSupport) {
					method.visitVarInsn(ALOAD, 0);
					method.visitVarInsn(ALOAD, 1);
					method.visitMethodInsn(INVOKEVIRTUAL, implName, "started",
							"(Lorg/osgi/framework/BundleContext;)V");
				}
				method.visitInsn(RETURN);
				method.visitMaxs(7, 5);
				method.visitEnd();
			}
			{
				method = writer.visitMethod(ACC_PUBLIC, "stop",
						"(Lorg/osgi/framework/BundleContext;)V", null,
						new String[] { "java/lang/Exception" });
				method.visitCode();
				method.visitVarInsn(ALOAD, 0);
				method.visitFieldInsn(GETFIELD, implName, "endpoint", "L"
						+ ENDPOINT_I + ";");
				method.visitLdcInsn(uri);
				method.visitMethodInsn(INVOKEINTERFACE, ENDPOINT_I,
						"untrackRegistration", "(Ljava/lang/String;)V");
				method.visitVarInsn(ALOAD, 0);
				method.visitInsn(ACONST_NULL);
				method.visitFieldInsn(PUTFIELD, implName, "endpoint", "L"
						+ ENDPOINT_I + ";");
				if (addLifecycleSupport) {
					method.visitVarInsn(ALOAD, 0);
					method.visitVarInsn(ALOAD, 1);
					method.visitMethodInsn(INVOKEVIRTUAL, implName, "stopped",
							"(Lorg/osgi/framework/BundleContext;)V");
				}
				method.visitInsn(RETURN);
				method.visitMaxs(2, 2);
				method.visitEnd();
			}
		}

		// add the interfaces to the list to be visited later
		for (int i = 0; i < interfaces.length; i++) {
			superInterfaces.add(interfaces[i]);
		}
	}

	/**
	 * @param source
	 *            source
	 * @param debug
	 *            debug
	 * @see org.objectweb.asm.ClassVisitor#visitSource(java.lang.String,
	 *      java.lang.String)
	 */
	public void visitSource(final String source, final String debug) {
		return;
	}

	/**
	 * @param owner
	 *            owner
	 * @param name
	 *            name
	 * @param desc
	 *            desc
	 * @see org.objectweb.asm.ClassVisitor#visitOuterClass(java.lang.String,
	 *      java.lang.String, java.lang.String)
	 */
	public void visitOuterClass(final String owner, final String name,
			final String desc) {
		return;
	}

	/**
	 * @param desc
	 *            desc
	 * @param visible
	 *            visible
	 * @return AnnotationVisitor
	 * @see org.objectweb.asm.ClassVisitor#visitAnnotation(java.lang.String,
	 *      boolean)
	 */
	public AnnotationVisitor visitAnnotation(final String desc,
			final boolean visible) {
		writer.visitAnnotation(checkRewriteDesc(desc), visible);
		return null;
	}

	/**
	 * @param attr
	 *            attr
	 * @see org.objectweb.asm.ClassVisitor
	 *      #visitAttribute(org.objectweb.asm.Attribute)
	 */
	public void visitAttribute(final Attribute attr) {
		writer.visitAttribute(attr);
	}

	/**
	 * @param name
	 *            name
	 * @param outerName
	 *            outerName
	 * @param innerName
	 *            innerName
	 * @param access
	 *            access
	 * @see org.objectweb.asm.ClassVisitor#visitInnerClass(java.lang.String,
	 *      java.lang.String, java.lang.String, int)
	 */
	public void visitInnerClass(final String name, final String outerName,
			final String innerName, final int access) {
		writer.visitInnerClass(checkRewrite(name), checkRewrite(outerName),
				checkRewrite(innerName), access);
	}

	/**
	 * @param access
	 *            access
	 * @param name
	 *            name
	 * @param desc
	 *            desc
	 * @param signature
	 *            signature
	 * @param value
	 *            value
	 * @return FieldVisitor
	 * @see org.objectweb.asm.ClassVisitor#visitField(int, java.lang.String,
	 *      java.lang.String, java.lang.String, java.lang.Object)
	 */
	public FieldVisitor visitField(final int access, final String name,
			final String desc, final String signature, final Object value) {
		if (name.equals("endpoint")) {
			return null;
		}
		return writer.visitField(access, name, checkRewriteDesc(desc),
				signature, value);
	}

	/**
	 * @param access
	 *            access
	 * @param name
	 *            name
	 * @param desc
	 *            desc
	 * @param signature
	 *            signature
	 * @param exceptions
	 *            exceptions
	 * @return MethodVisitor
	 * @see org.objectweb.asm.ClassVisitor#visitMethod(int, java.lang.String,
	 *      java.lang.String, java.lang.String, java.lang.String[])
	 */
	public MethodVisitor visitMethod(final int access, final String name,
			final String desc, final String signature, final String[] exceptions) {

		if (implemented.contains(name + desc)) {
			return null;
		}

		int methodAccess = access;
		if ((methodAccess & ACC_ABSTRACT) != 0) {

			final Type[] args = Type.getArgumentTypes(desc);
			boolean needsBoxing = false;

			methodAccess = (methodAccess ^ ACC_ABSTRACT);
			MethodVisitor method = writer.visitMethod(methodAccess, name, desc,
					signature, exceptions);

			method.visitVarInsn(ALOAD, 0);
			method.visitFieldInsn(GETFIELD, implName, "endpoint", "L"
					+ ENDPOINT_I + ";");
			method.visitLdcInsn(uri);
			method.visitLdcInsn(name + desc);
			if (args.length < 5) {
				method.visitInsn(ICONST[args.length]);
			} else {
				method.visitIntInsn(BIPUSH, args.length);
			}
			method.visitTypeInsn(ANEWARRAY, "java/lang/Object");
			int slot = 1;

			// boxing of primitive type arguments
			for (int i = 0; i < (args.length < 5 ? args.length : 5); i++) {
				if (args[i].getSort() == Type.ARRAY
						|| args[i].getSort() == Type.OBJECT) {
					method.visitInsn(DUP);
					method.visitInsn(ICONST[i]);
					method.visitVarInsn(ALOAD, slot);
					method.visitInsn(AASTORE);
					slot++;
				} else {
					method.visitInsn(DUP);
					method.visitInsn(ICONST[i]);
					method.visitTypeInsn(NEW,
							"ch/ethz/iks/r_osgi/types/BoxedPrimitive");
					method.visitInsn(DUP);
					method.visitVarInsn(args[i].getOpcode(ILOAD), slot);
					method.visitMethodInsn(INVOKESPECIAL,
							"ch/ethz/iks/r_osgi/types/BoxedPrimitive",
							"<init>", "(" + args[i].getDescriptor() + ")V");
					method.visitInsn(AASTORE);
					slot += args[i].getSize();
					needsBoxing = true;
				}
			}

			for (int i = 5; i < args.length; i++) {
				if (args[i].getSort() == Type.ARRAY
						|| args[i].getSort() == Type.OBJECT) {
					method.visitInsn(DUP);
					method.visitIntInsn(BIPUSH, i);
					method.visitVarInsn(ALOAD, slot);
					method.visitInsn(AASTORE);
					slot++;
				} else {
					method.visitInsn(DUP);
					method.visitIntInsn(BIPUSH, i);
					method.visitTypeInsn(NEW,
							"ch/ethz/iks/r_osgi/types/BoxedPrimitive");
					method.visitInsn(DUP);
					method.visitVarInsn(args[i].getOpcode(ILOAD), slot);
					method.visitMethodInsn(INVOKESPECIAL,
							"ch/ethz/iks/r_osgi/types/BoxedPrimitive",
							"<init>", "(" + args[i].getDescriptor() + ")V");
					method.visitInsn(AASTORE);
					slot += args[i].getSize();
					needsBoxing = true;
				}
			}
			method
					.visitMethodInsn(INVOKEINTERFACE, ENDPOINT_I,
							"invokeMethod",
							"(Ljava/lang/String;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/Object;");

			// unboxing of primitive type return values.
			final Type returnType = Type.getReturnType(desc);
			final int sort = returnType.getSort();
			switch (sort) {
			case Type.VOID:
				method.visitInsn(POP);
				method.visitInsn(RETURN);
				break;
			case Type.BOOLEAN:
			case Type.CHAR:
			case Type.SHORT:
			case Type.INT:
			case Type.LONG:
			case Type.DOUBLE:
			case Type.FLOAT:
			case Type.BYTE:
				method.visitTypeInsn(CHECKCAST, BOXED_TYPE[sort]);
				method.visitMethodInsn(INVOKEVIRTUAL, BOXED_TYPE[sort],
						UNBOX_METHOD[sort], "()" + returnType.getDescriptor());
				method.visitInsn(returnType.getOpcode(IRETURN));
				break;
			case Type.ARRAY:

				final StringBuffer a = new StringBuffer();

				final int esort = returnType.getElementType().getSort();
				if (esort < Type.ARRAY) {
					for (int i = 0; i < returnType.getDimensions(); i++) {
						a.append("[");
					}
					// primitive array
					method.visitTypeInsn(CHECKCAST, a.toString()
							+ returnType.getElementType().toString());
				} else {
					// object array
					a.append("[");
					method.visitTypeInsn(CHECKCAST, a.toString()
							+ returnType.getInternalName() + ";");
				}
				method.visitInsn(ARETURN);
				break;
			default:
				method.visitTypeInsn(CHECKCAST, returnType.getInternalName());
				method.visitInsn(ARETURN);
				break;
			}
			method.visitMaxs(
					(args.length == 0 ? 4 : 7) + (needsBoxing ? 2 : 0),
					2 + slot); // args.length);
			method.visitEnd();

			implemented.add(name + desc);
			return null;

		} else {
			// proxy method, contains code so just rewrite the code ...
			MethodVisitor method = writer.visitMethod(access, name, desc,
					signature, exceptions);
			implemented.add(name + desc);
			return new MethodRewriter(method);
		}
	}

	/**
	 * 
	 * @see org.objectweb.asm.ClassVisitor#visitEnd()
	 */
	public void visitEnd() {
		writer.visitEnd();
	}

	private final class ClassRewriter extends ClassAdapter {

		/**
		 * 
		 */
		private ClassRewriter(ClassWriter writer) {
			super(writer);
		}

		/**
		 * 
		 * @see org.objectweb.asm.ClassAdapter#visit(int, int, java.lang.String,
		 *      java.lang.String, java.lang.String, java.lang.String[])
		 */
		public void visit(final int version, final int access,
				final String name, final String signature,
				final String superName, final String[] interfaces) {
			// rewriting

			super.cv.visit(version >= V1_5 ? V1_5 : V1_2, access,
					checkRewrite(name), signature, checkRewrite(superName),
					interfaces);
		}

		/**
		 * 
		 * @see org.objectweb.asm.ClassAdapter#visitField(int, java.lang.String,
		 *      java.lang.String, java.lang.String, java.lang.Object)
		 */
		public FieldVisitor visitField(final int access, final String name,
				final String desc, final String signature, final Object value) {
			super.cv.visitField(access, checkRewrite(name),
					checkRewriteDesc(desc), signature, value);
			return null;
		}

		/**
		 * Visits the enclosing class of the class. This method must be called
		 * only if the class has an enclosing class.
		 * 
		 * @param owner
		 *            internal name of the enclosing class of the class.
		 * @param name
		 *            the name of the method that contains the class, or
		 *            <tt>null</tt> if the class is not enclosed in a method
		 *            of its enclosing class.
		 * @param desc
		 *            the descriptor of the method that contains the class, or
		 *            <tt>null</tt> if the class is not enclosed in a method
		 *            of its enclosing class.
		 */
		public void visitOuterClass(final String owner, final String name,
				final String desc) {
			super.cv.visitOuterClass(checkRewrite(owner), checkRewrite(name),
					checkRewriteDesc(desc));
		}

		/**
		 * 
		 * @see org.objectweb.asm.ClassAdapter#visitInnerClass(java.lang.String,
		 *      java.lang.String, java.lang.String, int)
		 */
		public void visitInnerClass(final String name, final String outerName,
				final String innerName, final int access) {
			super.cv.visitInnerClass(checkRewrite(name),
					checkRewrite(outerName), checkRewrite(innerName), access);
		}

		/**
		 * 
		 * @see org.objectweb.asm.ClassAdapter#visitMethod(int,
		 *      java.lang.String, java.lang.String, java.lang.String,
		 *      java.lang.String[])
		 */
		public MethodVisitor visitMethod(final int access, final String name,
				final String desc, final String signature,
				final String[] exceptions) {
			if ("<init>()V".equals(name + desc)) {
				return null;
			}
			return new MethodRewriter(super.cv.visitMethod(access, name,
					checkRewriteDesc(desc), signature, exceptions));
		}

	}

	/**
	 * 
	 * @author Jan S. Rellermeyer, ETH Zurich
	 */
	private final class MethodRewriter extends MethodAdapter {
		/**
		 * @param methodWriter
		 *            methodWriter
		 */
		private MethodRewriter(final MethodVisitor methodWriter) {
			super(methodWriter);
		}

		/**
		 * @param opcode
		 *            opcode
		 * @param desc
		 *            desc
		 * @see org.objectweb.asm.MethodVisitor#visitTypeInsn(int,
		 *      java.lang.String)
		 */
		public void visitTypeInsn(final int opcode, String desc) {
			super.mv.visitTypeInsn(opcode, checkRewrite(desc));
		}

		/**
		 * @param opcode
		 *            opcode
		 * @param owner
		 *            owner
		 * @param name
		 *            name
		 * @param desc
		 *            desc
		 * @see org.objectweb.asm.MethodVisitor#visitFieldInsn(int,
		 *      java.lang.String, java.lang.String, java.lang.String)
		 */
		public void visitFieldInsn(final int opcode, final String owner,
				final String name, final String desc) {
			super.mv.visitFieldInsn(opcode, checkRewrite(owner), name,
					checkRewriteDesc(desc));
		}

		/**
		 * @param opcode
		 *            opcode
		 * @param owner
		 *            owner
		 * @param name
		 *            name
		 * @param desc
		 *            desc
		 * @see org.objectweb.asm.MethodVisitor#visitMethodInsn(int,
		 *      java.lang.String, java.lang.String, java.lang.String)
		 */
		public void visitMethodInsn(final int opcode, final String owner,
				final String name, final String desc) {
			// rewriting
			super.mv.visitMethodInsn(opcode, checkRewrite(owner), name,
					checkRewriteDesc(desc));
		}

		/**
		 * @param desc
		 *            desc
		 * @param dims
		 *            dims
		 * @see org.objectweb.asm.MethodVisitor
		 *      #visitMultiANewArrayInsn(java.lang.String, int)
		 */
		public void visitMultiANewArrayInsn(final String desc, final int dims) {
			// rewriting
			super.mv.visitMultiANewArrayInsn(checkRewrite(desc), dims);
		}

		/**
		 * @param name
		 *            name
		 * @param desc
		 *            desc
		 * @param signature
		 *            signature
		 * @param start
		 *            start
		 * @param end
		 *            end
		 * @param index
		 *            index
		 * @see org.objectweb.asm.MethodVisitor
		 *      #visitLocalVariable(java.lang.String, java.lang.String,
		 *      java.lang.String, org.objectweb.asm.Label,
		 *      org.objectweb.asm.Label, int)
		 */
		public void visitLocalVariable(final String name, final String desc,
				final String signature, final Label start, final Label end,
				final int index) {
			// rewriting
			super.mv.visitLocalVariable(name, checkRewriteDesc(desc),
					signature, start, end, index);
		}

		/**
		 * @param desc
		 *            desc
		 * @param visible
		 *            visible
		 * @see org.objectweb.asm.MethodVisitor
		 * @return AnnotationVisitor #visitAnnotation(java.lang.String, boolean)
		 */
		public AnnotationVisitor visitAnnotation(final String desc,
				final boolean visible) {
			// rewrite
			super.mv.visitAnnotation(checkRewriteDesc(desc), visible);
			return null;
		}

	}

	/**
	 * generate a source id from IP or host name.
	 * 
	 * @param id
	 *            id
	 * @return sourceID
	 */
	private static String generateSourceID(final String id) {
		final int pos1 = id.indexOf("://");
		char[] chars = id.substring(pos1 + 3).replace('/', '_').replace(':',
				'_').toCharArray();
		StringBuffer buffer = new StringBuffer();
		for (int i = 0; i < chars.length; i++) {
			if (chars[i] == '.') {
				buffer.append('o');
				continue;
			}
			if (chars[i] > 47 && chars[i] < 58) {
				buffer.append(CHAR_TABLE[chars[i] - 48]);
				continue;
			}
			if (chars[i] == '#') {
				continue;
			}
			buffer.append(chars[i]);
		}
		return buffer.toString();
	}

	private String checkRewrite(String clazzName) {
		if (smartProxyClassNameDashed == null) {
			return clazzName;
		}
		if (clazzName == null) {
			return null;
		}
		if (clazzName.equals(smartProxyClassNameDashed)
				|| clazzName.startsWith(smartProxyClassNameDashed + "$")) {
			final String rest = clazzName.substring(smartProxyClassNameDashed
					.length());
			return implName + rest;
		} else {
			return clazzName;
		}
	}

	private String checkRewriteDesc(final String desc) {
		if (smartProxyClassNameDashed == null) {
			return desc;
		}
		if (desc == null) {
			return null;
		}
		String result = desc;
		int i = result.indexOf(smartProxyClassNameDashed + ";");
		while (i > 0) {
			int j = i + smartProxyClassNameDashed.length();
			result = result.substring(0, i) + implName + result.substring(j);
			i = result.indexOf(smartProxyClassNameDashed, j);
		}
		i = result.indexOf(smartProxyClassNameDashed + "$");
		while (i > 0) {
			int j = i + smartProxyClassNameDashed.length();
			result = result.substring(0, i) + implName + result.substring(j);
			i = result.indexOf(smartProxyClassNameDashed, j);
		}
		return result;
	}
}
