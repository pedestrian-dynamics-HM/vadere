package org.vadere.annotation.factories;

import java.io.PrintWriter;

public class Util {

	private static final String QUOTE = "\"";

	public static void createSingletone(final String instanceType, PrintWriter writer){
		writer.append("	private static ").append(instanceType).append(" instance;").println();
		writer.println();
		writer.println("	//performance threadsafe Singletone. Sync block will only be used once");
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

	public static String quote(final String data){
		return QUOTE + data + QUOTE;
	}
}
