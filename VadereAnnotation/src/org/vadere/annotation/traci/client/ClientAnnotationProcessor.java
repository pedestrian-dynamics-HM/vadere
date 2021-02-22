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
import javax.tools.FileObject;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;

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

			StringBuilder pythonConstants = new StringBuilder();

			for (Element annotatedElement : annotatedElements) {
				try {
					writeApiClass(annotatedElement);
					writePythonBinding(annotatedElement, pythonConstants);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			String fileName = "pythonapi/VadereConstants.py";
			try {
				FileObject file = processingEnv.getFiler().createResource(StandardLocation.CLASS_OUTPUT, "", fileName);
				try (PrintWriter w = new PrintWriter(file.openWriter())) {
					w.println("#");
					w.println("# Generated source file. DO NOT CHANGE!");
					w.println("");
					w.println("from . constants import *");
					w.println("from . vadere_vars import *");
					w.println("");
					w.append(pythonConstants.toString());

				}
			} catch (IOException e) {
				e.printStackTrace();
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

	private String qq(String s){
		return "\"" + s + "\"";
	}

	private String q(String s){
		return "'" + s + "'";
	}

	private String t(int i){
		return " ".repeat(i*4);
	}


	private void writePythonBinding(Element apiClass, StringBuilder pythonConstants) throws IOException {
		TraCiApiWrapper traCIApi = new TraCiApiWrapper(apiClass);
		String fileName = "pythonapi/Vadere" + traCIApi.name + ".py";
		FileObject file = processingEnv.getFiler().createResource(StandardLocation.CLASS_OUTPUT, "", fileName);

		pythonConstants.append("# Command constants for Vadere").append(traCIApi.name).append("\n");
		pythonConstants.append(traCIApi.cmdGetVarName).append(" = 0x").append(Integer.toHexString(traCIApi.cmdGet)).append("\n");
		pythonConstants.append(traCIApi.cmdResponseGetVarName).append(" = 0x").append(Integer.toHexString(traCIApi.cmdResponseGet)).append("\n");
		pythonConstants.append(traCIApi.cmdSetVarName).append(" = 0x").append(Integer.toHexString(traCIApi.cmdSet)).append("\n");
		pythonConstants.append(traCIApi.cmdSubVarName).append(" = 0x").append(Integer.toHexString(traCIApi.cmdSub)).append("\n");
		pythonConstants.append(traCIApi.cmdResponseSubVarName).append(" = 0x").append(Integer.toHexString(traCIApi.cmdResponseSub)).append("\n");
		pythonConstants.append(traCIApi.cmdCtxVarName).append(" = 0x").append(Integer.toHexString(traCIApi.cmdCtx)).append("\n");
		pythonConstants.append(traCIApi.cmdResponseCtxVarName).append(" = 0x").append(Integer.toHexString(traCIApi.cmdResponseCtx)).append("\n\n");

		try (PrintWriter w = new PrintWriter(file.openWriter())) {
			w.println("#");
			w.println("# Generated source file. DO NOT CHANGE!");
			w.println("");
			w.println("from .domain import Domain");
			w.println("from . import VadereConstants as tc");
			w.println("\n");
			w.println("class Vadere" + traCIApi.name + "(Domain):");
			w.println(t(1) + "def __init__(self):");
			w.append(t(2)).append("Domain.__init__(self, ").append(qq(traCIApi.domainName)).append(",")
					.append("tc.").append(traCIApi.cmdGetVarName).append(", tc.").append(traCIApi.cmdSetVarName).append(", \n").append(t(8))
					.append("tc.").append(traCIApi.cmdSubVarName).append(", tc.").append(traCIApi.cmdResponseSubVarName).append(", \n").append(t(8))
					.append("tc.").append(traCIApi.cmdCtxVarName).append(", tc.").append(traCIApi.cmdResponseCtxVarName).append(")\n\n");

			for (Element element : apiClass.getEnclosedElements()) {
				List<? extends AnnotationMirror> anMirrors = element.getAnnotationMirrors();
				if (anMirrors != null){
					for (AnnotationMirror anMirror : anMirrors) {
						String anName = anMirror.getAnnotationType().asElement().getSimpleName().toString();
						String singeAn = traCIApi.singleAnnotation
								.substring(traCIApi.singleAnnotation.lastIndexOf(".") + 1).trim();
						if (anName.equals(singeAn)){
							ApiHandler apiHandler = new ApiHandler(traCIApi, element, anMirror);
							switch (apiHandler.apiType){
								case "GET":
									writePythonGET(w, apiHandler);
									break;
								case "SET":
									writePythonSET(w, apiHandler);
									break;
							}
						}
					}
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	private void writePythonGET(PrintWriter w, ApiHandler apiHandler) {
		if (apiHandler.dataTypeStr.isEmpty()){
			// standard GET command without additional data
			if (apiHandler.ignoreElementId){
				w.append(t(1)).append("def ").append(apiHandler.namePython).append("(self):").println();
				w.append(t(2)).append("return self._getUniversal(").append(apiHandler.varIdPython).append(", ").append(qq("")).append(")").println();
			}
			else {
				w.append(t(1)).append("def ").append(apiHandler.namePython).append("(self, element_id):").println();
				w.append(t(2)).append("return self._getUniversal(").append(apiHandler.varIdPython).append(", element_id)").println();
			}
		} else {
			// extended GET command which accepts any kind of data based on the standard traci data types
			if (apiHandler.ignoreElementId){
				w.append(t(1)).append("def ").append(apiHandler.namePython).append("(self, data):").println();
				w.append(t(2)).append("return self._getUniversal(").append(apiHandler.varIdPython).append(", ").append(qq("")).append(", data)").println();
			} else {
				w.append(t(1)).append("def ").append(apiHandler.namePython).append("(self, element_id, data):").println();
				w.append(t(2)).append("return self._getUniversal(").append(apiHandler.varIdPython).append(", element_id, data)").println();
			}
		}
		w.println();
	}


	private void writePythonSET(PrintWriter w, ApiHandler apiHandler) {
		if (apiHandler.ignoreElementId) {
			w.append(t(1)).append("def ").append(apiHandler.namePython).append("(self");
			if (apiHandler.dataTypeStr.length() > 0){
				w.append(", data");
			}
			w.append("):").println();
			w.append(t(2)).append("self._setCmd(").append(apiHandler.varIdPython).append(", ").append(qq("")).append(", ").append(qq(apiHandler.dataTypeStrPython()));
			if (apiHandler.dataTypeStr.length() > 0){
				w.append(", data)");
			} else {
				w.append(", None)");
			}
		} else {
			w.append(t(1)).append("def ").append(apiHandler.namePython).append("(self, element_id");
			if (apiHandler.dataTypeStr.length() > 0){
				w.append(", data");
			}
			w.append("):").println();
			w.append(t(2)).append("self._setCmd(").append(apiHandler.varIdPython).append(", element_id").append(", ").append(qq(apiHandler.dataTypeStrPython()));
			if (apiHandler.dataTypeStr.length() > 0){
				w.append(", data)");
			} else {
				w.append(", None)");
			}
		}
		w.println();
		w.println();
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
		String domainName;
		String singleAnnotation;
		String multipleAnnotation;
		String cmdEnum;
		String varEnum;
		String packageName;
		String[] imports;
		String extendedClassName;
		String varShort;
		int cmdGet;
		int cmdResponseGet;
		int cmdSet; // no Response needed.
		int cmdSub;
		int cmdResponseSub;
		int cmdCtx;
		int cmdResponseCtx;

		String varName;
		String cmdGetVarName;
		String cmdResponseGetVarName;
		String cmdSetVarName; // no Response needed.
		String cmdSubVarName;
		String cmdResponseSubVarName;
		String cmdCtxVarName;
		String cmdResponseCtxVarName;

		TraCiApiWrapper(Element apiClass){
			TraCIApi traCIApi = apiClass.getAnnotation(TraCIApi.class);


			name = traCIApi.name();
			nameShort = traCIApi.nameShort();
			nameShort = nameShort.isEmpty() ? name : nameShort;
			domainName = "v_" + name.substring(0, name.length()-3).toLowerCase();
			packageName = traCIApi.packageName();
			imports = traCIApi.imports();
			extendedClassName = traCIApi.extendedClassName();

			cmdGet = traCIApi.cmdGet();
			cmdResponseGet = cmdGet + 16;
			cmdSet = traCIApi.cmdSet();
			cmdSub = traCIApi.cmdSub();
			cmdResponseSub = traCIApi.cmdResponseSub();
			cmdCtx = traCIApi.cmdCtx();
			cmdResponseCtx = traCIApi.cmdResponseCtx();

			varName = traCIApi.var();

			cmdGetVarName = "CMD_GET_" + varName + "_VARIABLE";
			cmdResponseGetVarName= "RESPONSE_GET_" + varName + "_VARIABLE";
			cmdSetVarName = "CMD_SET_" + varName + "_VARIABLE";
			cmdSubVarName = "CMD_SUBSCRIBE_" + varName + "_VARIABLE";
			cmdResponseSubVarName = "RESPONSE_SUBSCRIBE_" + varName + "_VARIABLE";
			cmdCtxVarName = "CMD_SUBSCRIBE_" + varName + "_CONTEXT";
			cmdResponseCtxVarName = "RESPONSE_SUBSCRIBE_" + varName + "_CONTEXT";

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
		String varIdPython;
		String varType;
		String name;
		String namePython;
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
						this.varIdPython = "tc.VAR_" + value.toString();
						this.varType = varPrefix + "." + value.toString() + ".type";
						break;
					case "name":
						this.name = (String) value;
						this.namePython = toSnakeCase(this.name);
						break;
					case "ignoreElementId":
						this.ignoreElementId = (boolean) value;
						break;
					case "dataTypeStr":
						this.dataTypeStr = value.toString();
				}
			}
		}

		private String toSnakeCase(String input){
			StringBuilder out = new StringBuilder();
			boolean firstUpper = true;
			for (int i = 0; i < input.length(); i++){
				char ch = input.charAt(i);
				if (Character.isUpperCase(ch)){
					if (firstUpper) {
						out.append('_');
					}
					out.append(Character.toLowerCase(ch));
					firstUpper = false;
				} else {
					out.append(ch);
					firstUpper = true;
				}
			}
			return out.toString();
		}


		public String dataTypeStrPython(){
			switch (dataTypeStr) {
				case "Double":
					return "d";
				case "Integer":
					return "i";
				case "String":
					return "s";
				case "ArrayList<String>":
					return "l";
				case "ArrayList<Double>":
					return "f";
				case "VPoint":
					return "o";
			}
			return "Error";
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
