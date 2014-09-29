package org.stagemonitor.os;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Simple library class for working with JNI (Java Native Interface)
 *
 * @see http://frommyplayground.com/how-to-load-native-jni-library-from-jar
 *
 * @author Adam Heirnich &lt;adam@adamh.cz&gt;, http://www.adamh.cz
 */
public class NativeUtils {

	/**
	 * Private constructor - this class will never be instanced
	 */
	private NativeUtils() {
	}

	public static String getLibraryPath(List<String> nativeLibs) throws IOException {
		List<String> libraryPath = new ArrayList<String>();
		if (System.getProperty("java.library.path") != null) {
			libraryPath.add(System.getProperty("java.library.path"));
		}
		for (String nativeLib : nativeLibs) {
			libraryPath.add(loadLibraryFromJar(nativeLib));
		}
		StringBuilder sb = new StringBuilder();

		for (Iterator<String> iterator = libraryPath.iterator(); iterator.hasNext(); ) {
			sb.append(iterator.next());
			if (iterator.hasNext()) {
				sb.append(';');
			}
		}
		return sb.toString();
	}

	/**
	 * Loads library from current JAR archive
	 *
	 * The file from JAR is copied into system temporary directory and then loaded. The temporary file is deleted after exiting.
	 * Method uses String as filename because the pathname is "abstract", not system-dependent.
	 *
	 * @param path The filename inside JAR as absolute path (beginning with '/'), e.g. /package/File.ext
	 * @throws IOException If temporary file creation or read/write operation fails
	 * @throws IllegalArgumentException If source file (param path) does not exist
	 * @throws IllegalArgumentException If the path is not absolute or if the filename is shorter than three characters (restriction of {@see File#createTempFile(java.lang.String, java.lang.String)}).
	 */
	public static String loadLibraryFromJar(String path) throws IOException {

		if (!path.startsWith("/")) {
			throw new IllegalArgumentException("The path has to be absolute (start with '/').");
		}

		// Obtain filename from path
		String[] parts = path.split("/");
		String filename = (parts.length > 1) ? parts[parts.length - 1] : null;


		// Prepare temporary file
		File temp = new File(createTempDirectory(), filename);
		System.out.println(temp.getAbsolutePath());
		if (!temp.createNewFile()) {
			throw new IOException("Could not create new File");
		}
		temp.deleteOnExit();

		if (!temp.exists()) {
			throw new FileNotFoundException("File " + temp.getAbsolutePath() + " does not exist.");
		}

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

		return temp.getAbsolutePath();

	}

	public static File createTempDirectory() throws IOException {
		final File temp = File.createTempFile("temp", Long.toString(System.nanoTime()));

		if(!temp.delete()) {
			throw new IOException("Could not delete temp file: " + temp.getAbsolutePath());
		}

		if(!temp.mkdir()) {
			throw new IOException("Could not create temp directory: " + temp.getAbsolutePath());
		}

		return temp;
	}
}