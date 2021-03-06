/**
 *  This class implements the SCORE operator for all retrieval models.
 *  The single argument to a score operator is a query operator that
 *  produces an inverted list.  The SCORE operator uses this
 *  information to produce a score list that contains document ids and
 *  scores.
 *
 *  Copyright (c) 2015, Carnegie Mellon University.  All Rights Reserved.
 */

import java.io.IOException;
import java.util.Iterator;

public class QryopSlScore extends QryopSl {
	private int ctf = 0;
	private String field = "body";
	private DocLengthStore dls;

	/**
	 * Construct a new SCORE operator.  The SCORE operator accepts just
	 * one argument.
	 *
	 * @param q The query operator argument.
	 * @return @link{QryopSlScore}
	 */
	public QryopSlScore(Qryop q) throws IOException {
		this();
		this.args.add(q);
	}

	/**
	 * Construct a new SCORE operator.  Allow a SCORE operator to be
	 * created with no arguments.  This simplifies the design of some
	 * query parsing architectures.
	 *
	 * @return @link{QryopSlScore}
	 */
	public QryopSlScore() throws IOException {
		this.dls = new DocLengthStore(QryEval.READER);
	}

	/**
	 * Appends an argument to the list of query operator arguments.  This
	 * simplifies the design of some query parsing architectures.
	 *
	 * @param q The query argument to append.
	 */
	public void add(Qryop q) {
		this.args.add(q);
	}

	/**
	 * Evaluate the query operator.
	 *
	 * @param r A retrieval model that controls how the operator behaves.
	 * @return The result of evaluating the query.
	 * @throws IOException
	 */
	public QryResult evaluate(RetrievalModel r) throws IOException {

		if (r instanceof RetrievalModelUnrankedBoolean) {
			return (evaluateBoolean(r));
		} else if (r instanceof RetrievalModelRankedBoolean) {
			return evaluateRankedBoolean(r);
		} else if (r instanceof RetrievalModelBM25) {
			return evaluateBM25(r);
		} else if (r instanceof RetrievalModelIndri) {
			return evaluateIndri((RetrievalModelIndri) r);
		}

		return null;
	}

	private QryResult evaluateIndri(RetrievalModelIndri r) throws IOException {

		QryResult result = args.get(0).evaluate(r);

		this.field = result.invertedList.field;
		double contextLen = (double) QryEval.READER.getSumTotalTermFreq(this.field);
		this.ctf = result.invertedList.ctf;
		double mle = this.ctf / contextLen;

		for (int i = 0; i < result.invertedList.df; i++) {
			int docid = result.invertedList.postings.get(i).docid;
			double docLen = (double) this.dls.getDocLength(field, docid);
			double tf = (double) result.invertedList.postings.get(i).tf;
			double score = 0.0;

			score += (1 - r.getLambda()) * (tf + r.getMu() * mle) / (docLen + r.getMu());
			score += r.getLambda() * mle;

			result.docScores.add(docid, score);
		}

		// Delete the inverted lsit.
		if (result.invertedList.df > 0)
			result.invertedList = new InvList();

		return result;
	}

	private QryResult evaluateBM25(RetrievalModel r) throws IOException {

		// Get inverted list of the term.
		QryResult result = args.get(0).evaluate(r);
		this.field = result.invertedList.field;
		// Each pass of the loop computes a score for one document. Note:
		// If the evaluate operation above returned a score list (which is
		// very possible), this loop gets skipped.
		double N = (double) QryEval.READER.numDocs();
		double avgLen = QryEval.READER.getSumTotalTermFreq(this.field) / (double) QryEval.READER.getDocCount(this.field);
		double df = (double) result.invertedList.df;
		double b = ((RetrievalModelBM25) r).getB();
		double k1 = ((RetrievalModelBM25) r).getK1();
		double k3 = ((RetrievalModelBM25) r).getK3();

		for (int i = 0; i < result.invertedList.df; i++) {
			// BM25
			int docid = result.invertedList.postings.get(i).docid;

			double docLen = (double) this.dls.getDocLength(field, docid);
			double tf = (double) result.invertedList.postings.get(i).tf;
			double score1 = Math.log((N - df + 0.5) / (df + 0.5));

			double score2 = tf / (tf + k1 * ((1 - b) + b * (docLen / avgLen)));
			double score3 = (k3 + 1) / (k3 + 1);

			result.docScores.add(docid, score1 * score2 * score3);
		}

		// Delete the inverted lsit.
		if (result.invertedList.df > 0)
			result.invertedList = new InvList();

		return result;
	}

