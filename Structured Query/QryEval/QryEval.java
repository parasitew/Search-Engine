/**
 *  QryEval illustrates the architecture for the portion of a search
 *  engine that evaluates queries.  It is a template for class
 *  homework assignments, so it emphasizes simplicity over efficiency.
 *  It implements an unranked Boolean retrieval model, however it is
 *  easily extended to other retrieval models.  For more information,
 *  see the ReadMe.txt file.
 *
 *  Copyright (c) 2015, Carnegie Mellon University.  All Rights Reserved.
 */

import org.apache.lucene.analysis.Analyzer.TokenStreamComponents;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.*;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import java.io.*;
import java.text.DecimalFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class QryEval {

	public static IndexReader READER;
	//  The index file reader is accessible via a global variable. This
	//  isn't great programming style, but the alternative is for every
	//  query operator to store or pass this value, which creates its
	//  own headaches.
	public static EnglishAnalyzerConfigurable analyzer =
			new EnglishAnalyzerConfigurable(Version.LUCENE_43);

	//  Create and configure an English analyzer that will be used for
	//  query parsing.
	static {
		analyzer.setLowercase(true);
		analyzer.setStopwordRemoval(true);
		analyzer.setStemmer(EnglishAnalyzerConfigurable.StemmerType.KSTEM);
	}

	static String usage = "Usage:  java " + System.getProperty("sun.java.command")
			+ " paramFile\n\n";

	static boolean enableFieldBasedRetrieval = false;

	/**
	 * @param args The only argument is the path to the parameter file.
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		/**
		 * **********************************
		 */

		// Input: 1(numParam) params result
		// must supply parameter file
		if (args.length < 1) {
			System.err.println(usage);
			System.exit(1);
		}

		/**
		 *  read in the parameter file; one parameter per line in format of key=value
		 */
		Map<String, String> params = new HashMap<String, String>();
		Scanner scan = new Scanner(new File(args[0]));
		String line = null;

		while (scan.hasNext()) {
			line = scan.nextLine();
			String[] pair = line.split("=");
			params.put(pair[0].trim(), pair[1].trim());
		}

		scan.close();

		String trecEvalOutputPath = params.get("trecEvalOutputPath");
		String queryFilePath = params.get("queryFilePath");
		String retrievalAlgorithm = params.get("retrievalAlgorithm");

		// parameters required for this example to run
		if (!params.containsKey("indexPath")) {
			System.err.println("Error: Parameters were missing.");
			System.exit(1);
		}

		// open the index
		READER = DirectoryReader.open(FSDirectory.open(new File(params.get("indexPath"))));

		if (READER == null) {
			System.err.println(usage);
			System.exit(1);
		}

		DocLengthStore s = new DocLengthStore(READER);

		RetrievalModel model = null;

		if ("UnrankedBoolean".equals(retrievalAlgorithm)) {
			model = new RetrievalModelUnrankedBoolean();
		} else if ("RankedBoolean".equals(retrievalAlgorithm)) {
			model = new RetrievalModelRankedBoolean();
		} else if ("BM25".equals(retrievalAlgorithm)) {
			model = new RetrievalModelBM25();

			((RetrievalModelBM25) model).setK1(Double.valueOf(params.get("BM25:k_1")));
			((RetrievalModelBM25) model).setB(Double.valueOf(params.get("BM25:b")));
			((RetrievalModelBM25) model).setK3(Double.valueOf(params.get("BM25:k_3")));
		} else if ("Indri".equals(retrievalAlgorithm)) {
			model = new RetrievalModelIndri();

			((RetrievalModelIndri) model).setMu(Double.valueOf(params.get("Indri:mu")));
			((RetrievalModelIndri) model).setLambda(Double.valueOf(params.get("Indri:lambda")));
		}
		//model = new RetrievalModelUnrankedBoolean();
		//model = new RetrievalModelRankedBoolean();

		BufferedWriter writer = new BufferedWriter(new FileWriter(trecEvalOutputPath));

		long startTime = System.currentTimeMillis();


		// Evaluate Query
		Scanner scanner = new Scanner(new File(params.get("queryFilePath")));
		while (scanner.hasNext()) {
			String str = scanner.nextLine();
			String[] strs = str.split(":");
			int queryNum = Integer.valueOf(strs[0]);
			String query = strs[1];

			Qryop qTree = parseQuery(query, model);
			writeResults(queryNum, query, qTree.evaluate(model), writer, model);
		}

		writer.close();
		scanner.close();

		long endTime = System.currentTimeMillis();
		System.out.println("Total evaluation time: " + (endTime - startTime) + " milliseconds");
		printMemoryUsage(false);

	}

	/**
	 * Write an error message and exit.  This can be done in other
	 * ways, but I wanted something that takes just one statement so
	 * that it is easy to insert checks without cluttering the code.
	 *
	 * @param message The error message to write before exiting.
	 * @return void
	 */
	static void fatalError(String message) {
		System.err.println(message);
		System.exit(1);
	}

	/**
	 * Get the external document id for a document specified by an
	 * internal document id. If the internal id doesn't exists, returns null.
	 *
	 * @param iid The internal document id of the document.
	 * @throws IOException
	 */
	static String getExternalDocid(int iid) throws IOException {
		Document d = QryEval.READER.document(iid);
		String eid = d.get("externalId");
		return eid;
	}

	/**
	 * Finds the internal document id for a document specified by its
	 * external id, e.g. clueweb09-enwp00-88-09710.  If no such
	 * document exists, it throws an exception.
	 *
	 * @param externalId The external document id of a document.s
	 * @return An internal doc id suitable for finding document vectors etc.
	 * @throws Exception
	 */
	static int getInternalDocid(String externalId) throws Exception {
		Query q = new TermQuery(new Term("externalId", externalId));

		IndexSearcher searcher = new IndexSearcher(QryEval.READER);
		TopScoreDocCollector collector = TopScoreDocCollector.create(1, false);
		searcher.search(q, collector);
		ScoreDoc[] hits = collector.topDocs().scoreDocs;

		if (hits.length < 1) {
			throw new Exception("External id not found.");
		} else {
			return hits[0].doc;
		}
	}

	/**
	 * parseQuery converts a query string into a query tree.
	 *
	 * @param qString A string containing a query.
	 * @return qTree   A query tree
	 * @throws IOException
	 */
	static Qryop parseQuery(String qString, RetrievalModel model) throws IOException {
		Set<String> fields = new HashSet<String>();
		fields.add("url");
		fields.add("keywords");
		fields.add("inlink");
		fields.add("title");
		fields.add("body");

		Qryop currentOp = null;
		Stack<Qryop> stack = new Stack<Qryop>();

		// Add a default query operator to an unstructured query. This
		// is a tiny bit easier if unnecessary whitespace is removed.

		qString = qString.trim();

//		if (qString.charAt(0) != '#') {
//			qString = "#or(" + qString + ")";
//		}

		if (model instanceof RetrievalModelBM25) {
			qString = "#sum(" + qString + ")";
		} else if (model instanceof RetrievalModelIndri) {
			qString = "#and(" + qString + ")";
		} else {
			qString = "#or(" + qString + ")";
		}

		// Tokenize the query.
		StringTokenizer tokens = new StringTokenizer(qString, "\t\n\r ,()", true);
		String token = null;

		// Each pass of the loop processes one token. To improve
		// efficiency and clarity, the query operator on the top of the
		// stack is also stored in currentOp.

		System.out.println(qString);
		String regNear = "(?i)(#near/)(\\d+)";
		String regWindow = "(?i)(#window/)(\\d+)";
		Pattern patternNear = Pattern.compile(regNear);
		Pattern patternWindow = Pattern.compile(regWindow);

		boolean isOperator = true;
		while (tokens.hasMoreTokens()) {

			token = tokens.nextToken();
			Matcher matcherNear = patternNear.matcher(token);
			Matcher matcherWinodw = patternWindow.matcher(token);

			if (token.matches("[ ,(\t\n\r]")) {
				// Ignore most delimiters.
			} else if (token.equalsIgnoreCase("#and")) {
				currentOp = new QryopSlAnd();
				stack.push(currentOp);
			} else if (token.equalsIgnoreCase("#or")) {
				currentOp = new QryopSlOr();
				stack.push(currentOp);
			} else if (token.equalsIgnoreCase("#sum")) {
				currentOp = new QryopSlSum();

				stack.push(currentOp);
			} else if (token.equalsIgnoreCase("#syn")) {
				currentOp = new QryopIlSyn();
				stack.push(currentOp);
			} else if (matcherNear.find()) {
				int dist = Integer.valueOf(matcherNear.group(2));
				currentOp = new QryopIlNear(dist);

				stack.push(currentOp);
			} else if (matcherWinodw.find()) {
                int windowSize = Integer.valueOf(matcherWinodw.group(2));
                currentOp = new QryopIlWindow(windowSize);
                System.out.println("cur:" + currentOp);
                stack.push(currentOp);
            } else if (token.startsWith(")")) { // Finish current query
				// operator.
				// If the current query operator is not an argument to
				// another query operator (i.e., the stack is empty when it
				// is removed), we're done (assuming correct syntax - see
				// below). Otherwise, add the current operator as an
				// argument to the higher-level operator, and shift
				// processing back to the higher-level operator.

				stack.pop();

				if (stack.empty())
					break;
				Qryop arg = currentOp;
				currentOp = stack.peek();
				currentOp.add(arg);
			} else {
				assert currentOp != null;

				// NOTE: You should do lexical processing of the token before
				// creating the query term, and you should check to see whether
				// the token specifies a particular field (e.g., apple.title).


				if (token.length() > 0) {

					String field = "body";
					String[] strs = new String[0];
					if (token.contains(".")) {
						String[] ss = token.split("\\.");
						if (fields.contains(ss[1])) {
							strs = tokenizeQuery(ss[0]);
							field = ss[1];
						}

					} else {
						strs = tokenizeQuery(token);
					}

					if (strs.length > 0) {
						currentOp.add(new QryopIlTerm(strs[0], field));
					}
				}
			}
		}

		// A broken structured query can leave unprocessed tokens on the
		// stack, so check for that.

		if (tokens.hasMoreTokens()) {
			System.err.println("Error:  Query syntax is incorrect.  " + qString);
			return null;
		}

		return currentOp;
	}

	/**
	 * Print a message indicating the amount of memory used.  The
	 * caller can indicate whether garbage collection should be
	 * performed, which slows the program but reduces memory usage.
	 *
	 * @param gc If true, run the garbage collector before reporting.
	 * @return void
	 */
	public static void printMemoryUsage(boolean gc) {

		Runtime runtime = Runtime.getRuntime();

		if (gc) {
			runtime.gc();
		}

		System.out.println("Memory used:  " +
				((runtime.totalMemory() - runtime.freeMemory()) /
						(1024L * 1024L)) + " MB");
	}

	/**
	 * Print the query results.
	 * <p/>
	 * THIS IS NOT THE CORRECT OUTPUT FORMAT.  YOU MUST CHANGE THIS
	 * METHOD SO THAT IT OUTPUTS IN THE FORMAT SPECIFIED IN THE HOMEWORK
	 * PAGE, WHICH IS:
	 * <p/>
	 * QueryID Q0 DocID Rank Score RunID
	 *
	 * @param queryName Original query.
	 * @param result    Result object generated by {ink Qryop#evaluate()}.
	 * @throws IOException
	 */
	static void printResults(String queryName, QryResult result) throws IOException {

		System.out.println(queryName + ":  ");
		if (result.docScores.scores.size() < 1) {
			System.out.println("\tNo results.");
		} else {
			for (int i = 0; i < result.docScores.scores.size(); i++) {
				System.out.println("\t" + i + ":  "
						+ getExternalDocid(result.docScores.getDocid(i))
						+ ", "
						+ result.docScores.getDocidScore(i));
			}
		}
	}

	/**
	 * Given a query string, returns the terms one at a time with stopwords
	 * removed and the terms stemmed using the Krovetz stemmer.
	 * <p/>
	 * Use this method to process raw query terms.
	 *
	 * @param query String containing query
	 * @return Array of query tokens
	 * @throws IOException
	 */
	static String[] tokenizeQuery(String query) throws IOException {

		TokenStreamComponents comp = analyzer.createComponents("dummy", new StringReader(query));
		TokenStream tokenStream = comp.getTokenStream();

		CharTermAttribute charTermAttribute = tokenStream.addAttribute(CharTermAttribute.class);
		tokenStream.reset();

		List<String> tokens = new ArrayList<String>();
		while (tokenStream.incrementToken()) {
			String term = charTermAttribute.toString();
			tokens.add(term);
		}
		return tokens.toArray(new String[tokens.size()]);
	}

	static void writeResults(int queryNum, String queryName, QryResult result, BufferedWriter writer, RetrievalModel
			model) throws
			IOException {

		// Sort by score.
		result.docScores.sortScoreList();
		System.out.println(queryName + ":  ");

		//if (model instanceof RetrievalModelRankedBoolean) {
		//	result.docScores.sortScoreList();
		//}

		if (result.docScores.scores.size() < 1) {
			System.out.println("\tNo results.");
		} else {
			int resultNum = result.docScores.scores.size();

			if (resultNum > 100) {
				resultNum = 100;
			}

			DecimalFormat format = new DecimalFormat("0.000000000000");
			for (int i = 0; i < resultNum; i++) {
				writer.write(queryNum + " Q0 " + getExternalDocid(result.docScores.getDocid(i)) + " " + (i + 1) + " " +
						format.format(result.docScores.getDocidScore(i)) + " fubar\n");
				System.out.println(queryNum + " Q0 " + getExternalDocid(result.docScores.getDocid(i)) + " " + (i + 1) +
						" " + format.format(result.docScores.getDocidScore(i)) + " fubar\n");
			}
		}
	}
}
