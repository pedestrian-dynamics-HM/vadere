package org.vadere.annotation.factories.attributes;

import com.google.auto.service.AutoService;

import org.vadere.annotation.factories.BaseFactoryProcessor;

import java.io.PrintWriter;
import java.util.Set;

import javax.annotation.processing.Processor;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;


@SupportedAnnotationTypes("org.vadere.annotation.factories.attributes.ModelAttributeClass")
@SupportedSourceVersion(SourceVersion.RELEASE_11)
@AutoService(Processor.class)
public class BaseAttributeFactoryProcessor extends BaseFactoryProcessor {


	@Override
	protected void addImports(Set<? extends Element> elements, PrintWriter writer) {

	}

	@Override
	protected void addMembers(Set<? extends Element> elements, PrintWriter writer) {

	}

	@Override
	protected void addLastConstructor(Set<? extends Element> elements, PrintWriter writer) {
		for (Element e : elements) {
			TypeElement te = (TypeElement) e;
			String className = te.getSimpleName().toString();
			writer.append("		addMember(");
			writer.append(className).append(".class, ");
			writer.append("this::").append("get").append(name(e)).println(");");
		}
	}

	@Override
	protected void addLast(Set<? extends Element> elements, PrintWriter writer) {

	}

}
