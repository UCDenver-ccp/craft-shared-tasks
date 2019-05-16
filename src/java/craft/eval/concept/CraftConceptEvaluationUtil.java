package craft.eval.concept;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
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

import org.apache.commons.io.IOUtils;

import edu.ucdenver.ccp.common.collections.CollectionsUtil;
import edu.ucdenver.ccp.common.file.CharacterEncoding;
import edu.ucdenver.ccp.common.file.FileUtil;
import edu.ucdenver.ccp.common.file.FileWriterUtil;
import edu.ucdenver.ccp.common.file.reader.StreamLineIterator;
import edu.ucdenver.ccp.file.conversion.TextDocument;
import edu.ucdenver.ccp.file.conversion.bionlp.BioNLPDocumentReader;
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
			String base = (this.name().endsWith("_EXT")) ? this.name().substring(0, this.name().length()-4)
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
				// convert the ontology to a string so that the zip file can be closed
				ZipFile zipFile = new ZipFile(ontologyFile);
				ZipEntry zipEntry = zipFile.entries().nextElement();
				InputStream inputStream = zipFile.getInputStream(zipEntry);
				String s = IOUtils.toString(inputStream, encoding.getCharacterSetName());
				zipFile.close();
				return new ByteArrayInputStream(s.getBytes());
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
					SlotErrorRate ser = bm.evaluate(goldDocument.getAnnotations(), testDocument.getAnnotations(),
							boundaryMatchStrategy);
					writeSlotErrorRate(sourceId, ser, writer);
					totalSer.update(ser);
				}
			}
			writeSlotErrorRate("TOTAL", totalSer, writer);
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

	public static void main(String[] args) {

		File craftDistributionDirectory = new File(
				"/Users/bill/Dropbox/work/projects/craft-shared-task-2019/CRAFT.bill.git");
		File referenceDirectoryBase = new File(
				"/Users/bill/Dropbox/work/projects/craft-shared-task-2019/sample-test-data/concept-gold/bionlp");
		File testDirectoryBase = new File(
				"/Users/bill/Dropbox/work/projects/craft-shared-task-2019/sample-test-data/concept");

		try {

			// File ontFile = new File(
			// "/Users/bill/Dropbox/work/projects/craft-shared-task-2019/CRAFT.bill.git/concept-annotation/CL/CL/CL.obo.zip");
			// File ontFile = new File("/Users/bill/Downloads/CL+extensions.obo.zip");
			List<File> ontFiles = CollectionsUtil.createList(
					// new File(
					// "/Users/bill/Dropbox/work/projects/craft-shared-task-2019/CRAFT.bill.git/concept-annotation/CL/CL/CL.obo.zip"),
					// new File(
					// "/Users/bill/Dropbox/work/projects/craft-shared-task-2019/CRAFT.bill.git/concept-annotation/CL/CL+extensions/CL+extensions.obo.zip"),
					new File(
							"/Users/bill/Dropbox/work/projects/craft-shared-task-2019/CRAFT.bill.git/concept-annotation/CHEBI/CHEBI/CHEBI.obo.zip"),
					new File(
							"/Users/bill/Dropbox/work/projects/craft-shared-task-2019/CRAFT.bill.git/concept-annotation/CHEBI/CHEBI+extensions/CHEBI+extensions.obo.zip"),
					new File(
							"/Users/bill/Dropbox/work/projects/craft-shared-task-2019/CRAFT.bill.git/concept-annotation/GO_BP/GO_BP/GO.obo.zip"),
					new File(
							"/Users/bill/Dropbox/work/projects/craft-shared-task-2019/CRAFT.bill.git/concept-annotation/GO_BP/GO_BP+extensions/GO+GO_BP_extensions.obo.zip"),
					new File(
							"/Users/bill/Dropbox/work/projects/craft-shared-task-2019/CRAFT.bill.git/concept-annotation/GO_CC/GO_CC/GO.obo.zip"),
					new File(
							"/Users/bill/Dropbox/work/projects/craft-shared-task-2019/CRAFT.bill.git/concept-annotation/GO_CC/GO_CC+extensions/GO+GO_CC_extensions.obo.zip"),
					new File(
							"/Users/bill/Dropbox/work/projects/craft-shared-task-2019/CRAFT.bill.git/concept-annotation/GO_MF/GO_MF/GO_MF_stub.obo.zip"),
					new File(
							"/Users/bill/Dropbox/work/projects/craft-shared-task-2019/CRAFT.bill.git/concept-annotation/GO_MF/GO_MF+extensions/GO_MF_stub+GO_MF_extensions.obo.zip"),
					new File(
							"/Users/bill/Dropbox/work/projects/craft-shared-task-2019/CRAFT.bill.git/concept-annotation/MOP/MOP/MOP.obo.zip"),
					new File(
							"/Users/bill/Dropbox/work/projects/craft-shared-task-2019/CRAFT.bill.git/concept-annotation/MOP/MOP+extensions/MOP+extensions.obo.zip"),
					new File(
							"/Users/bill/Dropbox/work/projects/craft-shared-task-2019/CRAFT.bill.git/concept-annotation/NCBITaxon/NCBITaxon/NCBITaxon.obo.zip"),
					new File(
							"/Users/bill/Dropbox/work/projects/craft-shared-task-2019/CRAFT.bill.git/concept-annotation/NCBITaxon/NCBITaxon+extensions/NCBITaxon+extensions.obo.zip"),
					new File(
							"/Users/bill/Dropbox/work/projects/craft-shared-task-2019/CRAFT.bill.git/concept-annotation/PR/PR/PR.obo.zip"),
					new File(
							"/Users/bill/Dropbox/work/projects/craft-shared-task-2019/CRAFT.bill.git/concept-annotation/PR/PR+extensions/PR+extensions.obo.zip"),
					new File(
							"/Users/bill/Dropbox/work/projects/craft-shared-task-2019/CRAFT.bill.git/concept-annotation/SO/SO/SO.obo.zip"),
					new File(
							"/Users/bill/Dropbox/work/projects/craft-shared-task-2019/CRAFT.bill.git/concept-annotation/SO/SO+extensions/SO+extensions.obo.zip"),
					new File(
							"/Users/bill/Dropbox/work/projects/craft-shared-task-2019/CRAFT.bill.git/concept-annotation/UBERON/UBERON/UBERON.obo.zip"),
					new File(
							"/Users/bill/Dropbox/work/projects/craft-shared-task-2019/CRAFT.bill.git/concept-annotation/UBERON/UBERON+extensions/UBERON+extensions.obo.zip")

			);
			for (File ontFile : ontFiles) {
				System.out.println(ontFile);
				ZipFile zipFile = new ZipFile(ontFile);
				ZipEntry zipEntry = zipFile.entries().nextElement();
				InputStream inputStream = zipFile.getInputStream(zipEntry);

				for (StreamLineIterator lineIter = new StreamLineIterator(inputStream, CharacterEncoding.UTF_8,
						null); lineIter.hasNext();) {
					lineIter.next().getText();
				}
				zipFile.close();
			}
			// evaluate(craftDistributionDirectory, referenceDirectoryBase, testDirectoryBase,
			// BoundaryMatchStrategy.JACCARD, CharacterEncoding.UTF_8);
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

}
