package org.stagemonitor.core.instrument;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.ConcurrentMap;
import java.util.jar.JarFile;

import org.stagemonitor.core.util.IOUtils;

/**
 * This class uses a map which is injected into the bootstrap classpath. It is used to share objects between classloaders.
 * <p/>
 * The reason that the org.stagemonitor.dispatcher.Dispatcher class can't be used directly is that JBoss' ModuleClassLoader
 * is not kind enough to load the class, even if it is {@link Instrumentation#appendToBootstrapClassLoaderSearch(JarFile)}.
 * So the workaround is to reflectively load the class and {@link #reflectivleyGetDispatcherMap(Class)}.
 */
public class Dispatcher {

	private static final String DISPATCHER_CLASS_NAME = "org.stagemonitor.dispatcher.Dispatcher";
	private static final String DISPATCHER_CLASS_LOCATION = "org/stagemonitor/dispatcher/Dispatcher.class";

	private static ConcurrentMap<String, Object> values;

	/**
	 * This is the byte[] presentation of the org.stagemonitor.dispatcher.Dispatcher class
	 * <p/>
	 * It is needed to insert a {@link javassist.ByteArrayClassPath} to Javassist's {@link javassist.ClassPool}
	 * because Javassist does not "see" classes which are added via {@link Instrumentation#appendToBootstrapClassLoaderSearch(JarFile)}.
	 * This is because even though the added classes can be loaded via {@link ClassLoader#loadClass(String)} they can't
	 * be resolved as a {@link InputStream} via {@link ClassLoader#getResourceAsStream(String)}.
	 */
	private static byte[] dispatcherClassAsByteArray;

	public static void init(Instrumentation instrumentation) throws IOException, ClassNotFoundException,
			NoSuchMethodException, InvocationTargetException, IllegalAccessException {
		final File tempDispatcherJar = createTempDispatcherJar();
		final JarFile jarfile = new JarFile(tempDispatcherJar);
		dispatcherClassAsByteArray = IOUtils.readToBytes(jarfile.getInputStream(jarfile.getJarEntry(DISPATCHER_CLASS_LOCATION)));
		Class<?> dispatcher = ensureDispatcherIsAppendedToBootstrapClasspath(instrumentation, jarfile);
		values = reflectivleyGetDispatcherMap(dispatcher);
	}

	private static Class<?> ensureDispatcherIsAppendedToBootstrapClasspath(Instrumentation instrumentation, JarFile jarfile)
			throws ClassNotFoundException {
		final ClassLoader bootstrapClassloader = ClassLoader.getSystemClassLoader().getParent();
		try {
			return bootstrapClassloader.loadClass(DISPATCHER_CLASS_NAME);
			// already injected
		} catch (ClassNotFoundException e) {
			instrumentation.appendToBootstrapClassLoaderSearch(jarfile);
			return bootstrapClassloader.loadClass(DISPATCHER_CLASS_NAME);
		}
	}

	private static File createTempDispatcherJar() throws IOException {
		final InputStream input = MainStagemonitorClassFileTransformer.class.getClassLoader()
				.getResourceAsStream("stagemonitor-dispatcher.jar.gradlePleaseDontExtract");
		final File tempDispatcherJar = File.createTempFile("stagemonitor-dispatcher", ".jar");
		tempDispatcherJar.deleteOnExit();
		IOUtils.copy(input, new FileOutputStream(tempDispatcherJar));
		return tempDispatcherJar;
	}

	@SuppressWarnings("unchecked")
	private static ConcurrentMap<String, Object> reflectivleyGetDispatcherMap(Class<?> dispatcher) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
		return (ConcurrentMap<String, Object>) dispatcher.getMethod("getValues").invoke(null);
	}

	/**
	 * Add a value to the shared map
	 *
	 * @param key   the key that can be used to retrieve the value via {@link #get(String)}
	 * @param value the object to share
	 */
	public static void put(String key, Object value) {
		values.put(key, value);
	}

	/**
	 * Gets a shared value by it's key
	 * <p/>
	 * Automatically casts the value to the desired type
	 *
	 * @param key the key
	 * @param <T> the type of the value
	 * @return the shared value
	 */
	@SuppressWarnings("unchecked")
	public static <T> T get(String key) {
		return (T) values.get(key);
	}


	/**
	 * Gets a shared value by it's key
	 * <p/>
	 * Automatically casts the value to the desired type
	 *
	 * @param key        the key
	 * @param valueClass the class of the value
	 * @param <T>        the type of the value
	 * @return the shared value
	 */
	@SuppressWarnings("unchecked")
	public static <T> T get(String key, Class<T> valueClass) {
		return (T) values.get(key);
	}

	/**
	 * Gets a shared value by it's key
	 *
	 * @param key the key
	 * @return the shared value
	 */
	public static Object getObject(String key) {
		return values.get(key);
	}

	/**
	 * Returns the underlying {@link ConcurrentMap}
	 * @return the underlying {@link ConcurrentMap}
	 */
	public static ConcurrentMap<String, Object> getValues() {
		return values;
	}

	public static byte[] getDispatcherClassAsByteArray() {
		return dispatcherClassAsByteArray;
	}
}

