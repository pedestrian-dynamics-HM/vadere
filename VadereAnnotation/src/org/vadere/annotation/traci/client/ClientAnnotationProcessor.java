package org.vadere.annotation.traci.client;

import com.google.auto.service.AutoService;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.MirroredTypeException;
import javax.tools.JavaFileObject;

@SupportedAnnotationTypes("org.vadere.annotation.traci.client.TraCIApi")
@SupportedSourceVersion(SourceVersion.RELEASE_11)
@AutoService(Processor.class)
public class ClientAnnotationProcessor extends AbstractProcessor {


	private StringBuilder apiMembers;
	private StringBuilder apiInit;
	private StringBuilder apiMapping;
	private StringBuilder apiAbstract;

	@Override
	public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

		apiMembers = new StringBuilder();
		apiInit = new StringBuilder();
		apiMapping = new StringBuilder();
		apiAbstract = new StringBuilder();

		// see SupportedAnnotationTypes (here only TraCIApi)
		for (TypeElement annotation : annotations) {
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

			for (Element annotatedElement : annotatedElements) {
				try {
					writeApiClass(annotatedElement);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

		}
		try {
			TypeElement element = processingEnv.getElementUtils().
					getTypeElement("org.vadere.manager.client.AbstractTestClient");
			if (element == null)
				writeAbstractTestClient();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return true;
	}

	protected void writeAbstractTestClient() throws IOException {
		JavaFileObject jFile = processingEnv.getFiler().createSourceFile("AbstractTestClient");
		try (PrintWriter writer = new PrintWriter(jFile.openWriter())){
			writer.append("package org.vadere.manager.client; ").println();
			writer.println();
			writer.append("import org.vadere.manager.TraCISocket;").println();
			writer.append("import org.vadere.manager.client.ConsoleReader;").println();
			writer.append("import java.io.IOException;").println();
			writer.println();
			writer.append("public abstract class AbstractTestClient {").println();
			writer.append(apiMembers.toString()).println();
			writer.append("\tpublic AbstractTestClient() { }").println();
			writer.println();
			writer.append("\tpublic void init(TraCISocket socket, ConsoleReader consoleReader){").println();
			writer.append(apiInit.toString());
			writer.println();
			writer.append(apiMapping.toString());
			writer.append("\t}").println();
			writer.println();
			writer.append(apiAbstract.toString());
			writer.println();
			writer.append("}").println();
		}

	}

	protected void writeApiClass(Element apiClass) throws IOException {
		TraCiApiWrapper traCIApi = new TraCiApiWrapper(apiClass);
		JavaFileObject jFile = processingEnv.getFiler().createSourceFile(traCIApi.name);

		apiMembers.append(String.format("\tprotected %s.%s %s;\n", traCIApi.packageName, traCIApi.name, traCIApi.name.toLowerCase()));
		apiInit.append(String.format("\t\t%s = new %s.%s(socket);\n", traCIApi.name.toLowerCase(), traCIApi.packageName, traCIApi.name));

		try (PrintWriter writer = new PrintWriter(jFile.openWriter())){
			writer.append("package ").append(traCIApi.packageName).append(";").println();
			writer.println();
			for (String anImport : traCIApi.imports) {
				writer.append("import ").append(anImport).append(";").println();
			}
			writer.append("import ").append(traCIApi.cmdEnum).append(";").println();
			writer.append("import ").append(traCIApi.varEnum).append(";").println();
			writer.println();

			// start API class
			writer.append("public class ").append(traCIApi.name).append(" extends ")
					.append(traCIApi.extendedClassName).append(" {").println();

			// constructor
			writer.append("\tpublic ").append(traCIApi.name).append("(TraCISocket socket) {").println();
			writer.append("\t\tsuper(socket, \"").append(traCIApi.name).append("\");").println();
			writer.append("\t}").println();
			writer.println();


			for (Element element : apiClass.getEnclosedElements()) {
				List<? extends AnnotationMirror> anMirrors = element.getAnnotationMirrors();
				if (anMirrors != null){
					for (AnnotationMirror anMirror : anMirrors) {
						String anName = anMirror.getAnnotationType().asElement().getSimpleName().toString();
						String singeAn = traCIApi.singleAnnotation
								.substring(traCIApi.singleAnnotation.lastIndexOf(".") + 1).trim();
						if (anName.equals(singeAn)){
							ApiHandler apiHandler = new ApiHandler(traCIApi, element, anMirror);
							apiMapping.append(String.format("\t\tconsoleReader.addCommand(\"%s.%s\", \"\", this::%s_%s);\n", traCIApi.nameShort.toLowerCase(), apiHandler.name, traCIApi.name.toLowerCase(), apiHandler.name));
							apiAbstract.append(String.format("\t\tabstract public void %s_%s (String args[]) throws IOException;\n",traCIApi.name.toLowerCase(), apiHandler.name));
							switch (apiHandler.apiType){
								case "GET":
									writeGET(writer, apiHandler);
									break;
								case "SET":
									writeSET(writer, apiHandler);
									break;
							}
						}
					}
				}
			}
			writer.append("}").println(); // end API class
		}
	}

	protected void writeGET(PrintWriter writer, ApiHandler apiHandler){
		if (apiHandler.dataTypeStr.isEmpty()){
			// standard GET command without additional data
			if (apiHandler.ignoreElementId){
				writer.append("\tpublic TraCIResponse ").append(apiHandler.name).append("() throws IOException {").println();
				writer.append("\t\tTraCIPacket p = TraCIGetCommand.build(").append(apiHandler.cmd).append(", ").append(apiHandler.varId).append(", \"-1\");").println();
			}
			else {
				writer.append("\tpublic TraCIResponse ").append(apiHandler.name).append("(String elementID) throws IOException {").println();
				writer.append("\t\tTraCIPacket p = TraCIGetCommand.build(").append(apiHandler.cmd).append(", ").append(apiHandler.varId).append(", elementID);").println();
			}
		} else {
			// extended GET command which accepts any kind of data based on the standard traci data types
			if (apiHandler.ignoreElementId){
				writer.append("\tpublic TraCIResponse ").append(apiHandler.name).append("(").append(apiHandler.dataTypeStr).append(" data) throws IOException {").println();
				writer.append("\t\tTraCIPacket p = TraCIGetCommand.build(").append(apiHandler.cmd).append(", \"-1\" , ").append(apiHandler.varId).append(", ").append(apiHandler.varType).append(", data);").println();
			} else {
				writer.append("\tpublic TraCIResponse ").append(apiHandler.name).append("(String elementId, ").append(apiHandler.dataTypeStr).append(" data) throws IOException {").println();
				writer.append("\t\tTraCIPacket p = TraCIGetCommand.build(").append(apiHandler.cmd).append(", elementId, ").append(apiHandler.varId).append(", ").append(apiHandler.varType).append(", data);").println();
			}
		}


		writer.append("\n\t\tsocket.sendExact(p);\n").println();
		writer.append("\t\treturn socket.receiveResponse();").println();
		writer.append("\t}").println();
		writer.println();
	}

	protected void writeSET(PrintWriter writer, ApiHandler apiHandler){
		if (apiHandler.ignoreElementId) {
			writer.append("\tpublic TraCIResponse ").append(apiHandler.name).append("(").append(apiHandler.dataTypeStr);
			if(apiHandler.dataTypeStr.length() > 0){
				writer.append(" data");
			}
			writer.append(") throws IOException {").println();
			writer.append("\t\tTraCIPacket p = TraCISetCommand.build(").append(apiHandler.cmd).append(", \"-1\", ").append(apiHandler.varId).append(", ").append(apiHandler.varType);
			if(apiHandler.dataTypeStr.length() > 0){
				writer.append(", data");
			} else {
				writer.append(", null");
			}
			writer.append(");").println();
		} else {
			writer.append("\tpublic TraCIResponse ").append(apiHandler.name).append("(String elementId");
			if(apiHandler.dataTypeStr.length() > 0){
				writer.append(", ").append(apiHandler.dataTypeStr).append(" data");
			}
			writer.append(") throws IOException {").println();
			writer.append("\t\tTraCIPacket p = TraCISetCommand.build(").append(apiHandler.cmd).append(", elementId, ").append(apiHandler.varId).append(", ").append(apiHandler.varType);
			if(apiHandler.dataTypeStr.length() > 0){
				writer.append(", data");
			} else {
				writer.append(", null");
			}
			writer.append(");").println();
		}

		writer.append("\n\t\tsocket.sendExact(p);\n").println();
		writer.append("\t\treturn socket.receiveResponse();").println();
		writer.append("\t}").println();
		writer.println();
	}

	class TraCiApiWrapper {
		String name;
		String nameShort;
		String singleAnnotation;
		String multipleAnnotation;
		String cmdEnum;
		String varEnum;
		String packageName;
		String[] imports;
		String extendedClassName;

		TraCiApiWrapper(Element apiClass){
			TraCIApi traCIApi = apiClass.getAnnotation(TraCIApi.class);


			name = traCIApi.name();
			nameShort = traCIApi.nameShort();
			nameShort = nameShort.isEmpty() ? name : nameShort;
			packageName = traCIApi.packageName();
			imports = traCIApi.imports();
			extendedClassName = traCIApi.extendedClassName();

			try {
				singleAnnotation = traCIApi.singleAnnotation().getCanonicalName();
			} catch (MirroredTypeException e){
				singleAnnotation = e.getTypeMirror().toString();
			}

			try {
				multipleAnnotation = traCIApi.multipleAnnotation().getCanonicalName();
			} catch (MirroredTypeException e){
				multipleAnnotation = e.getTypeMirror().toString();
			}

			try {
				cmdEnum = traCIApi.cmdEnum().getCanonicalName();
			} catch (MirroredTypeException e){
				cmdEnum = e.getTypeMirror().toString();
			}

			try {
				varEnum = traCIApi.varEnum().getCanonicalName();
			} catch (MirroredTypeException e){
				varEnum = e.getTypeMirror().toString();
			}
		}
	}

	class ApiHandler {

		String cmd;
		String varId;
		String varType;
		String name;
		String dataTypeStr;
		boolean ignoreElementId;
		String apiType; //SET, GET, SUB

		public ApiHandler(TraCiApiWrapper traCIApi, Element method, AnnotationMirror annotationMirror){

			ignoreElementId = false; //default
			dataTypeStr = "";
			String cmdPrefix = traCIApi.cmdEnum;
			cmdPrefix = cmdPrefix.substring(cmdPrefix.lastIndexOf('.') + 1).trim();
			String varPrefix = traCIApi.varEnum;
			varPrefix = varPrefix.substring(varPrefix.lastIndexOf('.') + 1).trim();

			Map<? extends ExecutableElement, ? extends AnnotationValue> valueMap = annotationMirror.getElementValues();
			for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : valueMap.entrySet()) {
				String key = entry.getKey().getSimpleName().toString();
				Object value = entry.getValue().getValue();
				switch (key){
					case "cmd":
						this.cmd = cmdPrefix+ "." + value.toString();
						this.apiType = value.toString().substring(0, 3);
						break;
					case "var":
						this.varId = varPrefix + "." + value.toString() + ".id";
						this.varType = varPrefix + "." + value.toString() + ".type";
						break;
					case "name":
						this.name = (String) value;
						break;
					case "ignoreElementId":
						this.ignoreElementId = (boolean) value;
						break;
					case "dataTypeStr":
						this.dataTypeStr = value.toString();
				}
			}
		}

		@Override
		public String toString() {
			return "ApiHandler{" +
					"cmd='" + cmd + '\'' +
					", varId='" + varId + '\'' +
					", varType='" + varType + '\'' +
					", name='" + name + '\'' +
					", ignoreElementId=" + ignoreElementId +
					", apiType='" + apiType + '\'' +
					'}';
		}
	}
}
