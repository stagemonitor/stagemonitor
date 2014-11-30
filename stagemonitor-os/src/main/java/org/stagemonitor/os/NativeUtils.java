package org.stagemonitor.os;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.util.List;

/**
 * Simple library class for working with JNI (Java Native Interface)
 *
 * http://frommyplayground.com/how-to-load-native-jni-library-from-jar
 *
 * @author Adam Heirnich &lt;adam@adamh.cz&gt;, http://www.adamh.cz
 */
public final class NativeUtils {

	private static final String JAVA_LIBRARY_PATH = "java.library.path";

	private NativeUtils() {
	}

	/**
	 * Adds native libs that are stored inside a jar file to the java library path.
	 *
	 * @param nativeLibs the path to the native libs in jar file
	 * @param parentDir the parent directory name the native libs should be stored to
	 * @return the folder that contains the extracted nativeLibs
	 * @throws Exception
	 */
	public static String addResourcesToLibraryPath(List<String> nativeLibs, String parentDir) throws Exception {
		StringBuilder newLibraryPath = new StringBuilder();
		if (System.getProperty(JAVA_LIBRARY_PATH) != null) {
			newLibraryPath.append(System.getProperty(JAVA_LIBRARY_PATH)).append(File.pathSeparatorChar);
		}
		final File tempDirectory = new File(System.getProperty("java.io.tmpdir"), parentDir);
		tempDirectory.mkdir();
		newLibraryPath.append(tempDirectory.getAbsolutePath());

		for (String nativeLib : nativeLibs) {
			extractFromJarToTemp(nativeLib, tempDirectory);
		}

		System.setProperty(JAVA_LIBRARY_PATH, newLibraryPath.toString());

		final Field sysPathsField = ClassLoader.class.getDeclaredField("sys_paths");
		sysPathsField.setAccessible(true);
		sysPathsField.set(null, null);
		return tempDirectory.getAbsolutePath();
	}

	/**
	 * Loads library from current JAR archive
	 *
	 * The file from JAR is copied into system temporary directory and then loaded. The temporary file is deleted after exiting.
	 * Method uses String as filename because the pathname is "abstract", not system-dependent.
	 *
	 *
	 * @param path The filename inside JAR as absolute path (beginning with '/'), e.g. /package/File.ext
	 * @param tempDirectory
	 * @throws IOException If temporary file creation or read/write operation fails
	 * @throws IllegalArgumentException If source file (param path) does not exist
	 * @throws IllegalArgumentException If the path is not absolute or if the filename is shorter than three characters (restriction of {@see File#createTempFile(java.lang.String, java.lang.String)}).
	 */
	public static void extractFromJarToTemp(String path, File tempDirectory) throws IOException {
		File temp = createTempFile(path, tempDirectory);
		if (temp == null) {
			// already extracted
			return;
		}
		writeFromJarToTempFile(path, temp);
	}

	private static File createTempFile(String path, File tempDirectory) throws IOException {
		if (!path.startsWith("/")) {
			throw new IllegalArgumentException("The path has to be absolute (start with '/').");
		}

		// Obtain filename from path
		String[] parts = path.split("/");

		// Prepare temporary file
		File temp = new File(tempDirectory, parts[parts.length - 1]);
		if (temp.exists()) {
			// already extracted
			return null;
		}
		if (!temp.createNewFile()) {
			throw new IOException("Could not create new File");
		}

		if (!temp.exists()) {
			throw new FileNotFoundException("File " + temp.getAbsolutePath() + " does not exist.");
		}
		return temp;
	}

	private static void writeFromJarToTempFile(String path, File temp) throws IOException {
		// Prepare buffer for data copying
		byte[] buffer = new byte[1024];
		int readBytes;

		// Open and check input stream
		InputStream is = NativeUtils.class.getResourceAsStream(path);
		if (is == null) {
			throw new FileNotFoundException("File " + path + " was not found inside JAR.");
		}

		// Open output stream and copy data between source file in JAR and the temporary file
		OutputStream os = new FileOutputStream(temp);
		try {
			while ((readBytes = is.read(buffer)) != -1) {
				os.write(buffer, 0, readBytes);
			}
		} finally {
			// If read/write fails, close streams safely before throwing an exception
			os.close();
			is.close();
		}
	}
}