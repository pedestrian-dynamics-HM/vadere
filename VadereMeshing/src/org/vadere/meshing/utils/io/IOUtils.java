package org.vadere.meshing.utils.io;

import org.jetbrains.annotations.NotNull;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class IOUtils {
	public static BufferedWriter getWriter(@NotNull final String filename, @NotNull final File dir) throws IOException {
		FileWriter fileWriter = new FileWriter(dir.getAbsolutePath()+"/"+getNow()+"_"+filename);
		BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
		return bufferedWriter;
	}


	public static String getNow() {
		Date todaysDate = new java.util.Date();
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");
		String formattedDate = formatter.format(todaysDate);
		return formattedDate;
	}
}
