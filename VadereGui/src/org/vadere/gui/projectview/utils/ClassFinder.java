package org.vadere.gui.projectview.utils;

import org.apache.commons.lang3.tuple.Pair;
import org.vadere.simulator.models.MainModel;
import org.vadere.simulator.models.Model;
import org.vadere.simulator.projects.dataprocessing.datakey.DataKey;
import org.vadere.simulator.projects.dataprocessing.datakey.OutputFileMap;
import org.vadere.simulator.projects.dataprocessing.outputfile.OutputFile;
import org.vadere.simulator.projects.dataprocessing.processor.DataProcessor;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.models.AttributesOSM;
import org.vadere.util.logging.Logger;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

@Deprecated
public class ClassFinder {

    private static Logger log = Logger.getLogger(ClassFinder.class);

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
	
	public static Map<String, List<String>> groupPackages(List<String> classNamesInPackageNotation) {
		List<String> groupNames = deriveGroupNamesFromPackageNames(classNamesInPackageNotation);
		Map<String, List<String>> groupNamesToMembers = sortClassNamesIntoGroups(classNamesInPackageNotation, groupNames);
		return groupNamesToMembers;
	}

	// all output file classes
	public static List<Class<?>> getOutputFileClasses() {
		return findSubclassesInPackage(OutputFile.class.getPackage().getName(), OutputFile.class)
				.stream().filter(cfile -> !Modifier.isAbstract(cfile.getModifiers()))
				.collect(Collectors.toList());
	}

	public static Map<String, Class> getDataKeysOutputFileRelation() {
		try {
			return getClasses(DataKey.class.getPackage().getName())
					.stream()
					.filter(c -> !Modifier.isInterface(c.getModifiers()))
					.filter(c -> DataKey.class.isAssignableFrom(c))
					.map(c -> {
						// Find corresponding outputfile class
						try {
							OutputFileMap annotation = c.getAnnotation(OutputFileMap.class);
							return Pair.of((Class) c, Optional.of(annotation.outputFileClass()));
						} catch (Exception ex) {
							ex.printStackTrace();
						}

						return null;
					})
					.filter(p -> p.getValue().isPresent())
					.map(p -> Pair.of(p.getKey(), p.getValue().get()))
					.collect(Collectors.toMap(p -> p.getKey().getSimpleName(), p -> (Class) p.getValue()));
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		return null;
	}

	public static Map<String, Class> getProcessorClassesWithNames() {
		Map<String, Class> map = new HashMap<>();
		getAllProcessorClasses().forEach(procCls -> map.put(procCls.getSimpleName(), procCls));
		return map;
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
	 * Deprecated since this method is slow if we have to access jar file, which is the case if we cup the project via vadere.jar!
	 *
	 * @param packageName The base package
	 * @return The classes
	 * @throws ClassNotFoundException
	 * @throws IOException
	 */
	@Deprecated
	private static List<Class<?>> getClasses(String packageName) throws ClassNotFoundException, IOException {
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		String path = packageName.replace('.', '/');
		List<File> dirs = new ArrayList<>();
        ArrayList<Class<?>> classes = new ArrayList<>();

		assert classLoader != null;
		Enumeration<URL> resources = classLoader.getResources(path);

		while (resources.hasMoreElements()) {
			URL url = resources.nextElement();

			/*
			 * TODO[Issue #37, Bug]: Find a better solution! The problem is that one can not easily access class-files inside a packed jar!
			 * this code runs if the project is started via a executable jar
			 * it is slow, since the whole .jar will be unpacked.
			 */
			if(url.getProtocol() == "jar") {
                JarURLConnection urlcon = (JarURLConnection) (url.openConnection());
                try (JarFile jar = urlcon.getJarFile();) {
                    Enumeration<JarEntry> entries = jar.entries();
                    while (entries.hasMoreElements()) {
                        String entry = entries.nextElement().getName();
                        if(entry.startsWith(path) && entry.endsWith(".class")) {
                            String classPath = entry.substring(0, entry.length() - 6).replace('/', '.');
                            classes.add(ClassFinder.class.forName(classPath));
                        }
                    }
                }
            } // this code is fine but will only be used if the project is started from an IDE!
            else {
                dirs.add(new File(url.getFile()));
                for (File directory : dirs) {
                    classes.addAll(findClasses(directory, packageName));
                }
            }
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
	
	private static List<String> deriveGroupNamesFromPackageNames(List<String> classNamesInPackageNotation) {
		List<String> groupNames = new ArrayList<String>();
		
		// Use characters until last dot as group names.
		for (String classNameInPackageNotation : classNamesInPackageNotation) {
			int lastDotPosition = classNameInPackageNotation.lastIndexOf(".");
			
			if (lastDotPosition >= 0) {
				String groupName = classNameInPackageNotation.substring(0, lastDotPosition);
				groupNames.add(groupName);
			}
		}
		
		return groupNames;
	}
	
	private static Map<String, List<String>> sortClassNamesIntoGroups(List<String> classNamesInPackageNotation, List<String> groupNames) {
		TreeMap<String, List<String>> groupNamesToMembers = new TreeMap<>();
		
		for (String groupName : groupNames) {
			List<String> groupMembers = classNamesInPackageNotation.stream().filter(name -> name.startsWith(groupName)).sorted().collect(Collectors.toList());
			groupNamesToMembers.put(groupName, groupMembers);
		}
		
		List<String> modelNamesWithoutPackage = classNamesInPackageNotation.stream().filter(name -> name.lastIndexOf(".") == -1).sorted().collect(Collectors.toList());
		
		if (modelNamesWithoutPackage.size() > 0) {
			groupNamesToMembers.put("...", modelNamesWithoutPackage);
		}
		
		return groupNamesToMembers;
	}
}
