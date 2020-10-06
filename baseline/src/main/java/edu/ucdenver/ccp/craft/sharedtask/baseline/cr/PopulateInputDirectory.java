package edu.ucdenver.ccp.craft.sharedtask.baseline.cr;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import org.apache.commons.io.FileUtils;

import edu.ucdenver.ccp.common.file.FileUtil;

/**
 * There may be more streamlined ways to run the Berkeley coreference resolution
 * system, however the way it was run for the CRAFT Shared Task 2019 baseline
 * requires the input files to be in individual directories. This class
 * creates the required directory structure.
 *
 */
public class PopulateInputDirectory {

	public static void populate(File corefInputDirectory, File inputDirectory)
			throws IOException {

		for (Iterator<File> fileIterator = FileUtil.getFileIterator(corefInputDirectory, false); fileIterator
				.hasNext();) {
			File file = fileIterator.next();
			String id = file.getName().substring(0, file.getName().indexOf("."));
			String inputFileName = id + ".txt";
			
			File dir = new File(inputDirectory, id);
			dir.mkdirs();
			FileUtils.copyFile(file, new File(dir, inputFileName));
		}

	}

	public static void main(String[] args) {
		File corefInputDirectory = new File(args[0]);
		File inputDirectory = new File(args[1]);

		try {
			populate(corefInputDirectory, inputDirectory);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}

	}

}
