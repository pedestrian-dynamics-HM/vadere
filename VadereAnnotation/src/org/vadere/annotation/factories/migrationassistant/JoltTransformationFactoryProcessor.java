package org.vadere.annotation.factories.migrationassistant;

import com.google.auto.service.AutoService;

import org.vadere.annotation.factories.BaseFactoryProcessor;

import java.io.PrintWriter;
import java.util.Set;

import javax.annotation.processing.Processor;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.type.MirroredTypeException;

@SupportedAnnotationTypes("org.vadere.annotation.factories.migrationassistant.MigrationTransformation")
@SupportedSourceVersion(SourceVersion.RELEASE_11)
@AutoService(Processor.class)
public class JoltTransformationFactoryProcessor extends BaseFactoryProcessor {
	@Override
	protected void addImports(Set<? extends Element> elements, PrintWriter writer) {
	}

	@Override
	protected void addMembers(Set<? extends Element> elements, PrintWriter writer) {

	}

	@Override
	protected void addLastConstructor(Set<? extends Element> elements, PrintWriter writer) {
		for (Element e : elements) {
			MigrationTransformation annotation = e.getAnnotation(MigrationTransformation.class);
			String versionLabel;
			try {
				versionLabel = annotation.targetVersionLabel();
			} catch (MirroredTypeException ex) {
				versionLabel = ex.getTypeMirror().toString();
			}
			writer.append("		addMember(");
			writer.append(quote(versionLabel)).append(", ");
			writer.append(e.getSimpleName().toString()).append(".class, ");
			writer.append("this::").append("get").append(name(e)).append(");");
			writer.println();
		}
	}

	@Override
	protected void addLast(Set<? extends Element> elements, PrintWriter writer) {

	}
}
