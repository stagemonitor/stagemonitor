/**
 *  Copyright Terracotta, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.stagemonitor.core.instrument;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.Instrumentation;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.stagemonitor.agent.StagemonitorAgent;
import org.stagemonitor.core.util.IOUtils;

/**
 * This will try to load the agent using the Attach API of JDK6.
 * If you are on an older JDK (v5) you can still use the agent by adding the -javaagent:[pathTojar] to your VM
 * startup script
 *
 * @author Alex Snaps
 */
final class AgentLoader {

	private static final Logger LOGGER = Logger.getLogger(AgentLoader.class.getName());

	private static final String VIRTUAL_MACHINE_CLASSNAME = "com.sun.tools.attach.VirtualMachine";
	private static Method VIRTUAL_MACHINE_ATTACH;
	private static Method VIRTUAL_MACHINE_DETACH;
	private static Method VIRTUAL_MACHINE_LOAD_AGENT;

	static {
		try {
			final Class<?> ehcacheAgentLoader = Class.forName("net.sf.ehcache.pool.sizeof.AgentLoader");
			// ehcache found, get virtual machine class from ehcache to work around
			// java.lang.UnsatisfiedLinkError: Native Library /usr/java/jdk1.8.0_40/jre/lib/amd64/libattach.so already loaded in another classloader
			try {
				final Field virtual_machine_attach = ehcacheAgentLoader.getDeclaredField("VIRTUAL_MACHINE_ATTACH");
				virtual_machine_attach.setAccessible(true);
				VIRTUAL_MACHINE_ATTACH = (Method) virtual_machine_attach.get(null);
				final Field virtual_machine_detach = ehcacheAgentLoader.getDeclaredField("VIRTUAL_MACHINE_DETACH");
				virtual_machine_detach.setAccessible(true);
				VIRTUAL_MACHINE_DETACH = (Method) virtual_machine_detach.get(null);
				final Field virtual_machine_load_agent = ehcacheAgentLoader.getDeclaredField("VIRTUAL_MACHINE_LOAD_AGENT");
				virtual_machine_load_agent.setAccessible(true);
				VIRTUAL_MACHINE_LOAD_AGENT = (Method) virtual_machine_load_agent.get(null);
			} catch (Exception e) {
				LOGGER.log(Level.WARNING, e.getMessage(), e);
			}
		} catch (ClassNotFoundException e) {
			// ehcache not found
			Method attach = null;
			Method detach = null;
			Method loadAgent = null;
			try {
				Class<?> virtualMachineClass = getVirtualMachineClass();
				attach = virtualMachineClass.getMethod("attach", String.class);
				detach = virtualMachineClass.getMethod("detach");
				loadAgent = virtualMachineClass.getMethod("loadAgent", String.class);
			} catch (Throwable t) {
				LOGGER.info("Unavailable or unrecognised attach API : " + t.toString());
			}
			VIRTUAL_MACHINE_ATTACH = attach;
			VIRTUAL_MACHINE_DETACH = detach;
			VIRTUAL_MACHINE_LOAD_AGENT = loadAgent;
		}
	}

	private static Class<?> getVirtualMachineClass() throws ClassNotFoundException {
		try {
			return AccessController.doPrivileged(new PrivilegedExceptionAction<Class<?>>() {
				public Class<?> run() throws Exception {
					try {
						return ClassLoader.getSystemClassLoader().loadClass(VIRTUAL_MACHINE_CLASSNAME);
					} catch (ClassNotFoundException cnfe) {
						for (File jar : getPossibleToolsJars()) {
							try {
								Class<?> vmClass = new URLClassLoader(new URL[]{jar.toURI().toURL()}).loadClass(VIRTUAL_MACHINE_CLASSNAME);
								LOGGER.info("Located valid 'tools.jar' at " + jar);
								return vmClass;
							} catch (Throwable t) {
								LOGGER.info("Exception while loading tools.jar from '" + jar + "': " + t);
							}
						}
						throw new ClassNotFoundException(VIRTUAL_MACHINE_CLASSNAME);
					}
				}
			});
		} catch (PrivilegedActionException pae) {
			Throwable actual = pae.getCause();
			if (actual instanceof ClassNotFoundException) {
				throw (ClassNotFoundException) actual;
			}
			throw new AssertionError("Unexpected checked exception : " + actual);
		}
	}

	private static List<File> getPossibleToolsJars() {
		List<File> jars = new ArrayList<File>();

		File javaHome = new File(System.getProperty("java.home"));
		File jreSourced = new File(javaHome, "lib/tools.jar");
		if (jreSourced.exists()) {
			jars.add(jreSourced);
		}
		if ("jre".equals(javaHome.getName())) {
			File jdkHome = new File(javaHome, "../");
			File jdkSourced = new File(jdkHome, "lib/tools.jar");
			if (jdkSourced.exists()) {
				jars.add(jdkSourced);
			}
		}
		return jars;
	}

