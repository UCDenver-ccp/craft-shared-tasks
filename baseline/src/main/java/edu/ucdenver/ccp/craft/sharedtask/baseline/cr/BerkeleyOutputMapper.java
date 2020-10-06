package edu.ucdenver.ccp.craft.sharedtask.baseline.cr;

/*-
 * #%L
 * Colorado Computational Pharmacology's CRAFT Shared
 * 						Task Baseline Utility
 * 						project
 * %%
 * Copyright (C) 2019 - 2020 Regents of the University of Colorado
 * %%
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * 3. Neither the name of the Regents of the University of Colorado nor the names of its contributors
 *    may be used to endorse or promote products derived from this software without
 *    specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.ucdenver.ccp.common.collections.CollectionsUtil;
import edu.ucdenver.ccp.common.file.CharacterEncoding;
import edu.ucdenver.ccp.common.file.FileReaderUtil;
import edu.ucdenver.ccp.common.file.FileUtil;
import edu.ucdenver.ccp.common.file.FileWriterUtil;

/**
 * maps berkeley output from berkeley produced files to the gold standard
 * tokenized files so that they can be evaluated
 *
 */
public class BerkeleyOutputMapper {

	public static void mapOutput(File inputFile, File skeletonFile, File outputFile) throws IOException {

		Map<Integer, Set<Integer>> berkLineToSkelLinesMap = new HashMap<Integer, Set<Integer>>();

		int skelIndex = 0;
		List<String> skeletonLines = FileReaderUtil.loadLinesFromFile(skeletonFile, CharacterEncoding.UTF_8);
		List<String> berkeleyLines = FileReaderUtil.loadLinesFromFile(inputFile, CharacterEncoding.UTF_8);

		for (int b = 0; b < berkeleyLines.size(); b++) {
			String berkLine = berkeleyLines.get(b);
			if (berkLine.trim().isEmpty() || berkLine.startsWith("#begin") || berkLine.startsWith("#end")) {
				continue;
			}
			String[] toks = berkLine.split("\\t");
			String word = toks[3];
			skelIndex = advanceToLine(skelIndex, word, skeletonLines, berkLineToSkelLinesMap, b);

		}

		for (Entry<Integer, Set<Integer>> entry : berkLineToSkelLinesMap.entrySet()) {
			Integer berkIndex = entry.getKey();
			Set<Integer> skelIndexes = entry.getValue();

			String berkLine = berkeleyLines.get(berkIndex);
			String[] berkToks = berkLine.split("\\t");
			String lastBerkCol = berkToks[berkToks.length - 1];
			if (!lastBerkCol.equals("-")) {
				if (skelIndexes.size() > 1) {
					throw new IllegalArgumentException("crap");
				} else {
					Integer sIndex = skelIndexes.iterator().next();
					String sline = skeletonLines.get(sIndex);
					String[] sToks = sline.split("\\t");
					String lastSkelTok = sToks[sToks.length - 1];
					if (lastSkelTok.equals("-")) {
						sToks[sToks.length - 1] = lastBerkCol;
					} else {
						sToks[sToks.length - 1] = combineCorefPattern(lastSkelTok, lastBerkCol);
					}
					String updatedSkelLine = String.join("\t", sToks);
					skeletonLines.set(sIndex, updatedSkelLine);
				}
			}
		}

		try (BufferedWriter writer = FileWriterUtil.initBufferedWriter(outputFile)) {
			for (String s : skeletonLines) {
				writer.write(s + "\n");
			}
		}

	}

