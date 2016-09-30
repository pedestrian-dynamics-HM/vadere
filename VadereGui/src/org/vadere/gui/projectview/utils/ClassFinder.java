package org.vadere.gui.projectview.utils;

import org.vadere.simulator.models.MainModel;
import org.vadere.simulator.models.Model;
import org.vadere.simulator.projects.dataprocessing.outputfile.OutputFile;
import org.vadere.simulator.projects.dataprocessing.processor.DataProcessor;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.models.AttributesOSM;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.stream.Collectors;

public class ClassFinder {

	public static List<String> getAttributesNames() {
		// OSM ok for determining package name? use Object.class as tag instead?
		return getClassNamesWithTagInPackage(AttributesOSM.class.getPackage().getName(), Attributes.class);
	}

	// all MainModel classes
	public static List<String> getMainModelNames() {
		return getClassNamesWithTagInPackage(MainModel.class.getPackage().getName(), MainModel.class);
	}

	// all Model classes without the MainModel classes
	public static List<String> getModelNames() {
		List<String> modelNames = getClassNamesWithTagInPackage(Model.class.getPackage().getName(), Model.class);
		modelNames.removeAll(getMainModelNames());
		return modelNames;
	}

	// all output file classes
	public static List<Class<?>> getOutputFileClasses() {
		return findSubclassesInPackage(OutputFile.class.getPackage().getName(), OutputFile.class)
				.stream().filter(cfile -> !Modifier.isAbstract(cfile.getModifiers()))
				.collect(Collectors.toList());
	}

	public static List<Class<?>> getProcessorClasses(Type keyType) {
		return findSubclassesInPackage(DataProcessor.class.getPackage().getName(), DataProcessor.class)
				.stream()
				.filter(cproc -> !Modifier.isAbstract(cproc.getModifiers()))
				.filter(cproc -> findGenericProcessorSuperclass(cproc).getActualTypeArguments()[0].equals(keyType))
				.collect(Collectors.toList());
	}

	public static List<Class<?>> getAllProcessorClasses() {
		return findSubclassesInPackage(DataProcessor.class.getPackage().getName(), DataProcessor.class)
				.stream()
				.filter(cproc -> !Modifier.isAbstract(cproc.getModifiers()))
				.collect(Collectors.toList());
	}

	private static List<String> getClassNamesWithTagInPackage(String packageName, Class<?> baseClassOrInterface) {
		return findSubclassesInPackage(packageName, baseClassOrInterface).stream()
				.map(Class::getName)
				.collect(Collectors.toList());
	}

	private static List<Class<?>> findSubclassesInPackage(String packageName, Class<?> baseClassOrInterface) {
		try {
			return getClasses(packageName).stream()
					.filter(c -> !c.isInterface()
							&& baseClassOrInterface.isAssignableFrom(c) 
							&& isNotAnInnerClass(c))
					.collect(Collectors.toList());
		} catch (ClassNotFoundException | IOException e) {
			e.printStackTrace();
		}
		return new ArrayList<>();
	}

	private static boolean isNotAnInnerClass(Class<?> clazz) {
		return !clazz.getName().contains("$");
	}

	// below via https://dzone.com/articles/get-all-classes-within-package

	/**
	 * Scans all classes accessible from the context class loader which belong to the given package
	 * and subpackages.
	 *
	 * @param packageName The base package
	 * @return The classes
	 * @throws ClassNotFoundException
	 * @throws IOException
	 */
	private static List<Class<?>> getClasses(String packageName) throws ClassNotFoundException, IOException {
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		assert classLoader != null;
		String path = packageName.replace('.', '/');
		Enumeration<URL> resources = classLoader.getResources(path);
		List<File> dirs = new ArrayList<>();
		while (resources.hasMoreElements()) {
			URL resource = resources.nextElement();
			dirs.add(new File(resource.getFile()));
		}
		ArrayList<Class<?>> classes = new ArrayList<>();
		for (File directory : dirs) {
			classes.addAll(findClasses(directory, packageName));
		}
		return classes;
	}

	/**
	 * Recursive method used to find all classes in a given directory and subdirs.
	 *
	 * @param directory The base directory
	 * @param packageName The package name for classes found inside the base directory
	 * @return The classes
	 * @throws ClassNotFoundException
	 */
	private static List<Class<?>> findClasses(File directory, String packageName) throws ClassNotFoundException {
		List<Class<?>> classes = new ArrayList<>();
		if (!directory.exists()) {
			return classes;
		}
		File[] files = directory.listFiles();
		for (File file : files) {
			if (file.isDirectory()) {
				assert !file.getName().contains(".");
				classes.addAll(findClasses(file, packageName + "." + file.getName()));
			} else if (file.getName().endsWith(".class")) {
				classes.add(
						Class.forName(packageName + '.' + file.getName().substring(0, file.getName().length() - 6)));
			}
		}
		return classes;
	}

	private static ParameterizedType findGenericProcessorSuperclass(Class<?> c) {
		Class<?> superclass = c;

		while (!superclass.equals(Object.class)) {
			if(superclass.getSuperclass().equals(DataProcessor.class))
				return (ParameterizedType) superclass.getGenericSuperclass();

			superclass = superclass.getSuperclass();
		}

		return null;
	}
}
