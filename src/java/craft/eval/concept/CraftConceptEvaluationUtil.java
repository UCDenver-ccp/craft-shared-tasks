package craft.eval.concept;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import edu.ucdenver.ccp.common.collections.CollectionsUtil;
import edu.ucdenver.ccp.common.file.CharacterEncoding;
import edu.ucdenver.ccp.common.file.FileUtil;
import edu.ucdenver.ccp.common.file.FileWriterUtil;
import edu.ucdenver.ccp.file.conversion.TextDocument;
import edu.ucdenver.ccp.file.conversion.bionlp.BioNLPDocumentReader;
import edu.ucdenver.ccp.nlp.core.annotation.TextAnnotation;
import edu.ucdenver.ccp.nlp.core.mention.ClassMention;
import edu.ucdenver.ccp.nlp.evaluation.bossy2013.BossyMetric;
import edu.ucdenver.ccp.nlp.evaluation.bossy2013.BoundaryMatchStrategy;
import edu.ucdenver.ccp.nlp.evaluation.bossy2013.SlotErrorRate;

/**
 * Utility class for performing concept evaluations against the CRAFT corpus.
 */
public class CraftConceptEvaluationUtil {

	public static final String OUTPUT_FILE_HEADER = "#document-id\tsubstitutions\tinsertions\tdeletions\tmatches\tref-count\tprediction-count\tSER\tprecision\trecall\tF1-score";

	public enum OntologyKey {
		/* @formatter:off */
		CHEBI("CHEBI.obo"),
		CHEBI_EXT("CHEBI+extensions.obo"),
		CL("CL.obo"),
		CL_EXT("CL+extensions.obo"),
		GO_BP("GO.obo"),
		GO_BP_EXT("GO+GO_BP_extensions.obo"),
		GO_CC("GO.obo"),
		GO_CC_EXT("GO+GO_CC_extensions.obo"),
		GO_MF("GO_MF_stub.obo"),
		GO_MF_EXT("GO_MF_stub+GO_MF_extensions.obo"),
		MOP("MOP.obo"),
		MOP_EXT("MOP+extensions.obo"),
		NCBITAXON("NCBITaxon.obo"),
		NCBITAXON_EXT("NCBITaxon+extensions.obo"),
		PR("PR.obo"),
		PR_EXT("PR+extensions.obo"),
		SO("SO.obo"),
		SO_EXT("SO+extensions.obo"),
		UBERON("UBERON.obo"),
		UBERON_EXT("UBERON+extensions.obo");
		/* @formatter:on */

		private final String fileName;
		private ZipFile zipFile;

		private OntologyKey(String fileName) {
			this.fileName = fileName;
		}

		private String getRelativePath() {
			String base = (this.name().endsWith("_EXT")) ? this.name().substring(0, this.name().length() - 4)
					: this.name();
			if (base.equals("NCBITAXON")) {
				base = "NCBITaxon";
			}
			String secondLevel = (this.name().contains("_EXT")) ? base + "+extensions" : base;
			return "concept-annotation" + File.separator + base + File.separator + secondLevel;
		}

		public void closeZipFile() throws IOException {
			if (zipFile != null) {
				zipFile.close();
			}
		}

		/**
		 * @param craftDistributionBaseDirectory
		 * @return a path to the ontology file (could be compressed (.zip, .gz) or uncompressed.
		 * @throws IllegalStateException
		 *             if the ontology file cannot be founds
		 */
		public File getOntologyFile(File craftDistributionBaseDirectory) {
			File uncompressedFile = new File(craftDistributionBaseDirectory,
					getRelativePath() + File.separator + this.fileName);
			if (uncompressedFile.exists()) {
				return uncompressedFile;
			}
			File zippedFile = new File(uncompressedFile.getAbsolutePath() + ".zip");
			if (zippedFile.exists()) {
				return zippedFile;
			}
			File gzippedFile = new File(uncompressedFile.getAbsolutePath() + ".gz");
			if (gzippedFile.exists()) {
				return gzippedFile;
			}
			throw new IllegalStateException(
					"Unable to locate ontology file for key: " + this.name() + " in expected directory: "
							+ uncompressedFile.getParentFile().getAbsolutePath() + ". Expected to find '"
							+ uncompressedFile.getName() + "' or a compressed (.zip or .gz) version of it.");
		}

