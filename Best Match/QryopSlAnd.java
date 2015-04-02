/**
 *  This class implements the AND operator for all retrieval models.
 *
 *  Copyright (c) 2015, Carnegie Mellon University.  All Rights Reserved.
 */

import java.io.IOException;

public class QryopSlAnd extends QryopSl {

	/**
	 * It is convenient for the constructor to accept a variable number
	 * of arguments. Thus new qryopAnd (arg1, arg2, arg3, ...).
	 *
	 * @param q A query argument (a query operator).
	 */
	public QryopSlAnd(Qryop... q) {
		for (int i = 0; i < q.length; i++)
			this.args.add(q[i]);
	}

	/**
	 * Appends an argument to the list of query operator arguments.  This
	 * simplifies the design of some query parsing architectures.
	 *
	 * @param {q} q The query argument (query operator) to append.
	 * @return void
	 * @throws java.io.IOException
	 */
	public void add(Qryop q) {
		this.args.add(q);
	}

	/**
	 * Evaluates the query operator, including any child operators and
	 * returns the result.
	 *
	 * @param r A retrieval model that controls how the operator behaves.
	 * @return The result of evaluating the query.
	 * @throws java.io.IOException
	 */
	public QryResult evaluate(RetrievalModel r) throws IOException {

		if (r instanceof RetrievalModelUnrankedBoolean) {
			return (evaluateBoolean(r));
		} else if (r instanceof RetrievalModelRankedBoolean) {
			return (evaluateRankedBoolean(r));
		} else if (r instanceof RetrievalModelIndri) {
			return evaluateIndri(r);
		}

		return null;
	}

	public QryResult evaluateRankedBoolean(RetrievalModel r) throws IOException {

		//  Initialization

		allocArgPtrs(r);
		QryResult result = new QryResult();
		//  Exact-match AND requires that ALL scoreLists contain a
		//  document id.  Use the first (shortest) list to control the
		//  search for matches.

		ArgPtr ptr0 = this.argPtrs.get(0);

		EVALUATEDOCUMENTS:
		for (; ptr0.nextDoc < ptr0.scoreList.scores.size(); ptr0.nextDoc++) {

			int ptr0Docid = ptr0.scoreList.getDocid(ptr0.nextDoc);

			double docScore = ptr0.scoreList.getDocidScore(ptr0.nextDoc);

			//  Do the other query arguments have the ptr0Docid?
			for (int j = 1; j < this.argPtrs.size(); j++) {

				ArgPtr ptrj = this.argPtrs.get(j);

				while (true) {
					if (ptrj.nextDoc >= ptrj.scoreList.scores.size())
						break EVALUATEDOCUMENTS;        // No more docs can match
					else if (ptrj.scoreList.getDocid(ptrj.nextDoc) > ptr0Docid)
						continue EVALUATEDOCUMENTS;    // The ptr0docid can't match.
					else if (ptrj.scoreList.getDocid(ptrj.nextDoc) < ptr0Docid)
						ptrj.nextDoc++;            // Not yet at the right doc.
					else {
						double score = ptrj.scoreList.getDocidScore(ptrj.nextDoc);
						if (score < docScore) {
							docScore = score;
						}
						break;                // ptrj matches ptr0Docid
					}
				}
			}

			//  The ptr0Docid matched all query arguments, so save it.

			result.docScores.add(ptr0Docid, docScore);
		}

		freeArgPtrs();

		return result;
	}


