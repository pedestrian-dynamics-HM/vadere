package org.vadere.annotation.factories;

import com.google.auto.service.AutoService;

import java.io.PrintWriter;
import java.util.Set;

import javax.annotation.processing.Processor;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.MirroredTypeException;

@SupportedAnnotationTypes("org.vadere.annotation.factories.OutputFileClass")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@AutoService(Processor.class)
public class OutputFileFactoryProcessor extends BaseFactoryProcessor {

	public OutputFileFactoryProcessor(){
		baseClass = "OutputFileBaseFactory";
	}

	protected String label(Element element){
		OutputFileClass dataProcessorClass = element.getAnnotation(OutputFileClass.class);
		String label = dataProcessorClass.label();

		return label.isEmpty() ? element.getSimpleName().toString() : label;
	}

	protected String descr(Element element){
		OutputFileClass dataProcessorClass = element.getAnnotation(OutputFileClass.class);
		return dataProcessorClass.description();
	}

	@Override
	protected void addImports(Set<? extends Element> elements, PrintWriter writer) {
		writer.println("import org.vadere.simulator.projects.dataprocessing.datakey.DataKey;");
		writer.println("import org.vadere.simulator.projects.dataprocessing.datakey.OutputFileMap;");
		writer.println("import org.vadere.simulator.projects.dataprocessing.store.OutputFileStore;");
		writer.println("import org.vadere.util.factory.OutputFileBaseFactory;");
		writer.println("import java.util.Arrays;");
		writer.println("import java.util.HashMap;");
	}


	@Override
	protected void addLastConstructor(Set<? extends Element> elements, PrintWriter writer) {

		for (Element e : elements) {
			OutputFileClass annotation = e.getAnnotation(OutputFileClass.class);
			String classValue;
			try{
				classValue =annotation.dataKeyMapping().getName();
			} catch (MirroredTypeException ex){
				classValue = ex.getTypeMirror().toString();
			}
			int lastPoint = classValue.lastIndexOf('.');
			TypeElement p = (TypeElement)e;
			writer.append("		addExistingKey(")
					.append(Util.quote(classValue.substring(lastPoint+1))).append(", ")
					.append(Util.quote(p.getQualifiedName().toString()))
					.println(");");
		}
	}

	@Override
	protected void addLast(Set<? extends Element> elements, PrintWriter writer) {
		writer.println("	public OutputFile<?> createOutputfile(OutputFileStore fileStore) {");
		writer.println("		OutputFile<?> file = getInstanceOf(fileStore.getType());");
		writer.println("		file.setRelativeFileName(fileStore.getFilename());");
		writer.println("		file.setProcessorIds(fileStore.getProcessors());");
		writer.println("		file.setSeparator(fileStore.getSeparator());");
		writer.println("		return file;");
		writer.println("	}");
		writer.println();
		writer.println("	public OutputFile<?> createDefaultOutputfile() {");
		writer.println("		OutputFileStore fileStore = new OutputFileStore();");
		writer.println("		OutputFile<?> file = getInstanceOf(fileStore.getType());");
		writer.println("		file.setSeparator(fileStore.getSeparator());");
		writer.println("		return file;");
		writer.println("	}");
		writer.println();
		writer.println("	public OutputFile<?> createOutputfile(String type) {");
		writer.println("		OutputFileStore fileStore = new OutputFileStore();");
		writer.println("		fileStore.setType(type);");
		writer.println("		OutputFile<?> file = getInstanceOf(fileStore.getType());");
		writer.println("		file.setSeparator(fileStore.getSeparator());");
		writer.println("		return file;");
		writer.println("	}");
		writer.println();
		writer.println("	public OutputFile<?> createOutputfile(Class type) {");
		writer.println("		return createOutputfile(type.getCanonicalName());");
		writer.println("	}");
		writer.println();
		writer.println("	public OutputFile<?> createOutputfile(Class type, Integer... processorsIds) {");
		writer.println("		OutputFile<?> file = createOutputfile(type.getCanonicalName());");
		writer.println("		file.setProcessorIds(Arrays.asList(processorsIds));");
		writer.println("		return file;");
		writer.println("	}");
		writer.println();
		writer.println("	public OutputFile<?> createDefaultOutputfileByDataKey(Class<? extends DataKey<?>> keyType, Integer... processorsIds) {");
		writer.println("		OutputFile<?> file = createDefaultOutputfileByDataKey(keyType);");
		writer.println("		file.setProcessorIds(Arrays.asList(processorsIds));");
		writer.println("		return file;");
		writer.println("	}");
		writer.println();
		writer.println("	public OutputFile<?> createDefaultOutputfileByDataKey(Class<? extends DataKey<?>> keyType) {");
		writer.println();
		writer.println("		OutputFileMap outputFileMap = keyType.getAnnotation(OutputFileMap.class);");
		writer.println("		return createOutputfile(outputFileMap.outputFileClass());");
		writer.println("	}");
	}

	@Override
	protected void addMembers(Set<? extends Element> elements, PrintWriter writer) {

	}


}