		public InputStream getOntologyStream(File craftDistributionBaseDirectory, CharacterEncoding encoding)
				throws IOException {
			File ontologyFile = getOntologyFile(craftDistributionBaseDirectory);
			System.out.println("Loading ontology file: " + ontologyFile.getAbsolutePath());
			if (ontologyFile.getName().endsWith(".obo")) {
				return new FileInputStream(ontologyFile);
			}
			if (ontologyFile.getName().endsWith(".gz")) {
				return new GZIPInputStream(new FileInputStream(ontologyFile));
			}
			if (ontologyFile.getName().endsWith(".zip")) {
				zipFile = new ZipFile(ontologyFile);
				ZipEntry zipEntry = zipFile.entries().nextElement();
				return zipFile.getInputStream(zipEntry);
			}
			throw new IllegalArgumentException(
					"Unable to return input stream for ontology file: " + ontologyFile.getAbsolutePath());
		}
	}

	/**
	 * Same as the other evaluate(directory, ...) method but the CharacterEncoding defaults to
	 * UTF-8.
	 *
	 * @param craftDistributionDirectory
	 * @param referenceDirectoryBase
	 * @param testDirectoryBase
	 * @param boundaryMatchStrategy
	 * @throws IOException
	 */
	public static void evaluate(File craftDistributionDirectory, File referenceDirectoryBase, File testDirectoryBase,
								BoundaryMatchStrategy boundaryMatchStrategy) throws IOException {
		evaluate(craftDistributionDirectory, referenceDirectoryBase, testDirectoryBase, boundaryMatchStrategy,
				CharacterEncoding.UTF_8);
	}

	/**
	 * @param craftDistributionDirectory
	 *            - the base directory for the CRAFT distribution
	 * @param referenceDirectoryBase
	 *            - the path to the base directory where the BioNLP formatted files containing gold
	 *            standard concept annotations are located. This directory should contain a separate
	 *            directory for each concept type named using the appropriate ontology key, e.g
	 *            CHEBI, CHEBI_EXT, CL, CL_EXT, etc. Directory names can be upper- or lower-case.
	 * @param testDirectoryBase
	 *            - the path to the base directory where the BioNLP formatted files containing the
	 *            concept annotations to be evaluated are located. This directory should contain a
	 *            separate directory for each concept type named using the appropriate ontology key,
	 *            e.g CHEBI, CHEBI_EXT, CL, CL_EXT, etc. Directory names can be upper- or
	 *            lower-case.
	 * @param boundaryMatchStrategy
	 * @param ontologyEncoding
	 * @throws IOException
	 */
	public static void evaluate(File craftDistributionDirectory, File referenceDirectoryBase, File testDirectoryBase,
								BoundaryMatchStrategy boundaryMatchStrategy, CharacterEncoding ontologyEncoding) throws IOException {
		File[] directories = testDirectoryBase.listFiles(new FileFilter() {
			@Override
			public boolean accept(File file) {
				return file.isDirectory();
			}
		});
		for (File testDirectory : directories) {
			if (testDirectory.isDirectory()) {
				OntologyKey ontKey = null;
				try {
					ontKey = OntologyKey.valueOf(testDirectory.getName().toUpperCase());
				} catch (IllegalArgumentException e) {
					List<String> ontologyKeys = new ArrayList<String>();
					for (OntologyKey key : OntologyKey.values()) {
						ontologyKeys.add(key.name());
					}
					Collections.sort(ontologyKeys);
					String ontologyKeysString = CollectionsUtil.createDelimitedString(ontologyKeys, ", ");
					System.err.println(
							"ERROR - encountered a directory that does not correspond to an ontology key used by CRAFT. "
									+ "Directories must be named using one of the following (lower or upper case can be used): "
									+ ontologyKeysString);
					System.err.println(
							"Please adjust the offending directory name accordingly and restart the evaluation.");
					System.exit(-1);
				}
				File referenceDirectory = new File(referenceDirectoryBase, ontKey.name());
				if (!referenceDirectory.exists()) {
					referenceDirectory = new File(referenceDirectoryBase, ontKey.name().toLowerCase());
				}
				if (!referenceDirectory.exists()) {
					System.err.println("Expected concept annotation gold standard directory does not exist: "
							+ referenceDirectory.getAbsolutePath());
					System.err.println("Please adjust as necessary and restart the evaluation.");
					System.exit(-1);
				}
				evaluate(ontKey, craftDistributionDirectory, referenceDirectory, testDirectory, boundaryMatchStrategy,
						ontologyEncoding);
			}
		}
	}

