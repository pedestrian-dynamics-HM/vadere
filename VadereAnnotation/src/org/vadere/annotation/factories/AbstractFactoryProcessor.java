package org.vadere.annotation.factories;

import org.vadere.annotation.factories.dataprocessors.DataProcessorClass;
import org.vadere.annotation.factories.outputfiles.OutputFileClass;

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

/**
 * Base FactoryProcessor to generate Factories
 *
 * Factories are crated with two annotations: {@link FactoryType} and a specific Annotation Interface
 * declaring a new Factory. {@link FactoryType} is used to annotate the specific annotation used
 * for factories (i.e. {@link OutputFileClass}, {@link DataProcessorClass},
 * {@link org.vadere.annotation.factories.models.ModelClass} and
 * {@link org.vadere.annotation.factories.attributes.ModelAttributeClass})
 *
 * The {@link AbstractFactoryProcessor} defines process loop  used to
 * generate java source files. It also provides abstract methods as hooks to allow
 * child classes to add additional elements into the generated java source file.
 * All hooks have access to the PrintWriter used to create the java source file as well
 * the Set of TypElements annotated with the specific annotation used in this {@link javax.annotation.processing.Processor}
 *
 * <ul>
 * <li>{@link #addImports(Set, PrintWriter)}: </br>
 * This hook allows the implementing class to add additional import statements
 * which where not mentioned in the {@link FactoryType} annotation interface.
 * </li>
 * <li>{@link #addMembers(Set, PrintWriter)}: </br>
 * This hook allows the implementing class to add additional members to the new
 * java source file.
 * </li>
 * <li>{@link #addLastConstructor(Set, PrintWriter)}: </br>
 * This hook allows the implementing class to add additional statement to the
 * Constructor of the new Factory. Only the Default constructor is implemented!
 * </li>
 * <li>{@link #addLastConstructor(Set, PrintWriter)}: </br>
 * This hook allows the implementing class to add additional function to the
 * new java source file.
 * </li>
 * </ul>
 *
 *
 * {@link #addImports(Set, PrintWriter)}
 */
public abstract class AbstractFactoryProcessor extends AbstractProcessor {

	private static final String QUOTE = "\"";

	protected String factoryClassName;      //Name of the new Factory
	protected String extendedClassName;     //Name of the Factory which needs to be extended.
	protected String genericFactoryTypes;   //Generics if extendedClass needs it.
	protected String factoryPackage;        //Where to place the new Factory.
	protected String[] factoryImports;      //Imports needed by the new Factory.


	/**
	 * This method is called from the java compiler directly*
	 */
	@Override
	public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

		for (TypeElement annotation : annotations) {
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

			setup(annotation);

			try {
				writeFactory(annotatedElements);
			} catch (IOException e) {
				e.printStackTrace();
			}

		}

		return true;
	}

	protected abstract void addImports(Set<? extends Element> elements, PrintWriter writer);

	protected abstract void addMembers(Set<? extends Element> elements, PrintWriter writer);

	protected abstract void addLastConstructor(Set<? extends Element> elements, PrintWriter writer);

	protected abstract void addLast(Set<? extends Element> elements, PrintWriter writer);

	protected String name(Element e) {
		return e.getSimpleName().toString();
	}

	/**
	 * This method is called for each SupportedAnnotationTypes from the {@link #process(Set, RoundEnvironment)}
	 * method. The current implementation only allows ONE SupportedAnnotationTypes because this
	 * method build the whole java source file.
	 *
	 * @param elements Elements annotated with one of the SupportedAnnotationTypes
	 */
	protected abstract void writeFactory(Set<? extends Element> elements) throws IOException;


	/**
	 * Create class statement based on information provided by the {@link FactoryType} annotation
	 *
	 * @param elements Elements annotated with one of the SupportedAnnotationTypes
	 * @param writer   PrintWriter used to create the source file.
	 */
	protected void buildClass(Set<? extends Element> elements, PrintWriter writer) {
		writer.append("public class ").append(factoryClassName);
		if (!extendedClassName.isEmpty()) {
			writer.append(" extends ").append(extendedClassName);
			if (!genericFactoryTypes.isEmpty()) {
				writer.append("<").append(genericFactoryTypes).append("> ");
			}
		}
		writer.println("{");
	}

	/**
	 * Create a getter for each Element in elements set. The underling TypeElement
	 * is a Class and must have a constructor without parameters.
	 *
	 * @param elements Elements annotated with one of the SupportedAnnotationTypes
	 * @param writer   PrintWriter used to create the source file.
	 */
	protected void buildGetters(Set<? extends Element> elements, PrintWriter writer) {
		for (Element element : elements) {
			TypeElement p = (TypeElement) element;
			writer.append("	public ").append(p.getSimpleName())
					.append(" get").append(p.getSimpleName()).append("()").println("{");
			writer.append("		return new ").append(p.getSimpleName()).println("();");
			writer.append("	}").println();
			writer.println();
		}
	}

	/**
	 * Creates a static method to provide a thread safe singletone implementation.
	 */
	protected void createSingletone(final String instanceType, PrintWriter writer){
		writer.append("	private static ").append(instanceType).append(" instance;").println();
		writer.println();
		writer.println("	//good performance threadsafe Singletone. Sync block will only be used once");
		writer.append("	public static ").append(instanceType).append(" instance(){").println();
		writer.println("		if(instance ==  null){");
		writer.append("			synchronized (").append(instanceType).append(".class){").println();
		writer.println("				if(instance == null){");
		writer.append("					instance = new ").append(instanceType).append("();").println();
		writer.println("				}");
		writer.println("			}");
		writer.println("		}");
		writer.println("		return instance;");
		writer.println("	}");
		writer.println();
	}

	protected String quote(final String data){
		return QUOTE + data + QUOTE;
	}

	private void setup(TypeElement annotation) {
		FactoryType factoryType = annotation.getAnnotation(FactoryType.class);
		assert factoryType != null;
		this.factoryClassName = factoryType.factoryClassName();
		this.extendedClassName = factoryType.extendedClassName();
		this.genericFactoryTypes = factoryType.genericFactoryTypes();
		this.factoryPackage = factoryType.factoryPackage();
		this.factoryImports = factoryType.factoryImports();
	}
}
