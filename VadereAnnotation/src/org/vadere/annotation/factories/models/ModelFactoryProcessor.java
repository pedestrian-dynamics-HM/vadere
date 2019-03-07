package org.vadere.annotation.factories.models;

import com.google.auto.service.AutoService;

import org.vadere.annotation.factories.AbstractFactoryProcessor;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.processing.Processor;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.JavaFileObject;

@SupportedAnnotationTypes({"org.vadere.annotation.factories.models.ModelClass"})
@SupportedSourceVersion(SourceVersion.RELEASE_11)
@AutoService(Processor.class)
public class ModelFactoryProcessor extends AbstractFactoryProcessor {

	private List<TypeElement> mainModels;
	private HashMap<String, LinkedList<TypeElement>> subModels;

	@Override
	protected void addImports(Set<? extends Element> elements, PrintWriter writer) {
		writer.println("import java.util.HashMap;");
		writer.println("import java.util.List;");
		writer.println("import java.util.LinkedList;");
		writer.println("import java.util.List;");
	}

	@Override
	protected void addMembers(Set<? extends Element> elements, PrintWriter writer) {
	}

	@Override
	protected void addLastConstructor(Set<? extends Element> elements, PrintWriter writer) {
		writer.println("		HashMap<String,Class> subModelMap;");

		for (TypeElement e : mainModels) {
			writer.append("		mainModels.put(")
					.append(quote(e.getQualifiedName().toString())).append(", ")
					.append(e.getSimpleName().toString()).append(".class")
					.append(");").println();
		}

		subModels.entrySet().forEach(entry -> {
			String packageName = entry.getKey();
			writer.println();
			writer.append("		//").println(packageName);
			writer.println("		subModelMap = new HashMap<>();");
			entry.getValue().forEach(e -> {
				writer.append("		subModelMap.put(")
						.append(quote(e.getQualifiedName().toString())).append(", ")
						.append(e.getSimpleName().toString()).append(".class")
						.append(");").println();
			});
			writer.append("		models.put(").append(quote(packageName)).println(", subModelMap);");
		});
	}

	@Override
	protected void addLast(Set<? extends Element> elements, PrintWriter writer) {
	}

	@Override
	protected void writeFactory(Set<? extends Element> elements) throws IOException {
		JavaFileObject jFile = processingEnv.getFiler().createSourceFile(factoryClassName);

		mainModels = getMainModels(elements);
		subModels = getSubModelMap(elements);

		try (PrintWriter out = new PrintWriter(jFile.openWriter())) {
			out.append("package ").append(factoryPackage).append(";").println();
			out.println();
			// Add Imports start
			for (String factoryImport : factoryImports) {
				out.append("import ").append(factoryImport).println(";");
			}
			out.println();
			for (Element e : elements) {
				TypeElement p = (TypeElement) e;
				out.append("import ").append(p.getQualifiedName()).println(";");
			}
			out.println();

			//add additional import defined by subclass
			addImports(elements, out);
			//Add Import ends

			out.println();
			out.println();

			buildClass(elements, out);

			out.println();
			addMembers(elements, out);
			out.println();

			createSingletone(factoryClassName, out);

			out.println();
			// private constructor.
			out.append("	private ").append(factoryClassName).append("(){").println();
			out.println();
			addLastConstructor(elements, out);
			out.println("	}");

			out.println();
			out.println("}");
		}
	}

	/**
	 * Filter MainModels based on {@link ModelClass#isMainModel()}
	 */
	private List<TypeElement> getMainModels(Set<? extends Element> models) {
		return models.stream()
				.filter(model -> model.getAnnotation(ModelClass.class).isMainModel())
				.map(model -> (TypeElement) model)
				.collect(Collectors.toList());
	}

	/**
	 * Filter Models based on {@link ModelClass#isMainModel()} and group them based on
	 * their package name.
	 */
	private HashMap<String, LinkedList<TypeElement>> getSubModelMap(Set<? extends Element> models) {
		HashMap<String, LinkedList<TypeElement>> out = new HashMap<>();
		models.stream()
				.filter(model -> !model.getAnnotation(ModelClass.class).isMainModel())
				.map(model -> (TypeElement) model)
				.forEach(modelType -> {
					String qualifiedName = modelType.getQualifiedName().toString();
					String packageName = qualifiedName.substring(0, qualifiedName.lastIndexOf("."));

					if (out.containsKey(packageName)) {
						LinkedList<TypeElement> list = out.get(packageName);
						list.add(modelType);
					} else {
						LinkedList<TypeElement> list = new LinkedList<>();
						list.add(modelType);
						out.put(packageName, list);
					}
				});
		return out;
	}
}
