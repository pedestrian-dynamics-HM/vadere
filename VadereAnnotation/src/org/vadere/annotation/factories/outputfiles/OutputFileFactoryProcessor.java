package org.vadere.annotation.factories.outputfiles;

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

@SupportedAnnotationTypes("org.vadere.annotation.factories.outputfiles.OutputFileClass")
@SupportedSourceVersion(SourceVersion.RELEASE_11)
@AutoService(Processor.class)
public class OutputFileFactoryProcessor extends BaseFactoryProcessor {


	@Override
	protected void addImports(Set<? extends Element> elements, PrintWriter writer) {
		writer.println("import org.vadere.simulator.projects.dataprocessing.datakey.DataKey;");
		writer.println("import org.vadere.simulator.projects.dataprocessing.datakey.OutputFileMap;");
		writer.println("import org.vadere.simulator.projects.dataprocessing.store.OutputFileStore;");
		writer.println("import java.util.Arrays;");
		writer.println("import java.util.HashMap;");
	}

	@Override
	protected void addMembers(Set<? extends Element> elements, PrintWriter writer) {
	}

	@Override
	protected void addLastConstructor(Set<? extends Element> elements, PrintWriter writer) {

		for (Element e : elements) {
			OutputFileClass annotation = e.getAnnotation(OutputFileClass.class);
			String classValue;
			try {
				classValue = annotation.dataKeyMapping().getName();
			} catch (MirroredTypeException ex) {
				classValue = ex.getTypeMirror().toString();
			}

			int lastPoint = classValue.lastIndexOf('.');

			writer.append("		addMember(");
			writer.append(e.getSimpleName().toString()).append(".class, ");
			writer.append("this::").append("get").append(name(e)).append(", ");
			writer.append(quote(label(e))).append(", ");
			writer.append(quote(descr(e))).append(", ");
			writer.append(quote(classValue.substring(lastPoint + 1))).append(");");
			writer.println();
		}
	}

	@Override
	protected void addLast(Set<? extends Element> elements, PrintWriter writer) {
		writer.println("	public OutputFile<?> createOutputfile(OutputFileStore fileStore) throws ClassNotFoundException {");
		writer.println("		OutputFile<?> file = getInstanceOf(fileStore.getType());");
		writer.println("		file.setRelativeFileName(fileStore.getFilename());");
		writer.println("		file.setProcessorIds(fileStore.getProcessors());");
		writer.println("		file.setSeparator(fileStore.getSeparator());");
		writer.println("		return file;");
		writer.println("	}");
		writer.println();
		writer.println("	public OutputFile<?> createDefaultOutputfile() throws ClassNotFoundException {");
		writer.println("		OutputFileStore fileStore = new OutputFileStore();");
		writer.println("		OutputFile<?> file = getInstanceOf(fileStore.getType());");
		writer.println("		file.setSeparator(fileStore.getSeparator());");
		writer.println("		return file;");
		writer.println("	}");
		writer.println();
		writer.println("	public OutputFile<?> createOutputfile(String type) throws ClassNotFoundException {");
		writer.println("		OutputFileStore fileStore = new OutputFileStore();");
		writer.println("		fileStore.setType(type);");
		writer.println("		OutputFile<?> file = getInstanceOf(fileStore.getType());");
		writer.println("		file.setSeparator(fileStore.getSeparator());");
		writer.println("		return file;");
		writer.println("	}");
		writer.println();
		writer.println("	public OutputFile<?> createOutputfile(Class type) throws ClassNotFoundException {");
		writer.println("		return createOutputfile(type.getCanonicalName());");
		writer.println("	}");
		writer.println();
		writer.println("	public OutputFile<?> createOutputfile(Class type, Integer... processorsIds) throws ClassNotFoundException {");
		writer.println("		OutputFile<?> file = createOutputfile(type.getCanonicalName());");
		writer.println("		file.setProcessorIds(Arrays.asList(processorsIds));");
		writer.println("		return file;");
		writer.println("	}");
		writer.println();
		writer.println("	public OutputFile<?> createDefaultOutputfileByDataKey(Class<? extends DataKey<?>> keyType, Integer... processorsIds) throws ClassNotFoundException {");
		writer.println("		OutputFile<?> file = createDefaultOutputfileByDataKey(keyType);");
		writer.println("		file.setProcessorIds(Arrays.asList(processorsIds));");
		writer.println("		return file;");
		writer.println("	}");
		writer.println();
		writer.println("	public OutputFile<?> createDefaultOutputfileByDataKey(Class<? extends DataKey<?>> keyType) throws ClassNotFoundException {");
		writer.println();
		writer.println("		OutputFileMap outputFileMap = keyType.getAnnotation(OutputFileMap.class);");
		writer.println("		return createOutputfile(outputFileMap.outputFileClass());");
		writer.println("	}");
	}

	private String label(Element element) {
		OutputFileClass dataProcessorClass = element.getAnnotation(OutputFileClass.class);
		String label = dataProcessorClass.label();

		return label.isEmpty() ? element.getSimpleName().toString() : label;
	}

	private String descr(Element element) {
		OutputFileClass dataProcessorClass = element.getAnnotation(OutputFileClass.class);
		return dataProcessorClass.description();
	}
}