	/**
	 * Attempts to load the agent through the Attach API
	 * @return true if agent was loaded (which could have happened thought the -javaagent switch)
	 */
	static Instrumentation loadAgent() throws Exception {
		synchronized (AgentLoader.class.getName().intern()) {
			if (StagemonitorAgent.getInstrumentation() == null && VIRTUAL_MACHINE_LOAD_AGENT != null) {
				warnIfOSX();
				String name = ManagementFactory.getRuntimeMXBean().getName();
				Object vm = VIRTUAL_MACHINE_ATTACH.invoke(null, name.substring(0, name.indexOf('@')));
				try {
					File agent = getAgentFile();
					LOGGER.info("Trying to load agent @ " + agent);
					if (agent != null) {
						VIRTUAL_MACHINE_LOAD_AGENT.invoke(vm, agent.getAbsolutePath());
					}
				} finally {
					VIRTUAL_MACHINE_DETACH.invoke(vm);
				}
			}
			return StagemonitorAgent.getInstrumentation();
		}
	}

	private static void warnIfOSX() {
		if (isOSX() && System.getProperty("java.io.tmpdir") != null) {
			LOGGER.warning("Loading the SizeOfAgent will probably fail, as you are running on Apple OS X and have a value set for java.io.tmpdir\n" +
					"They both result in a bug, not yet fixed by Apple, that won't let us attach to the VM and load the agent.\n" +
					"Most probably, you'll also get a full thread-dump after this because of the failure... Nothing to worry about!\n" +
					"You can load the agent with the command line argument -javaagent:/path/to/stagemonitor-javaagent-<version>.jar");
		}
	}

	private static File getAgentFile() throws IOException, URISyntaxException {
		// first try to get stagemonitor-javaagent-<version>.jar
		File agentFile = new File(StagemonitorAgent.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
		if (agentFile.getPath().endsWith(".jar") && agentFile.isFile()) {
			return agentFile;
		}
		// fall back to stagemonitor-agent.jar which is inside stagemonitor-javaagent-<version>.jar
		agentFile = getAgentJarFromResource();
		if (agentFile != null) {
			// the agent jar resource is only available in stagemonitor-javaagent-<version>.jar
			// (see stagemonitor-javaagen/build.gradle) so during development it is not available
			return agentFile;
		}

		// the last fallback is to dynamically create the jar
		// this works during development, but for some obscure reason it does not work for example with tomcat
		// the Attach Listener fails wich a ClassNotFoundException
		return dynamicallyCreateAgentJar();
	}

	private static File getAgentJarFromResource() throws IOException {
		URL agent = StagemonitorAgent.class.getClassLoader().getResource("stagemonitor-agent.jar");
		if (agent == null) {
			return null;
		} else if (agent.getProtocol().equals("file")) {
			return new File(agent.getFile());
		} else {
			File temp = File.createTempFile("stagemonitor-agent", ".jar");
			try {
				FileOutputStream fout = new FileOutputStream(temp);
				try {
					InputStream in = agent.openStream();
					try {
						byte[] buffer = new byte[1024];
						while (true) {
							int read = in.read(buffer);
							if (read < 0) {
								break;
							} else {
								fout.write(buffer, 0, read);
							}
						}
					} finally {
						in.close();
					}
				} finally {
					fout.close();
				}
			} finally {
				temp.deleteOnExit();
			}
			LOGGER.info("Extracted agent jar to temporary file " + temp);
			return temp;
		}
	}

	private static File dynamicallyCreateAgentJar() throws IOException {
		File temp = File.createTempFile("stagemonitor-agent", ".jar");
		try {
			JarOutputStream out = new JarOutputStream(new FileOutputStream(temp), getManifest(StagemonitorAgent.class.getName()));
			try {
				final String agentLocation = '/' + StagemonitorAgent.class.getName().replace('.', '/') + ".class";
				InputStream in = StagemonitorAgent.class.getResourceAsStream(agentLocation);
				out.putNextEntry(new JarEntry(agentLocation));
				IOUtils.copy(in, out);
			} finally {
				out.close();
			}
		} finally {
			temp.deleteOnExit();
		}
		LOGGER.info("Extracted agent jar to temporary file " + temp);
		return temp;
	}

	private static Manifest getManifest(String agentClassName) {
		Manifest manifest = new Manifest();
		Attributes mainAttributes = manifest.getMainAttributes();
		mainAttributes.put(Attributes.Name.MANIFEST_VERSION, "1.0");
		mainAttributes.put(new Attributes.Name("Agent-Class"), agentClassName);
		mainAttributes.put(new Attributes.Name("Can-Retransform-Classes"), "true");
		mainAttributes.put(new Attributes.Name("Can-Redefine-Classes"), "true");
		return manifest;
	}

	/**
	 * Return true if the VM's vendor is Apple
	 * @return true, if OS X
	 */
	public static boolean isOSX() {
		final String vendor = System.getProperty("java.vm.vendor");
		return vendor != null && vendor.startsWith("Apple");
	}

}