	/**
	 * Ontology file is assumed to be in the reference directory
	 *
	 * @param referenceDirectory
	 * @param testDirectory
	 * @param boundaryMatchStrategy
	 * @param craftDistributionBaseDirectory
	 * @param ontologyEncoding
	 * @param annotationFileSuffix
	 * @throws IOException
	 */
	public static void evaluate(OntologyKey ontologyKey, File craftDistributionBaseDirectory, File referenceDirectory,
								File testDirectory, BoundaryMatchStrategy boundaryMatchStrategy, CharacterEncoding ontologyEncoding)
			throws IOException {

		System.out.println("Evaluating " + ontologyKey.name() + " concept annotations.");
		System.out.println("Gold standard directory: " + referenceDirectory);
		System.out.println("Test annotation directory: " + testDirectory);
		System.out.println("Boundary Matching Strategy: " + boundaryMatchStrategy.name());

		SlotErrorRate totalSer = new SlotErrorRate(BigDecimal.valueOf(0), 0, 0, 0, 0);

		BossyMetric bm = new BossyMetric(
				ontologyKey.getOntologyStream(craftDistributionBaseDirectory, ontologyEncoding));

		ontologyKey.closeZipFile();

		File outputFile = new File(testDirectory, ontologyKey.name().toLowerCase() + "_results.tsv");
		if (outputFile.exists()) {
			FileUtil.deleteFile(outputFile);
		}
		try (BufferedWriter writer = FileWriterUtil.initBufferedWriter(outputFile)) {
			writer.write(OUTPUT_FILE_HEADER + "\n");
			for (Iterator<File> testAnnotFileIter = FileUtil.getFileIterator(testDirectory, false); testAnnotFileIter
					.hasNext();) {
				File testAnnotFile = testAnnotFileIter.next();
				File goldAnnotFile = new File(referenceDirectory, testAnnotFile.getName());
				String sourceId = testAnnotFile.getName().substring(0, testAnnotFile.getName().lastIndexOf("."));
				File txtFile = new File(craftDistributionBaseDirectory,
						"articles" + File.separator + "txt" + File.separator + sourceId + ".txt");

				if (!txtFile.exists()) {
					System.err.println("NOTE: Skipping processing of file: " + testAnnotFile.getName()
							+ " that was found in the test annotation directory as it does not have a corresponding document txt file.");
				} else {
					BioNLPDocumentReader docReader = new BioNLPDocumentReader();
					TextDocument testDocument = docReader.readDocument(sourceId, "unknown", testAnnotFile, txtFile,
							CharacterEncoding.UTF_8);
					TextDocument goldDocument = docReader.readDocument(sourceId, "unknown", goldAnnotFile, txtFile,
							CharacterEncoding.UTF_8);

					/*
					 * Evaluation of the extension class annotations is somewhat tricky because
					 * although the gold standard annotations use the extension class namespace, the
					 * extension class namespace is not used in the provided ontologies for classes
					 * that were dynamically created. For example, the CHEBI role hierarchy has been
					 * mirrored with the extension namespace (CHEBI_EXT) in order to annotate
					 * chemical entities (instead of roles), however this hierarchy is not
					 * explicitly represented in the provided ontology. Instead, the original role
					 * hierarchy using the standard CHEBI (OBO) namespace is used. Because the
					 * ontologies use the standard namespace even for the dynamically generated
					 * extension classes, we must translate any input annotation using a dynamically
					 * generated namespace (e.g. _EXT) to its corresponding base namespace. We can
					 * recognize the dynamically generated classes by a simple pattern: XX_EXT:\d+$.
					 * That is, they have some characters representing the ontology (the XX)
					 * followed by _EXT: and then only numbers. To translate to the base namespace,
					 * we simply remove the _EXT. Keep in mind that there are non-dynamically
					 * generated ontology concepts that use the extension namespace that should not
					 * be translated. These all have words in their identifiers (not numbers) so
					 * they will not match the simple pattern we use here.
					 *
					 * NOTE: do not do this translation for GO_MF
					 */
					if (ontologyKey.name().endsWith("_EXT") && ontologyKey != OntologyKey.GO_MF_EXT) {
						/*
						 * so, if this is an extension class project, then we need to do the
						 * translation for both the test and reference annotation sets
						 */
						translateExtensionToBaseNamespace(goldDocument);
						translateExtensionToBaseNamespace(testDocument);
					}

					SlotErrorRate ser = bm.evaluate(goldDocument.getAnnotations(), testDocument.getAnnotations(),
							boundaryMatchStrategy);
					writeSlotErrorRate(sourceId, ser, writer);
					totalSer.update(ser);
				}
			}
			writeSlotErrorRate("TOTAL", totalSer, writer);
		}
	}

