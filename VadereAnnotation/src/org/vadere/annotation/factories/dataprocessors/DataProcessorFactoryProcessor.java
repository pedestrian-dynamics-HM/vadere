package org.vadere.annotation.factories.dataprocessors;

import com.google.auto.service.AutoService;

import org.vadere.annotation.factories.BaseFactoryProcessor;

import java.io.PrintWriter;
import java.util.Set;

import javax.annotation.processing.Processor;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;

@SupportedAnnotationTypes("org.vadere.annotation.factories.dataprocessors.DataProcessorClass")
@SupportedSourceVersion(SourceVersion.RELEASE_11)
@AutoService(Processor.class)
public class DataProcessorFactoryProcessor extends BaseFactoryProcessor {

	@Override
	protected void addImports(Set<? extends Element> elements, PrintWriter writer) {
		writer.println("import org.vadere.simulator.projects.dataprocessing.store.DataProcessorStore;");
	}

	@Override
	protected void addMembers(Set<? extends Element> elements, PrintWriter writer) {

	}

	@Override
	protected void addLastConstructor(Set<? extends Element> elements, PrintWriter writer) {
		for (Element e : elements) {
			writer.append("		addMember(");
			writer.append("this::").append("get").append(name(e)).append(", ");
			writer.append(quote(label(e))).append(", ");
			writer.append(quote(descr(e))).append(", ");
			writer.append(e.getSimpleName().toString()).append(".class");
			//if flags are provides here...
			String[] flags = processorFlags(e);
			if (flags.length > 0){
				writer.append(", ");
				for (int i = 0; i < flags.length -1; i++) {
					writer.append(quote(flags[i])).append(", ");
				}
				writer.append(quote(flags[flags.length-1]));
				writer.println("); ");
			} else {
				writer.println("); ");
			}

		}
	}

	@Override
	protected void addLast(Set<? extends Element> elements, PrintWriter writer) {
		writer.println("	public DataProcessor<?, ?> createDataProcessor(DataProcessorStore dataProcessorStore) throws ClassNotFoundException {");
		writer.println("		DataProcessor<?, ?> processor = getInstanceOf(dataProcessorStore.getType());");
		writer.println("		processor.setId(dataProcessorStore.getId());");
		writer.println("		processor.setAttributes(dataProcessorStore.getAttributes());");
		writer.println("		return processor;");
		writer.println("	}");
		writer.println();
		writer.println();
		writer.println("	public DataProcessor<?, ?> createDataProcessor(String type) throws ClassNotFoundException {");
		writer.println("		DataProcessorStore dataProcessorStore = new DataProcessorStore();");
		writer.println("		dataProcessorStore.setType(type);");
		writer.println("		DataProcessor<?, ?> processor = getInstanceOf(dataProcessorStore.getType());");
		writer.println("		processor.setId(dataProcessorStore.getId());");
		writer.println("		processor.setAttributes(dataProcessorStore.getAttributes());");
		writer.println("		return processor;");
		writer.println("	}");
		writer.println();
		writer.println("	public DataProcessor<?, ?> createDataProcessor(Class type) throws ClassNotFoundException {");
		writer.println("		return createDataProcessor(type.getCanonicalName());");
		writer.println("	}");

	}

	private String label(Element element) {
		DataProcessorClass dataProcessorClass = element.getAnnotation(DataProcessorClass.class);
		String label = dataProcessorClass.label();

		return label.isEmpty() ? element.getSimpleName().toString() : label;
	}

	private String descr(Element element) {
		DataProcessorClass dataProcessorClass = element.getAnnotation(DataProcessorClass.class);
		return dataProcessorClass.description();
	}

	private String[] processorFlags(Element element){
		DataProcessorClass dataProcessorClass = element.getAnnotation(DataProcessorClass.class);
		return dataProcessorClass.processorFlags();
	}

}
