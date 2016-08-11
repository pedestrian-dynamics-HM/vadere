package org.vadere.simulator.dataprocessing;

import com.google.gson.annotations.Expose;

import org.junit.Before;
import org.junit.Test;
import org.vadere.simulator.projects.dataprocessing.processors.Processor;
import org.vadere.util.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.net.URISyntaxException;

import static junit.framework.Assert.assertTrue;
import static junit.framework.TestCase.assertFalse;

public class TestProcessorSerializability {


	/**
	 * The file path to the folder that contain all Processor-Classes or
	 * that contain folder that contain Processor-Classes.
	 */
	private String pathToProcessorPackage;

	/**
	 * The package path to the folder (starting at /classes) ontain all Processor-Classes or
	 * that contain folder that contain Processor-Classes.
	 */
	private String packagePath;

	/**
	 * Checks if minimum one class is checked!
	 */
	private boolean checkClass;

	@Before
	public void setUp() throws URISyntaxException {
		URI uriToAttributePackage = Processor.class.getResource(Processor.class.getSimpleName() + ".class").toURI();
		pathToProcessorPackage =
				uriToAttributePackage.toString().replaceAll(Processor.class.getSimpleName() + ".class", "");
		packagePath = Processor.class.getProtectionDomain().getCodeSource().getLocation().getPath();
		packagePath = pathToProcessorPackage.substring(pathToProcessorPackage.lastIndexOf(packagePath))
				.replaceAll(packagePath, "").replaceAll("/", ".");

		if (packagePath.endsWith(".")) {
			packagePath = packagePath.substring(0, packagePath.length() - 1);
		}

		this.checkClass = false;
	}

	@Test
	public void testNoFinalModifier() throws IOException, ClassNotFoundException, URISyntaxException {
		File rootDirectory = new File(new URI(pathToProcessorPackage));
		checkDirectory(rootDirectory, packagePath);

		assertTrue("no class was checked, maybe the path is not correct!", checkClass);
	}

	private void checkDirectory(final File dir, final String path) throws ClassNotFoundException {

		if (dir.isDirectory() && dir.exists()) {
			for (File file : dir.listFiles()) {
				if (file.isDirectory()) {
					checkDirectory(file, path.replaceAll("/", ".") + "." + file.getName());
				} else {
					checkFile(file, path + "." + file.getName());
				}
			}
		}
	}

	private void checkFile(final File file, final String path) throws ClassNotFoundException {
		if (file.isFile() && file.exists() && path.endsWith(".class")) {
			ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
			String classPath = path.replaceAll(".class", "");
			Class clazz = classLoader.loadClass(classPath);

			if (!clazz.isEnum() && !clazz.isInterface() && hasInterface(clazz, Processor.class)) {
				for (Field field : clazz.getDeclaredFields()) {
					if (!fieldHasAnnotation(field, Expose.class)) {
						assertFalse("In " + classPath + " is " + field.getName() + " final!",
								Modifier.isFinal(field.getModifiers()));
					}
					this.checkClass = true;
				}
			}
		}
	}

	private boolean fieldHasAnnotation(final Field field, final Class<? extends Annotation> annotation) {
		return field.isAnnotationPresent(annotation);
	}

	private boolean hasInterface(Class clazz, final Class interf) {
		while (!clazz.equals(Object.class)) {
			Class<?>[] interfaces = clazz.getInterfaces();
			for (Class<?> impInterf : interfaces) {
				if (interf.equals(impInterf)) {
					return true;
				}
			}
			clazz = clazz.getSuperclass();
		}


		return false;
	}
}
