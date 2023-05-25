package org.vadere.annotation.factories;


import java.io.IOException;
import java.io.PrintWriter;
import java.util.Set;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.JavaFileObject;

/**
 * Implements the default layout of the java source file.
 */
public abstract class BaseFactoryProcessor extends AbstractFactoryProcessor {

	@Override
	protected void writeFactory(Set<? extends Element> elements) throws IOException {
		JavaFileObject jFile = processingEnv.getFiler().createSourceFile(factoryClassName);

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
			out.println();

			out.println("	// Getters");
			buildGetters(elements, out);

			addLast(elements, out);

			out.println();
			out.println("}");
		}
	}
}