	/**
	 * Evaluate the query operator for boolean retrieval models.
	 *
	 * @param r A retrieval model that controls how the operator behaves.
	 * @return The result of evaluating the query.
	 * @throws IOException
	 */
	public QryResult evaluateBoolean(RetrievalModel r) throws IOException {

		// Evaluate the query argument.

		QryResult result = args.get(0).evaluate(r);

		// Each pass of the loop computes a score for one document. Note:
		// If the evaluate operation above returned a score list (which is
		// very possible), this loop gets skipped.

		for (int i = 0; i < result.invertedList.df; i++) {

			// DIFFERENT RETRIEVAL MODELS IMPLEMENT THIS DIFFERENTLY.
			// Unranked Boolean. All matching documents get a score of 1.0.

			result.docScores.add(result.invertedList.postings.get(i).docid,
					(float) 1.0);
		}

		// The SCORE operator should not return a populated inverted list.
		// If there is one, replace it with an empty inverted list.

		if (result.invertedList.df > 0)
			result.invertedList = new InvList();

		return result;
	}

	public QryResult evaluateRankedBoolean(RetrievalModel r) throws IOException {

		// Evaluate the query argument.

		QryResult result = args.get(0).evaluate(r);

		// Each pass of the loop computes a score for one document. Note:
		// If the evaluate operation above returned a score list (which is
		// very possible), this loop gets skipped.

		for (int i = 0; i < result.invertedList.df; i++) {

			// DIFFERENT RETRIEVAL MODELS IMPLEMENT THIS DIFFERENTLY.
			// Ranked Boolean.
			result.docScores.add(result.invertedList.postings.get(i).docid,
					(float) result.invertedList.postings.get(i).tf);
		}

		// The SCORE operator should not return a populated inverted list.
		// If there is one, replace it with an empty inverted list.

		if (result.invertedList.df > 0)
			result.invertedList = new InvList();

		return result;
	}

	/*
	   *  Calculate the default score for a document that does not match
	   *  the query argument.  This score is 0 for many retrieval models,
	   *  but not all retrieval models.
	   *  @param r A retrieval model that controls how the operator behaves.
	   *  @param docid The internal id of the document that needs a default score.
	   *  @return The default score.
	   */
	public double getDefaultScore(RetrievalModel r, long docid) throws IOException {

		if (r instanceof RetrievalModelUnrankedBoolean) {
			return (0.0);
		} else if (r instanceof RetrievalModelIndri) {
			return getDefaultScoreIndri((RetrievalModelIndri) r, docid);
		}

		return 0.0;
	}

	private double getDefaultScoreIndri(RetrievalModelIndri r, long docid) throws IOException {
		double contextLen = (double) QryEval.READER.getSumTotalTermFreq(this.field);
		double mle = this.ctf / contextLen;

		double docLen = (double) dls.getDocLength(this.field, (int) docid);
		double score = 0.0;

		score += (1 - r.getLambda()) * (0 + r.getMu() * mle) / (docLen + r.getMu());
		score += r.getLambda() * mle;

		return score;
	}

	/**
	 * Return a string version of this query operator.
	 *
	 * @return The string version of this query operator.
	 */
	public String toString() {

		String result = new String();

		for (Iterator<Qryop> i = this.args.iterator(); i.hasNext(); )
			result += (i.next().toString() + " ");

		return ("#SCORE( " + result + ")");
	}
}
