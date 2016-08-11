package org.vadere.state.attributes;

import org.junit.Before;
import org.junit.Test;
import org.vadere.state.attributes.Attributes;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.net.URISyntaxException;

import static junit.framework.Assert.assertTrue;
import static junit.framework.TestCase.assertFalse;


/**
 * Checks that every Attribute is serializable.
 * All classes in the attribute package (and below that package) will be included.
 *
 */
public class TestAttributesSerializability {

	/**
	 * The packagePath to the folder that contain all Attribute-Classes or
	 * that contain folder that contain Attribute-Classes.
	 */
	private String pathToAttributePackage;

	/**
	 * The package packagePath to the folder (starting at /classes) ontain all Attribute-Classes or
	 * that contain folder that contain Attribute-Classes.
	 */
	private String packagePath;

	/**
	 * Checks if minimum one class is checked.
	 */
	private boolean checkClass;

	@Before
	public void setUp() throws URISyntaxException {
		URI uriToAttributePackage = Attributes.class.getResource(Attributes.class.getSimpleName() + ".class").toURI();
		pathToAttributePackage =
				uriToAttributePackage.toString().replaceAll(Attributes.class.getSimpleName() + ".class", "");
		packagePath = Attributes.class.getProtectionDomain().getCodeSource().getLocation().getPath();
		packagePath = pathToAttributePackage.toString().substring(pathToAttributePackage.lastIndexOf(packagePath))
				.replaceAll(packagePath, "").replaceAll("/", ".");

		if (packagePath.endsWith(".")) {
			packagePath = packagePath.substring(0, packagePath.length() - 1);
		}

		this.checkClass = false;
	}

	@Test
	public void testNoFinalModifier() throws IOException, ClassNotFoundException, URISyntaxException {
		File rootDirectory = new File(new URI(pathToAttributePackage));
		checkDirectory(rootDirectory, packagePath);

		assertTrue("no class was checked, maybe the path is not correct!", checkClass);
	}

	private void checkDirectory(final File dir, final String path) throws ClassNotFoundException {
		if (dir.isDirectory() && dir.exists()) {
			for (File file : dir.listFiles()) {
				if (file.isDirectory()) {
					checkDirectory(file, path + "." + file.getName());
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
			Class<?> clazz = classLoader.loadClass(classPath);

			if (!clazz.isEnum() && !clazz.isInterface() && isSubclassOrSame(clazz, Attributes.class)) {
				for (Field field : clazz.getDeclaredFields()) {
					int fieldModifier = field.getModifiers();
					assertFalse("In " + classPath + " is " + field.getName() + " final!",
							Modifier.isFinal(fieldModifier)
									&& !Modifier.isStatic(fieldModifier)
									&& !Modifier.isTransient(fieldModifier));
				}
				this.checkClass = true;
			}
		}
	}

	private boolean isSubclassOrSame(Class<?> clazz, final Class<?> superclass) {
		return superclass.isAssignableFrom(clazz);
	}
}
