package org.vadere.annotation.factories;

import com.google.auto.service.AutoService;

import java.io.PrintWriter;
import java.util.Set;

import javax.annotation.processing.Processor;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;

@SupportedAnnotationTypes("org.vadere.annotation.factories.DataProcessorClass")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@AutoService(Processor.class)
public class DataProcessorFactoryProcessor extends BaseFactoryProcessor {

	protected String label(Element element){
		DataProcessorClass dataProcessorClass = element.getAnnotation(DataProcessorClass.class);
		String label = dataProcessorClass.label();

		return label.isEmpty() ? element.getSimpleName().toString() : label;
	}

	protected String descr(Element element){
		DataProcessorClass dataProcessorClass = element.getAnnotation(DataProcessorClass.class);
		return dataProcessorClass.description();
	}

	@Override
	protected void addImports(Set<? extends Element> elements, PrintWriter writer) {
		writer.println("import org.vadere.simulator.projects.dataprocessing.store.DataProcessorStore;");
	}

	@Override
	protected void addLast(Set<? extends Element> elements, PrintWriter writer) {
		writer.println("	public DataProcessor<?, ?> createDataProcessor(DataProcessorStore dataProcessorStore) {");
		writer.println("		DataProcessor<?, ?> processor = getInstanceOf(dataProcessorStore.getType());");
		writer.println("		processor.setId(dataProcessorStore.getId());");
		writer.println("		processor.setAttributes(dataProcessorStore.getAttributes());");
		writer.println("		return processor;");
		writer.println("	}");
		writer.println();
		writer.println();
		writer.println("	public DataProcessor<?, ?> createDataProcessor(String type) {");
		writer.println("		DataProcessorStore dataProcessorStore = new DataProcessorStore();");
		writer.println("		dataProcessorStore.setType(type);");
		writer.println("		DataProcessor<?, ?> processor = getInstanceOf(dataProcessorStore.getType());");
		writer.println("		processor.setId(dataProcessorStore.getId());");
		writer.println("		processor.setAttributes(dataProcessorStore.getAttributes());");
		writer.println("		return processor;");
		writer.println("	}");
		writer.println();
		writer.println("	public DataProcessor<?, ?> createDataProcessor(Class type) {");
		writer.println("		return createDataProcessor(type.getCanonicalName());");
		writer.println("	}");

	}


}
