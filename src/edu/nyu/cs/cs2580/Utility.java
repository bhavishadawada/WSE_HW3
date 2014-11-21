package edu.nyu.cs.cs2580;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Utility {
	public static List<String> getFilesInDirectory(String directory) {
		File folder = new File(directory);
		List<String> files = new ArrayList<String>();
		for (final File fileEntry : folder.listFiles()) {
			files.add(fileEntry.getName());
		}
		System.out.println(files.size());
		return files;
	}
}
