/**
 * QryEval illustrates the architecture for the portion of a search
 * engine that evaluates queries.    It is a template for class
 * homework assignments, so it emphasizes simplicity over efficiency.
 * It implements an unranked Boolean retrieval model, however it is
 * easily extended to other retrieval models.    For more information,
 * see the ReadMe.txt file.
 * <p/>
 * Copyright (c) 2015, Carnegie Mellon University.    All Rights Reserved.
 */

import org.apache.lucene.analysis.Analyzer.TokenStreamComponents;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
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
    public static DocLengthStore DLS;
    public static Set<Integer> featureFilter = new HashSet<Integer>();
    public static List<Integer> testDocSize = new ArrayList<Integer>();


    //    The index file reader is accessible via a global variable. This
    //    isn't great programming style, but the alternative is for every
    //    query operator to store or pass this value, which creates its
    //    own headaches.
    public static EnglishAnalyzerConfigurable analyzer =
            new EnglishAnalyzerConfigurable(Version.LUCENE_43);

    //    Create and configure an English analyzer that will be used for
    //    query parsing.
    static {
        analyzer.setLowercase(true);
        analyzer.setStopwordRemoval(true);
        analyzer.setStemmer(EnglishAnalyzerConfigurable.StemmerType.KSTEM);
    }

    static String usage = "Usage:    java " + System.getProperty("sun.java.command")
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
         *    read in the parameter file; one parameter per line in format of key=value
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
        DLS = new DocLengthStore(READER);

        if (READER == null) {
            System.err.println(usage);
            System.exit(1);
        }


        RetrievalModel model = null;
        RetrievalModel leToRBM25Model = null;
        RetrievalModel leToRIndriModel = null;
        LeToR letor = null;

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
            ((RetrievalModelIndri) model).setFb(Boolean.valueOf(params.get("fb")));

            if (((RetrievalModelIndri) model).isFb()) {
                ((RetrievalModelIndri) model).setFbDocs(Integer.valueOf(params.get("fbDocs")));
                ((RetrievalModelIndri) model).setFbTerms(Integer.valueOf(params.get("fbTerms")));
                ((RetrievalModelIndri) model).setFbMu(Integer.valueOf(params.get("fbMu")));
                ((RetrievalModelIndri) model).setFbOrigWeight(Double.valueOf(params.get("fbOrigWeight")));
                ((RetrievalModelIndri) model).setFbInitialRankingFile(params.get("fbInitialRankingFile"));
                ((RetrievalModelIndri) model).setFbExpansionQueryFile(params.get("fbExpansionQueryFile"));
            }
        } else if ("letor".equals(retrievalAlgorithm)) {
            letor = new LeToR();

            letor.setTrainingQueryFile(params.get("letor:trainingQueryFile"));
            letor.setTrainingQrelsFile(params.get("letor:trainingQrelsFile"));
            letor.setTrainingFeatureVectorsFile(params.get("letor:trainingFeatureVectorsFile"));
            letor.setPageRankFile(params.get("letor:pageRankFile"));
            letor.setFeatureDisable(params.get("letor:featureDisable"));
            letor.setSvmRankLearnPath(params.get("letor:svmRankLearnPath"));
            letor.setSvmRankClassifyPath(params.get("letor:svmRankClassifyPath"));
            letor.setSvmRankParamC(params.get("letor:svmRankParamC"));
            letor.setSvmRankModelFile(params.get("letor:svmRankModelFile"));
            letor.setTestingFeatureVectorsFile(params.get("letor:testingFeatureVectorsFile"));
            letor.setTestingDocumentScores(params.get("letor:testingDocumentScores"));

            if (letor.getFeatureDisable() != null && letor.getFeatureDisable().length() > 0) {
                String[] strs = letor.getFeatureDisable().split(",");

                for (String str : strs) {
                    featureFilter.add(Integer.valueOf(str));
                }
            }


            leToRBM25Model = new RetrievalModelBM25();

            ((RetrievalModelBM25) leToRBM25Model).setK1(Double.valueOf(params.get("BM25:k_1")));
            ((RetrievalModelBM25) leToRBM25Model).setB(Double.valueOf(params.get("BM25:b")));
            ((RetrievalModelBM25) leToRBM25Model).setK3(Double.valueOf(params.get("BM25:k_3")));

            leToRIndriModel = new RetrievalModelIndri();

            ((RetrievalModelIndri) leToRIndriModel).setMu(Double.valueOf(params.get("Indri:mu")));
            ((RetrievalModelIndri) leToRIndriModel).setLambda(Double.valueOf(params.get("Indri:lambda")));
        }

        if ("letor".equals(retrievalAlgorithm)) {
            List<DocScore> docScoreList = new ArrayList<DocScore>();
            /////////////////////////////////////////////////////////////////////////////////////////
            genreateTrainingFeatures(letor, leToRBM25Model, leToRIndriModel);

            Process trainProc = Runtime.getRuntime().exec(
                    new String[]{letor.getSvmRankLearnPath(), "-c", letor.getSvmRankParamC(),
                            letor.getTrainingFeatureVectorsFile(), letor.getSvmRankModelFile()});

            execSVM(trainProc);


            int queryCnt = genreateTestingFeatures(letor, leToRBM25Model, leToRIndriModel, docScoreList, queryFilePath);

            Process testProc = Runtime.getRuntime().exec(
                    new String[]{letor.getSvmRankClassifyPath(), letor.getTestingFeatureVectorsFile(),
                            letor.getSvmRankModelFile(), letor.getTestingDocumentScores()});

            execSVM(testProc);

            // Read docScores.
            List<Double> scoreList = new ArrayList<Double>();
            Scanner scanner = new Scanner(new File(letor.getTestingDocumentScores()));

            while (scanner.hasNext()) {
                double val = scanner.nextDouble();
                scoreList.add(val);
            }

            scanner.close();


            PrintWriter pwOutput = new PrintWriter(trecEvalOutputPath);

            int index = 0;
            for (int i = 0; i < queryCnt; i++) {
                int size = testDocSize.get(i);
                PriorityQueue<DocScore> pq = new PriorityQueue<DocScore>(size);

                for (int j = 0; j < size; j++) {
                    DocScore docScore = new DocScore();
                    docScore.setDocid(docScoreList.get(index).getDocid());
                    docScore.setQid(docScoreList.get(index).getQid());
                    docScore.setScore(scoreList.get(index));

                    pq.add(docScore);
                    index++;
                }

                for (int j = 0; j < size; j++) {
                    DocScore docScore = pq.poll();

                    DecimalFormat format = new DecimalFormat("0.000000000000");
                    pwOutput.println(docScore.getQid() + " Q0 " + docScore.getDocid() + " " + (j + 1) + " " +
                            format.format(docScore.getScore()) + " yubinletor");
                }
            }

            pwOutput.close();
            /////////////////////////////////////////////////////////////////////////////////////////
        } else {

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
                QryResult result = qTree.evaluate(model);
                writeResults(queryNum, query, result, writer, model);
            }

            writer.close();
            scanner.close();

            long endTime = System.currentTimeMillis();
            System.out.println("Total evaluation time: " + (endTime - startTime) + " milliseconds");
        }
        printMemoryUsage(false);
    }

    // Train quries.
    private static void genreateTrainingFeatures(LeToR letor, RetrievalModel leToRBM25Model,
                                                 RetrievalModel leToRIndriModel) throws Exception {
        Map<String, Double> pageRankMap = readPageRank(letor);

        Scanner scanner = new Scanner(new File(letor.getTrainingQueryFile()));
        PrintWriter pw = new PrintWriter(letor.getTrainingFeatureVectorsFile());

        while (scanner.hasNext()) {
            String str = scanner.nextLine();
            System.out.println(str);
            String[] strs = str.split(":");

            int queryNum = Integer.valueOf(strs[0]);
            String query = strs[1];

            String[] stems = QryEval.tokenizeQuery(query);

            // Get features for all docs of this query, write normalized features into file.
            writeQueryFeatures(queryNum, stems, pageRankMap, leToRBM25Model, leToRIndriModel, null, true, pw, null,
                    letor);
        }

        pw.close();
    }

    private static int genreateTestingFeatures(LeToR letor, RetrievalModel leToRBM25Model, RetrievalModel leToRIndriModel,
                                               List<DocScore> docScoreList, String queryFilePath) throws Exception {
        Map<String, Double> pageRankMap = readPageRank(letor);
        int queryCnt = 0;

        Scanner scanner = new Scanner(new File(queryFilePath));
        PrintWriter pw = new PrintWriter(letor.getTestingFeatureVectorsFile());


        while (scanner.hasNext()) {
            queryCnt++;
            String str = scanner.nextLine();
            String[] strs = str.split(":");

            System.out.println("----------  " + str);

            int queryNum = Integer.valueOf(strs[0]);
            String query = strs[1];

            String[] stems = QryEval.tokenizeQuery(query);

            // Get initial top 100 ranking with BM25.
            Qryop qTree = parseQuery(query, leToRBM25Model);
            QryResult result = qTree.evaluate(leToRBM25Model);

            // Get features for all docs of this query, write normalized features into file.
            writeQueryFeatures(queryNum, stems, pageRankMap, leToRBM25Model, leToRIndriModel, result, false, pw,
                    docScoreList, letor);
        }

        pw.close();

        return queryCnt;
    }

    private static void writeQueryFeatures(int queryNum,
                                           String[] stems,
                                           Map<String, Double> pageRankMap,
                                           RetrievalModel leToRBM25Model,
                                           RetrievalModel leToRIndriModel,
                                           QryResult result,
                                           boolean isTrain,
                                           PrintWriter pw,
                                           List<DocScore> docScoreList,
                                           LeToR letor) throws Exception {
        List<List<Double>> res = new ArrayList<List<Double>>();
        boolean hasDocs = false;
        List<String> externDocidList = new ArrayList<String>();
        List<Integer> relevanceList = new ArrayList<Integer>();

        if (isTrain) {
            Scanner sc = new Scanner(new FileReader(letor.getTrainingQrelsFile()));

            // For each docment of this query
            while (sc.hasNext()) {
                List<Double> features;

                String line = sc.nextLine();
                String[] strs = line.split(" ");

                int qid = Integer.valueOf(strs[0]);

                if (qid == queryNum) {
                    hasDocs = true;

                    String externalDocid = strs[2];
                    int relevance = Integer.valueOf(strs[3]);

                    features = createDocFeatures(externalDocid, stems, pageRankMap, leToRBM25Model, leToRIndriModel);

                    externDocidList.add(externalDocid);
                    relevanceList.add(relevance);
                    res.add(features);
                } else if (hasDocs == true) {
                    break;
                }
            }

            sc.close();

            // Return if no docs for this query.
            if (res.size() == 0) {
                return;
            }

            // Normalize.
            double[] min = new double[18];
            double[] max = new double[18];

            for (int i = 0; i < 18; i++) {
                min[i] = Double.MAX_VALUE;
                max[i] = Double.MIN_VALUE;
            }

            for (int i = 0; i < 18; i++) {
                for (int j = 0; j < res.size(); j++) {
                    double val = res.get(j).get(i);

                    if (!Double.isNaN(val)) {
                        if (val < min[i]) {
                            min[i] = val;
                        }

                        if (val > max[i]) {
                            max[i] = val;
                        }
                    }
                }
            }

            for (int i = 0; i < res.size(); i++) {
                for (int j = 0; j < res.get(i).size(); j++) {
                    double val = res.get(i).get(j);
                    double score = 0.0;

                    if (!Double.isNaN(val) && max[j] != min[j]) {
                        score = (val - min[j]) / (max[j] - min[j]);
                    } else {
                        score = 0.0;
                    }

                    res.get(i).set(j, score);
                }
            }

            for (int i = 0; i < res.size(); i++) {
                String str = generateTrainString(queryNum, externDocidList.get(i), relevanceList.get(i), res.get(i));
                pw.println(str);
            }

        } else {
            result.docScores.sortScoreList();

            int size = 100;

            if (result.docScores.scores.size() < size) {
                size = result.docScores.scores.size();
            }

            testDocSize.add(size);

            for (int i = 0; i < size; i++) {
                List<Double> features;

                String externalDocid = getExternalDocid(result.docScores.getDocid(i));
                features = createDocFeatures(externalDocid, stems, pageRankMap, leToRBM25Model, leToRIndriModel);

                externDocidList.add(externalDocid);
                res.add(features);

                DocScore docScore = new DocScore();
                docScore.setDocid(externalDocid);
                docScore.setQid(queryNum);
                docScoreList.add(docScore);
            }

            // Normalize.
            double[] min = new double[18];
            double[] max = new double[18];

            for (int i = 0; i < 18; i++) {
                min[i] = Double.MAX_VALUE;
                max[i] = Double.MIN_VALUE;
            }

            for (int i = 0; i < 18; i++) {
                for (int j = 0; j < res.size(); j++) {
                    double val = res.get(j).get(i);

                    if (!Double.isNaN(val)) {
                        if (val < min[i]) {
                            min[i] = val;
                        }

                        if (val > max[i]) {
                            max[i] = val;
                        }
                    }
                }
            }

            for (int i = 0; i < res.size(); i++) {
                for (int j = 0; j < res.get(i).size(); j++) {
                    double val = res.get(i).get(j);
                    double score = 0.0;

                    if (!Double.isNaN(val) && max[j] != min[j]) {
                        score = (val - min[j]) / (max[j] - min[j]);
                    } else {
                        score = 0.0;
                    }

                    res.get(i).set(j, score);
                }
            }

            for (int i = 0; i < res.size(); i++) {
                String str = generateTestString(queryNum, externDocidList.get(i), res.get(i));
                pw.println(str);
            }
        }
    }

    private static String generateTrainString(int qid, String externalDocid, int relevance, List<Double> features) {
        StringBuffer sb = new StringBuffer();

        sb.append(relevance);

        sb.append(" qid:" + qid);

        for (int i = 0; i < features.size(); i++) {
            if (!featureFilter.contains(i + 1)) {
                sb.append(" " + (i + 1) + ":" + features.get(i));
            }
        }

        sb.append(" # " + externalDocid);

        return sb.toString();
    }

    private static String generateTestString(int qid, String externalDocid, List<Double> features) {
        StringBuffer sb = new StringBuffer();

        sb.append(0);

        sb.append(" qid:" + qid);

        for (int i = 0; i < features.size(); i++) {
            if (!featureFilter.contains(i + 1)) {
                sb.append(" " + (i + 1) + ":" + features.get(i));
            }
        }

        sb.append(" # " + externalDocid);

        return sb.toString();
    }

    private static List<Double> createDocFeatures(String externalDocid,
                                                  String[] stems,
                                                  Map<String, Double> pageRank,
                                                  RetrievalModel leToRBM25Model,
                                                  RetrievalModel leToRIndriModel) throws Exception {

        List<Double> features = new ArrayList<Double>();

        int internalDocid = QryEval.getInternalDocid(externalDocid);
        Document doc = QryEval.READER.document(internalDocid);

        // f1: Spam score for d (read from index).
        int spamScore = Integer.valueOf(doc.get("score"));
        features.add((double) spamScore);

        // f2: Url depth for d(number of '/' in the rawUrl field).
        String rawUrl = doc.get("rawUrl");
        int urlDepth = 0;

        for (char c : rawUrl.toCharArray()) {
            if (c == '/') {
                urlDepth++;
            }
        }

        features.add((double) urlDepth);

        // f3: FromWikipedia score for d (1 if the rawUrl contains "wikipedia.org", otherwise 0).
        if (rawUrl.contains("wikipedia.org")) {
            features.add(1.0);
        } else {
            features.add(0.0);
        }

        // f4: PageRank score for d (read from file).
        if (pageRank.containsKey(externalDocid)) {
            features.add(pageRank.get(externalDocid));
        } else {
            features.add(Double.NaN);
        }


        TermVector tv = null;
        Terms terms = null;


        terms = QryEval.READER.getTermVector(internalDocid, "body");
        if (terms != null) {
            tv = new TermVector(internalDocid, "body");

            // f5: BM25 score for <q, dbody>.
            features.add(leToRBM25Score(internalDocid, stems, tv, leToRBM25Model, "body"));
            // f6: Indri score for <q, dbody>.
            features.add(leToRIndriScore(internalDocid, stems, tv, leToRIndriModel, "body"));
            // f7: Term overlap score for <q, dbody>.
            features.add(leToROverlapScore(stems, tv));
        } else {
            features.add(Double.NaN);
            features.add(Double.NaN);
            features.add(Double.NaN);
        }

        terms = QryEval.READER.getTermVector(internalDocid, "title");
        if (terms != null) {
            tv = new TermVector(internalDocid, "title");

            // f8: BM25 score for <q, dtitle>.
            features.add(leToRBM25Score(internalDocid, stems, tv, leToRBM25Model, "title"));
            // f9: Indri score for <q, dtitle>. v
            features.add(leToRIndriScore(internalDocid, stems, tv, leToRIndriModel, "title"));
            // f10: Term overlap score for <q, dtitle>.
            features.add(leToROverlapScore(stems, tv));
        } else {
            features.add(Double.NaN);
            features.add(Double.NaN);
            features.add(Double.NaN);
        }


        terms = QryEval.READER.getTermVector(internalDocid, "url");
        if (terms != null) {
            tv = new TermVector(internalDocid, "url");

            // f11: BM25 score for <q, durl>.
            features.add(leToRBM25Score(internalDocid, stems, tv, leToRBM25Model, "url"));
            // f12: Indri score for <q, durl>.
            features.add(leToRIndriScore(internalDocid, stems, tv, leToRIndriModel, "url"));
            // f13: Term overlap score for <q, durl>.
            features.add(leToROverlapScore(stems, tv));
        } else {
            features.add(Double.NaN);
            features.add(Double.NaN);
            features.add(Double.NaN);
        }

        terms = QryEval.READER.getTermVector(internalDocid, "inlink");
        if (terms != null) {
            tv = new TermVector(internalDocid, "inlink");

            // f14: BM25 score for <q, dinlink>.
            features.add(leToRBM25Score(internalDocid, stems, tv, leToRBM25Model, "inlink"));
            // f15: Indri score for <q, dinlink>.
            features.add(leToRIndriScore(internalDocid, stems, tv, leToRIndriModel, "inlink"));
            // f16: Term overlap score for <q, dinlink>.
            features.add(leToROverlapScore(stems, tv));
        } else {
            features.add(Double.NaN);
            features.add(Double.NaN);
            features.add(Double.NaN);
        }

        // f17: tf-idf
        terms = QryEval.READER.getTermVector(internalDocid, "body");
        if (terms != null) {
            tv = new TermVector(internalDocid, "body");
            features.add(getTfidf(internalDocid, stems, tv));
        } else {
            features.add(Double.NaN);
        }

        // f18: vsm
        if (terms != null) {
            features.add(getVsm(internalDocid, stems, tv));
        } else {
            features.add(Double.NaN);
        }

        return features;
    }

    private static double leToRIndriScore(int docid, String[] stems, TermVector tv, RetrievalModel model, String field)
            throws IOException {
        double result = 1.0;

        double stemNum = (double) stems.length;
        double contextLen = (double) QryEval.READER.getSumTotalTermFreq(field);
        RetrievalModelIndri r = (RetrievalModelIndri) model;
        double docLen = (double) DLS.getDocLength(field, docid);
        boolean hasCommonStems = false;

        for (String stem : stems) {
            int stemIdx = tv.getStemIdx(stem);

            double ctf = QryEval.READER.totalTermFreq(new Term(field, stem));
            double tf = 0.0;

            if (stemIdx != -1) {
                hasCommonStems = true;
                ctf = tv.totalStemFreq(stemIdx);
                tf = (double) tv.stemFreq(stemIdx);
            }

            double mle = ctf / contextLen;
            double score = 0.0;

            score += (1 - r.getLambda()) * (tf + r.getMu() * mle) / (docLen + r.getMu());
            score += (r.getLambda()) * mle;

            result *= Math.pow(score, 1.0 / stemNum);
        }

        if (!hasCommonStems) {
            //System.out.println("Has no common stems");
            return 0.0;
        }

        return result;
    }

    private static double leToROverlapScore(String[] stems, TermVector tv) {
        int result = 0;

        for (String stem : stems) {
            if (tv.getStemIdx(stem) != -1) {
                result++;
            }
        }

        return ((double) result) / stems.length;
    }

    private static double leToRBM25Score(int docid, String[] stems, TermVector tv, RetrievalModel model, String field)
            throws IOException {
        double result = 0.0;

        double N = (double) QryEval.READER.numDocs();
        double avgLen = QryEval.READER.getSumTotalTermFreq(field) / (double) QryEval.READER.getDocCount(field);
        double b = ((RetrievalModelBM25) model).getB();
        double k1 = ((RetrievalModelBM25) model).getK1();
        double k3 = ((RetrievalModelBM25) model).getK3();

        double docLen = (double) DLS.getDocLength(field, docid);

        for (String stem : stems) {
            int stemIdx = tv.getStemIdx(stem);

            if (stemIdx != -1) {
                double df = tv.stemDf(stemIdx);
                double tf = tv.stemFreq(stemIdx);
                double score1 = Math.log((N - df + 0.5) / (df + 0.5));

                score1 = Math.max(0, score1);

                double score2 = tf / (tf + k1 * ((1 - b) + b * (docLen / avgLen)));
                double score3 = (k3 + 1) / (k3 + 1);

                result += score1 * score2 * score3;
            }
        }

        return result;
    }

    private static Map<String, Double> readPageRank(LeToR letor) throws FileNotFoundException {
        Scanner sc = new Scanner(new FileReader(letor.getPageRankFile()));
        Map<String, Double> hashMap = new HashMap<String, Double>();

        while (sc.hasNext()) {
            String line = sc.nextLine();
            String[] strs = line.split("\t");

            hashMap.put(strs[0], Double.valueOf(strs[1]));
        }

        return hashMap;
    }

    /**
     * Write an error message and exit.    This can be done in other
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
     * external id, e.g. clueweb09-enwp00-88-09710.    If no such
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
     * @return qTree    A query tree
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

//        if (qString.charAt(0) != '#') {
//            qString = "#or(" + qString + ")";
//        }

        if (model instanceof RetrievalModelBM25) {
            qString = "#sum(" + qString + ")";
        } else if (model instanceof RetrievalModelIndri) {
            qString = "#and(" + qString + ")";
        } else {
            qString = "#AND(" + qString + ")";
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

        double weight = 0;

        while (tokens.hasMoreTokens()) {
            token = tokens.nextToken();

            Matcher matcherNear = patternNear.matcher(token);
            Matcher matcherWinodw = patternWindow.matcher(token);


            token = token.trim();

            if (!")".equals(token) && !"(".equals(token) && token.length() > 0) {
                if (!stack.isEmpty() && stack.peek().hasWeight && stack.peek().isWeight) {
                    weight = Double.valueOf(token);
                    stack.peek().changeState();

                    continue;
                }
            }

            if (token.matches("[ ,(\t\n\r]")) {
                // Ignore most delimiters.
            } else if (token.equalsIgnoreCase("#and")) {
                currentOp = new QryopSlAnd();

                currentOp.weight = weight;

                stack.push(currentOp);
            } else if (token.equalsIgnoreCase("#or")) {
                currentOp = new QryopSlOr();

                currentOp.weight = weight;

                stack.push(currentOp);
            } else if (token.equalsIgnoreCase("#sum")) {
                currentOp = new QryopSlSum();

                currentOp.weight = weight;

                stack.push(currentOp);
            } else if (token.equalsIgnoreCase("#wand")) {
                currentOp = new QryopSlWand();

                currentOp.weight = weight;

                stack.push(currentOp);
            } else if (token.equalsIgnoreCase("#wsum")) {
                currentOp = new QryopSlWsum();

                currentOp.weight = weight;

                stack.push(currentOp);
            } else if (token.equalsIgnoreCase("#syn")) {
                currentOp = new QryopIlSyn();

                currentOp.weight = weight;

                stack.push(currentOp);
            } else if (matcherNear.find()) {
                int dist = Integer.valueOf(matcherNear.group(2));

                currentOp = new QryopIlNear(dist);

                currentOp.weight = weight;

                stack.push(currentOp);
            } else if (matcherWinodw.find()) {
                int windowSize = Integer.valueOf(matcherWinodw.group(2));
                currentOp = new QryopIlWindow(windowSize);

                currentOp.weight = weight;


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

                if (stack.empty()) {
                    break;
                }

                Qryop arg = currentOp;
                currentOp = stack.peek();

                currentOp.add(arg);
                currentOp.changeState();
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
                        currentOp.add(new QryopIlTerm(strs[0], field, weight));
                    }

                    currentOp.changeState();
                }
            }
        }

        // A broken structured query can leave unprocessed tokens on the
        // stack, so check for that.

        if (tokens.hasMoreTokens()) {
            System.err.println("Error:    Query syntax is incorrect.    " + qString);
            return null;
        }

        return currentOp;
    }

    /**
     * Print a message indicating the amount of memory used.    The
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

        System.out.println("Memory used:    " +
                ((runtime.totalMemory() - runtime.freeMemory()) /
                        (1024L * 1024L)) + " MB");
    }

    /**
     * Print the query results.
     * <p/>
     * THIS IS NOT THE CORRECT OUTPUT FORMAT.    YOU MUST CHANGE THIS
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

        System.out.println(queryName + ":    ");
        if (result.docScores.scores.size() < 1) {
            System.out.println("\tNo results.");
        } else {
            for (int i = 0; i < result.docScores.scores.size(); i++) {
                System.out.println("\t" + i + ":    "
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
        System.out.println(queryName + ":    ");

        //if (model instanceof RetrievalModelRankedBoolean) {
        //    result.docScores.sortScoreList();
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

    private static double getTfidf(int internalDocid, String[] stems, TermVector tv) throws IOException {
        double score = 0.0;

        int N = QryEval.READER.numDocs();

        for (String stem : stems) {
            int stemIdx = tv.getStemIdx(stem);

            if (stemIdx != -1) {
                double tf = tv.stemFreq(stemIdx);
                double df = tv.stemDf(stemIdx);

                score += tf * Math.log((N - df + 0.5) / (df + 0.5));
            }
        }

        return score;
    }

    private static double getVsm(int internalDocid, String[] stems, TermVector tv) throws IOException {
        double score = 0.0;

        int N = QryEval.READER.numDocs();

        double docLen = 0.0;
        double qryLen = 0.0;

        for (String stem : stems) {
            int stemIdx = tv.getStemIdx(stem);
            double df = QryEval.READER.docFreq(new Term("title", stem));
            double tf = 0.0;

            if (stemIdx != -1) {
                tf = tv.stemFreq(stemIdx);
            }

            score += (Math.log(tf) + 1) * Math.log(N / df);

            docLen += Math.pow(Math.log(tf) + 1, 2);

            qryLen += Math.pow(Math.log(N / df), 2);
        }

        return score / Math.sqrt(docLen * qryLen);
    }

    private static void execSVM(Process cmdProc) throws Exception {

        // consume stdout and print it out for debugging purposes
        BufferedReader stdoutReader = new BufferedReader(
                new InputStreamReader(cmdProc.getInputStream()));

        String line;
        while ((line = stdoutReader.readLine()) != null) {
            System.out.println(line);
        }
        // consume stderr and print it for debugging purposes
        BufferedReader stderrReader = new BufferedReader(
                new InputStreamReader(cmdProc.getErrorStream()));

        while ((line = stderrReader.readLine()) != null) {
            System.out.println(line);
        }

        // get the return value from the executable. 0 means success, non-zero
        // indicates a problem
        int retValue = cmdProc.waitFor();
        if (retValue != 0) {
            throw new Exception("SVM Rank crashed.");
        }
    }

    static class DocScore implements Comparable {
        private String docid;
        private int qid;
        private double score;

        public double getScore() {
            return score;
        }

        public void setScore(double score) {
            this.score = score;
        }

        public int getQid() {
            return qid;
        }

        public void setQid(int qid) {
            this.qid = qid;
        }

        public String getDocid() {
            return docid;
        }

        public void setDocid(String docid) {
            this.docid = docid;
        }

        @Override
        public int compareTo(Object o) {
            DocScore that = (DocScore) o;

            if (this.score < that.score) {
                return 1;
            } else if (this.score > that.score) {
                return -1;
            }

            return 0;
        }
    }
}