	/**
	 * Evaluates the query operator for boolean retrieval models,
	 * including any child operators and returns the result.
	 *
	 * @param r A retrieval model that controls how the operator behaves.
	 * @return The result of evaluating the query.
	 * @throws java.io.IOException
	 */
	public QryResult evaluateBoolean(RetrievalModel r) throws IOException {

		//  Initialization

		allocArgPtrs(r);
		QryResult result = new QryResult();
		//  Exact-match AND requires that ALL scoreLists contain a
		//  document id.  Use the first (shortest) list to control the
		//  search for matches.

		ArgPtr ptr0 = this.argPtrs.get(0);

		EVALUATEDOCUMENTS:
		for (; ptr0.nextDoc < ptr0.scoreList.scores.size(); ptr0.nextDoc++) {

			int ptr0Docid = ptr0.scoreList.getDocid(ptr0.nextDoc);

			double docScore = 1.0;

			//  Do the other query arguments have the ptr0Docid?
			for (int j = 1; j < this.argPtrs.size(); j++) {

				ArgPtr ptrj = this.argPtrs.get(j);

				while (true) {
					if (ptrj.nextDoc >= ptrj.scoreList.scores.size())
						break EVALUATEDOCUMENTS;        // No more docs can match
					else if (ptrj.scoreList.getDocid(ptrj.nextDoc) > ptr0Docid)
						continue EVALUATEDOCUMENTS;    // The ptr0docid can't match.
					else if (ptrj.scoreList.getDocid(ptrj.nextDoc) < ptr0Docid)
						ptrj.nextDoc++;            // Not yet at the right doc.
					else
						break;                // ptrj matches ptr0Docid
				}
			}

			//  The ptr0Docid matched all query arguments, so save it.

			result.docScores.add(ptr0Docid, docScore);
		}

		freeArgPtrs();

		return result;
	}

	public QryResult evaluateIndri(RetrievalModel r) throws IOException {
		System.out.println("Evaluate AND indri.");

		allocArgPtrs(r);
		QryResult result = new QryResult();
		if (this.argPtrs.size() == 1) {
			result.docScores = this.argPtrs.get(0).scoreList;

			return result;
		}

		for (int i = 0; i < this.argPtrs.size(); i++) {
			if (this.argPtrs.get(i).scoreList.scores.size() == 0) {
				this.argPtrs.remove(i);
				i--;
			}
		}

		double size = this.argPtrs.size();

		int argSize = this.argPtrs.size();
		while (argSize > 0) {
			int nextDocid = getSmallestCurrentDocid();

			if (nextDocid == Integer.MAX_VALUE) {
				break;
			}

			double docScore = 1.0;

			//  If an ArgPtr has reached the end of its list, remove it.
			//  The loop is backwards so that removing an arg does not
			//  interfere with iteration.
			for (int i = 0; i < this.argPtrs.size(); i++) {
				ArgPtr ptr = this.argPtrs.get(i);

				double score = 1.0;
				if (ptr.scoreList.scores.size() > ptr.nextDoc
						&& ptr.scoreList.getDocid(ptr.nextDoc) == nextDocid) {
					score = ptr.scoreList.getDocidScore(ptr.nextDoc);
					ptr.nextDoc++;
				} else {
					score = ((QryopSl) this.args.get(i)).getDefaultScore(r, nextDocid);
				}

				docScore *= Math.pow(score, 1.0 / size);
			}

			argSize = this.argPtrs.size();
			for (int i = 0; i < this.argPtrs.size(); i++) {
				ArgPtr ptri = this.argPtrs.get(i);

				if (ptri.nextDoc >= ptri.scoreList.scores.size()) {
					argSize--;
				}
			}

			result.docScores.add(nextDocid, docScore);
		}

		freeArgPtrs();

		return result;
	}

	/*
	   *  Calculate the default score for the specified document if it
	   *  does not match the query operator.  This score is 0 for many
	   *  retrieval models, but not all retrieval models.
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

		double defaultScore = 1.0;
		for (int i = 0; i < this.args.size(); i++) {
			defaultScore *= Math.pow(((QryopSl) this.args.get(i)).getDefaultScore(r, docid),
					1.0 / this.args.size());
		}

		return defaultScore;
	}

	public int getSmallestCurrentDocid() {

		int nextDocid = Integer.MAX_VALUE;

		for (int i = 0; i < this.argPtrs.size(); i++) {
			ArgPtr ptri = this.argPtrs.get(i);
			if (ptri.scoreList.scores.size() == 0) {
				continue;
			}

			if (ptri.scoreList.scores.size() > ptri.nextDoc &&
					nextDocid > ptri.scoreList.getDocid(ptri.nextDoc))
				nextDocid = ptri.scoreList.getDocid(ptri.nextDoc);
		}

		return (nextDocid);
	}

	/*
	   *  Return a string version of this query operator.
	   *  @return The string version of this query operator.
	   */
	public String toString() {

		String result = new String();

		for (int i = 0; i < this.args.size(); i++)
			result += this.args.get(i).toString() + " ";

		return ("#AND( " + result + ")");
	}
}
