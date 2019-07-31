package org.stagemonitor.core.instrument;

import net.bytebuddy.agent.ByteBuddyAgent;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

class EhCacheAttachmentProvider implements ByteBuddyAgent.AttachmentProvider {
	private Class<?> virtualMachineClass = null;

	public EhCacheAttachmentProvider() {
		try {
			final Class<?> ehcacheAgentLoader = Class.forName("net.sf.ehcache.pool.sizeof.AgentLoader");
			// ehcache found, get virtual machine class from ehcache to work around
			// java.lang.UnsatisfiedLinkError: Native Library /usr/java/jdk1.8.0_40/jre/lib/amd64/libattach.so already loaded in another classloader
			final Field virtual_machine_attach = ehcacheAgentLoader.getDeclaredField("VIRTUAL_MACHINE_ATTACH");
			virtual_machine_attach.setAccessible(true);
			virtualMachineClass = ((Method) virtual_machine_attach.get(null)).getDeclaringClass();
		} catch (Exception e) {
			// EhCache is not available
		}
	}

	@Override
	public Accessor attempt() {
		return new Accessor() {

			@Override
			public boolean isAvailable() {
				return virtualMachineClass != null;
			}

			@Override
			public boolean isExternalAttachmentRequired() {
				return false;
			}

			@Override
			public Class<?> getVirtualMachineType() {
				return virtualMachineClass;
			}

			@Override
			public ExternalAttachment getExternalAttachment() {
				return Unavailable.INSTANCE.getExternalAttachment();
			}
		};
	}
}
