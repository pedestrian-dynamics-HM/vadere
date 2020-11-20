package org.vadere.annotation.helptext;

import com.google.auto.service.AutoService;

import org.vadere.annotation.ImportScanner;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.FileObject;
import javax.tools.StandardLocation;

@SupportedAnnotationTypes({"*"}) // run for all annotations. process must return false so annotations are not consumed
@SupportedSourceVersion(SourceVersion.RELEASE_11)
@AutoService(Processor.class)
public class HelpTextAnnotationProcessor extends AbstractProcessor {

	ArrayList<Function<String, String>> pattern;
	Set<String> importedTypes;

	@Override
	public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

		initPattern();
		ImportScanner scanner = new ImportScanner();
		scanner.scan(roundEnv.getRootElements(), null);
		importedTypes = scanner.getImportedTypes();

		for (Element e: roundEnv.getRootElements()){
			if (e.getKind().isClass()  && e.asType().toString().startsWith("org.vadere.")) {
				try {
					String comment = processingEnv.getElementUtils().getDocComment(e);
					String relname = buildHelpTextPath(e.asType().toString());
					FileObject file = processingEnv.getFiler().createResource(StandardLocation.CLASS_OUTPUT, "", relname);
					try (PrintWriter w = new PrintWriter(file.openWriter())) {
						w.println("<h1> " + e.getSimpleName() + "</h1>");
						w.println();
						printComment(w, comment);
						w.println();
						printMemberDocString(e, w);
					}
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		}
		return false; // allow further processing
	}

	private String buildHelpTextPath(String className) {
		className = className.replace("<", "_");
		className = className.replace(">", "_");
		return "helpText/" + className + ".html";
	}

	private void initPattern() {
		pattern = new ArrayList<>();
		pattern.add( e -> {
			Pattern r = Pattern.compile("(\\{@link\\s+#)(.*?)(})");
			Matcher m = r.matcher(e);
			while (m.find()){
				e = m.replaceFirst("<span class='local_link'>$2</span>");
				m = r.matcher(e);
			}
			return e;
		});
		pattern.add( e -> {
			Pattern r = Pattern.compile("(\\{@link\\s+)(.*?)(})");
			Matcher m = r.matcher(e);
			while (m.find()){
				String linkId = findFullPath(m.group(2));
				e = m.replaceFirst(String.format("<a href='%s' class='class_link'>$2</a>", linkId));
				m = r.matcher(e);
			}
			return e;
		});

	}

	private String findFullPath(String className){
		String n = importedTypes.stream().filter(e-> e.endsWith(className)).findFirst().orElse("rel_/"+className);
		return "/helpText/" + n + ".html";
	}

	private void printComment(PrintWriter w, String multiLine){
		if (multiLine == null){
			w.println("<p>---</p>");
		} else {
			w.println("<p>");
			multiLine.lines().map(String::strip).map(this::applyMatcher).forEach(w::println);
			w.println("</p>");
		}
	}

	private String applyMatcher(String line){
		for(Function<String, String> p : pattern){
			line = p.apply(line);
		}
		return line;
	}

	private void printMemberDocString(Element e, PrintWriter w) {
		Set<? extends Element> fields = e.getEnclosedElements()
				.stream()
				.filter(o->o.getKind().isField())
				.collect(Collectors.toSet());
		for(Element field : fields){
			w.println("<hr>");
			w.println("<h2> Field: " + field.getSimpleName() + "</h2>");
			String comment = processingEnv.getElementUtils().getDocComment(field);
			printComment(w, comment);
			w.println();
		}

	}
}