	/**
	 * @param document
	 *            Processes the annotation in the input document. If there are annotations that use
	 *            an extension class that matches the XX_EXT:\d+$ pattern, then remove the _EXT
	 *            thereby creating an annotation to a base class instead of an extension class. This
	 *            is done b/c these particular extension classes are not explicitly represented in
	 *            the ontology, and their corresponding base classes should be used during
	 *            evaluation instead. See above comment for further discussion.
	 *
	 *            For PR, instead of just numbers, we need to account for UniProt identifiers after
	 *            the colon. This is straightforward as UniProt identifiers follow a regular
	 *            expression.
	 */
	public static void translateExtensionToBaseNamespace(TextDocument document) {
		for (TextAnnotation annot : document.getAnnotations()) {
			ClassMention cm = annot.getClassMention();
			if (cm.getMentionName().matches("[A-Z]+_EXT:\\d+")) {
				String updatedIdentifier = cm.getMentionName().replace("_EXT", "");
				cm.setMentionName(updatedIdentifier);
			}
			/* handle PR separately by allowing UniProt identifiers after the colon */
			if (cm.getMentionName()
					.matches("PR_EXT:[OPQ][0-9][A-Z0-9]{3}[0-9]-?[0-9]*|[A-NR-Z][0-9]([A-Z][A-Z0-9]{2}[0-9]){1,2}-?[0-9]*")) {
				String updatedIdentifier = cm.getMentionName().replace("_EXT", "");
				cm.setMentionName(updatedIdentifier);
			}
		}
	}

	private static void writeSlotErrorRate(String documentId, SlotErrorRate ser, BufferedWriter writer)
			throws IOException {
		StringBuffer sb = new StringBuffer();
		sb.append(documentId);
		sb.append("\t" + ser.getSubstitutions().setScale(4, BigDecimal.ROUND_HALF_UP));
		sb.append("\t" + ser.getInsertions());
		sb.append("\t" + ser.getDeletions());
		sb.append("\t" + ser.getMatches().setScale(4, BigDecimal.ROUND_HALF_UP));
		sb.append("\t" + ser.getReferenceCount());
		sb.append("\t" + ser.getPredictedCount());
		sb.append("\t" + ser.getSER().setScale(4, BigDecimal.ROUND_HALF_UP));
		sb.append("\t" + ser.getPrecision().setScale(4, BigDecimal.ROUND_HALF_UP));
		sb.append("\t" + ser.getRecall().setScale(4, BigDecimal.ROUND_HALF_UP));
		sb.append("\t" + ser.getFScore().setScale(4, BigDecimal.ROUND_HALF_UP));
		writer.write(sb.toString() + "\n");
	}

}
