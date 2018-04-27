package org.vadere.annotation.factories.processor;

import com.google.auto.service.AutoService;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.tools.JavaFileObject;

@SupportedAnnotationTypes("org.vadere.annotation.factories.processor.*")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@AutoService(Processor.class)
public class DataProcessorFactoryProcessor extends AbstractProcessor {
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

			try {
				writeDataProcessorFactory("org.vadere.simulator.projects.dataprocessing.processor.DataProcessorFactory3", annotatedElements);
			} catch (IOException e) {
				e.printStackTrace();
			}

		}

		return true;
	}

	private void writeDataProcessorFactory2(String className, Set<? extends Element> dataProcessors) throws IOException {
		String packageName = null;
		int lastDot = className.lastIndexOf('.');
		if (lastDot > 0)
			packageName = className.substring(0, lastDot);

		String clazz = className.substring(lastDot+1);
		JavaFileObject jFile = processingEnv.getFiler().createSourceFile(clazz);

		try (PrintWriter out = new PrintWriter(jFile.openWriter())){

			out.append("package ").append(packageName).println(";");
			out.println();

			out.append("public class ").append(clazz).println(" {");
			out.println();

			for (Element element : dataProcessors) {
				TypeElement p = (TypeElement)element;
				out.append("	public ").append(p.getSimpleName())
						.append(" get").append(p.getSimpleName()).append("()").println("{");
				out.append("		return new ").append(p.getSimpleName()).println("();");
				out.append("	}").println();

			}
			out.println();
			out.println("}");


		}
	}

	private void writeDataProcessorFactory(String className, Set<? extends Element> dataProcessors) throws IOException {
		String packageName = null;
		int lastDot = className.lastIndexOf('.');
		if (lastDot > 0)
			packageName = className.substring(0, lastDot);

		String clazz = className.substring(lastDot+1);
		JavaFileObject jFile = processingEnv.getFiler().createSourceFile(clazz);

		try (PrintWriter out = new PrintWriter(jFile.openWriter())) {
			out.println("package org.vadere.simulator.projects.dataprocessing.processor;");
			out.println();
			out.println("import org.vadere.simulator.projects.dataprocessing.datakey.DataKey;");
			out.println();
			out.println("import java.util.HashMap;");
			out.println("import java.util.LinkedList;");
			out.println("import java.util.List;");
			out.println("import java.util.Map;");
			out.println("import java.util.function.Supplier;");
			out.println("import java.util.stream.Collectors;");
			out.println("import java.util.stream.Stream;");
			out.println();
			out.println();
			out.println("public class DataProcessorFactory3 {");
			out.println();
			out.println("	private static DataProcessorFactory3 instance;");
			out.println();
			out.println("	//performant threadsafe Singletone. Only at creation time the synchronize block is used,");
			out.println("	//after that the if statement will allways be false.");
			out.println("	public static DataProcessorFactory3 instance(){");
			out.println("		if(instance ==  null){");
			out.println("			synchronized (DataProcessorFactory3.class){");
			out.println("				if(instance == null){");
			out.println("					instance = new DataProcessorFactory3();");
			out.println("				}");
			out.println("			}");
			out.println("		}");
			out.println("		return instance;");
			out.println("	}");
			out.println();
			out.println("	private DataProcessorFactory3(){");

//							todo
//							instantiate HashMap<String, ProcessorObject> installedProcessors
//							and put ProcessorObjects iside

			out.println("	}");
			out.println();
			out.println("	//instance");
			out.println();
			out.println("	private HashMap<String, ProcessorObject> installedProcessors;");
			out.println();
			out.println("	public DataProcessor<?, ?> getProcessor(String clazz){");
			out.println("		installedProcessors = new HashMap<>();");
			out.println("		if(installedProcessors.containsKey(clazz)){");
			out.println("			return installedProcessors.get(clazz).supplier.get();");
			out.println("		}else {");
			out.println("			return  null;");
			out.println("		}");
			out.println("	}");
			out.println();
			out.println("	public List<String> getListOfProcessors(){");
			out.println("		return new LinkedList<>(installedProcessors.keySet());");
			out.println("	}");
			out.println();
			out.println("	// return a map based on the key of installedProcessors and the corresponding labels.");
			out.println("	public Map<String, String> getLabels(){");
			out.println("		return installedProcessors.entrySet().stream().collect(");
			out.println("				Collectors.toMap(entry-> entry.getKey(), entry -> entry.getValue().getLable(entry.getKey())));");
			out.println("	}");
			out.println();


			for (Element element : dataProcessors) {
				TypeElement p = (TypeElement)element;
				out.append("	public ").append(p.getSimpleName())
						.append(" get").append(p.getSimpleName()).append("()").println("{");
				out.append("		return new ").append(p.getSimpleName()).println("();");
				out.append("	}").println();
				out.println();
			}

			out.println("	private class ProcessorObject{");
			out.println("		Supplier<DataProcessor<?, ?>> supplier;");
			out.println("		String label;");
			out.println("		String description;");
			out.println();
			out.println("		public ProcessorObject(String label, Supplier<DataProcessor<?, ?>> supplier){");
			out.println("			this(label, supplier, \"\");");
			out.println("		}");
			out.println();
			out.println("		public ProcessorObject(String label, Supplier<DataProcessor<?, ?>> supplier, String description){");
			out.println("			this.label = label;");
			out.println("			this.supplier = supplier;");
			out.println("			this.description = description;");
			out.println("		}");
			out.println();
			out.println("		//return label or name if label is empty string.");
			out.println("		public String getLable(String name){");
			out.println("			return label.equals(\"\") ? name : label;");
			out.println("		}");
			out.println();
			out.println("	}");

			out.println("}");
			out.println();
		}
	}
}
