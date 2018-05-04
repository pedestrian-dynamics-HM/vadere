package org.vadere.annotation.factories;


import java.io.IOException;
import java.io.PrintWriter;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.tools.JavaFileObject;

public abstract class BaseFactoryProcessor extends AbstractProcessor {

	protected String factoryType;
	protected String factoryClassName;
	protected String factoryPackage;
	protected String[] factoryImports;

	@Override
	public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

		for (TypeElement annotation : annotations){
			//only no abstract classes...
			Set<? extends Element> annotatedElements =
					roundEnv.getElementsAnnotatedWith(annotation)
							.stream()
							.filter(e -> e.getKind().isClass()
								&& !e.getKind().equals(ElementKind.ENUM)
								&& !e.getModifiers().contains(Modifier.ABSTRACT)
									)
							.collect(Collectors.toSet());

			if (annotatedElements.isEmpty())
				continue;

			FactoryType factoryType = annotation.getAnnotation(FactoryType.class);
			assert factoryType != null;
			this.factoryType = factoryType.factoryType();
			this.factoryClassName = factoryType.factoryName();
			this.factoryPackage = factoryType.factoryPackage();
			this.factoryImports = factoryType.factoryImports();

			try {
				writeFactory(annotatedElements);
			} catch (IOException e) {
				e.printStackTrace();
			}

		}

		return true;
	}

	protected abstract String label(Element element);

	protected abstract String descr(Element element);

	protected String name(Element e){
		return e.getSimpleName().toString();
	}

	protected abstract void addImports(Set<? extends Element> elements, PrintWriter writer);
	protected abstract void addLast(Set<? extends Element> elements, PrintWriter writer);

	private void writeFactory(Set<? extends Element> elements) throws IOException {
		JavaFileObject jFile = processingEnv.getFiler().createSourceFile(factoryClassName);

		try (PrintWriter out = new PrintWriter(jFile.openWriter())) {
			out.append("package ").append(factoryPackage).append(";").println();
			out.println();
			out.println("import org.vadere.util.factory.BaseFactory;");
			for (String factoryImport : factoryImports) {
				out.append("import ").append(factoryImport).println(";");
			}
			out.println();
			for(Element e : elements){
				TypeElement p = (TypeElement)e;
				out.append("import ").append(p.getQualifiedName()).println(";");
			}
			out.println();

			//add additional import defined by subclass
			addImports(elements, out);

			out.println();
			out.println();
			out.append("public class ").append(factoryClassName).append(" extends BaseFactory<").append(factoryType).append("> {").println();
			out.println();

			Util.createSingletone(factoryClassName, out);

			out.println();
			// private constructor.
			out.append("	private ").append(factoryClassName).append("(){").println();


			out.println("// add Members to Factory");
			for (Element e : elements) {
				out.append("	addMember(");
					out.append(e.getSimpleName().toString()).append(".class, ");
					out.append("this::").append("get").append(name(e)).append(", ");
					out.append(Util.quote(label(e))).append(", ");
					out.append(Util.quote(descr(e))).append(");");
					out.println();
			}
			out.println("	}");
			out.println();

			out.println();

			out.println("// Getters");
			for (Element element : elements) {
				TypeElement p = (TypeElement)element;
				out.append("	public ").append(p.getSimpleName())
						.append(" get").append(p.getSimpleName()).append("()").println("{");
				out.append("		return new ").append(p.getSimpleName()).println("();");
				out.append("	}").println();
				out.println();
			}


			out.println("// additional methods");
			//add additional methods defined by subclass
			addLast(elements, out);

			out.println();

			out.println("}");
		}
	}
}