	private static String combineCorefPattern(String lastSkelTok, String lastBerkCol) {
		String[] skelNums = lastSkelTok.trim().split("[\\(\\)\\|]");
		String[] berkNums = lastBerkCol.trim().split("[\\(\\)\\|]");

		Set<Integer> skelIds = new HashSet<Integer>();
		Set<Integer> berkIds = new HashSet<Integer>();

		for (String s : skelNums) {
			if (!s.trim().isEmpty()) {
				skelIds.add(Integer.parseInt(s));
			}
		}
		for (String b : berkNums) {
			if (!b.trim().isEmpty()) {
				berkIds.add(Integer.parseInt(b));
			}
		}

		skelIds.retainAll(berkIds);

		if (skelIds.isEmpty()) {
			return lastSkelTok + "|" + lastBerkCol;
		} else if (skelIds.size() == 1 && berkIds.size() == 1) {
			return "(" + skelIds.iterator().next() + ")";
		} else {
			String updatedLastCol = "";
			for (String skel : lastSkelTok.split("\\|")) {
				Pattern p = Pattern.compile("\\d+");
				Matcher m = p.matcher(skel);
				if (m.find()) {
					Integer id = Integer.parseInt(m.group());
					if (berkIds.contains(id)) {
						updatedLastCol += (((updatedLastCol.isEmpty()) ? "" : "|") + "(" + id + ")");
					} else {
						updatedLastCol += (((updatedLastCol.isEmpty()) ? "" : "|") + skel);
					}
				} else {
					throw new IllegalArgumentException("unable to find chain id in: " + skel);
				}
			}

			for (String berk : lastBerkCol.split("\\|")) {
				Pattern p = Pattern.compile("\\d+");
				Matcher m = p.matcher(berk);
				if (m.find()) {
					Integer id = Integer.parseInt(m.group());
					if (!skelIds.contains(id)) {
						updatedLastCol += (((updatedLastCol.isEmpty()) ? "" : "|") + berk);
					}
				} else {
					throw new IllegalArgumentException("unable to find chain id in: " + berk);
				}
			}
			return updatedLastCol;
		}
	}

	private static int advanceToLine(int skelIndex, String word, List<String> skeletonLines,
			Map<Integer, Set<Integer>> berkLineToSkelLinesMap, int berkIndex) {
		if (word.contains("\\/")) {
			word = word.replace("\\/", "/");
		}
		while (word.contains("\\*")) {
			word = word.replace("\\*", "*");
		}
		for (int i = skelIndex; i < skeletonLines.size(); i++) {
			String skelLine = skeletonLines.get(i);
			if (skelLine.trim().isEmpty() || skelLine.startsWith("#begin") || skelLine.startsWith("#end")) {
				continue;
			}
			String[] toks = skelLine.split("\\t");
			String skelWord = toks[3];
			if (overlaps(word, skelWord)) {
				CollectionsUtil.addToOne2ManyUniqueMap(berkIndex, i, berkLineToSkelLinesMap);
				return i;
			} else {
				continue;
			}

		}
		throw new IllegalArgumentException("Could not find word: " + word);
	}

	private static boolean overlaps(String word, String skelWord) {
		if (word.equals("-LRB-")) {
			return (skelWord.contains("(") || skelWord.contains("["));
		}
		if (word.equals("-RRB-")) {
			return (skelWord.contains(")") || skelWord.contains("]"));
		}
		if (word.equals("``")) {
			return (skelWord.contains("“") || skelWord.contains("\""));
		}
		if (word.equals("''")) {
			return (skelWord.contains("”") || skelWord.contains("\"") || skelWord.contains("``")
					|| skelWord.contains("''"));
		}
		if (word.equals("'")) {
			return (skelWord.contains("’") || skelWord.contains("'"));
		}

		return (skelWord.contains(word) || word.contains(skelWord));
	}

	public static void main(String[] args) {
		File brokenBerkeleyDirectory = new File(args[0]);
		File skeletonDirectory = new File(args[1]);
		File fixedBerkeleyDirectory = new File(args[2]);

		try {
			for (Iterator<File> fileIterator = FileUtil.getFileIterator(brokenBerkeleyDirectory, false); fileIterator
					.hasNext();) {
				File inputFile = fileIterator.next();
				File skeletonFile = new File(skeletonDirectory, inputFile.getName());
				File outputFile = new File(fixedBerkeleyDirectory, inputFile.getName());
				String docId = inputFile.getName().substring(0, inputFile.getName().indexOf("."));
				mapOutput(inputFile, skeletonFile, outputFile);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
