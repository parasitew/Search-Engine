import java.io.IOException;

/**
 * Created by Wei on 1/27/15.
 */
public class QryopSlOr extends QryopSl {
	public QryopSlOr(Qryop... q) {
		for (int i = 0; i < q.length; i++) {
			this.args.add(q[i]);
		}
	}

	@Override
	public void add(Qryop q) throws IOException {
		this.args.add(q);
	}

	public QryResult evaluate(RetrievalModel r) throws IOException {
		System.out.println("Evaluate OR.");

		if (r instanceof RetrievalModelUnrankedBoolean) {
			return (evaluateBoolean(r));
		} else if (r instanceof RetrievalModelRankedBoolean) {
			return (evaluateRankedBoolean(r));
		}

		return null;
	}

	public QryResult evaluateBoolean(RetrievalModel r) throws IOException {
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

		while (this.argPtrs.size() > 0) {
			int nextDocid = getSmallestCurrentDocid();
			double docScore = 1.0;

			//  If an ArgPtr has reached the end of its list, remove it.
			//  The loop is backwards so that removing an arg does not
			//  interfere with iteration.
			for (int i = 0; i < this.argPtrs.size(); i++) {
				ArgPtr ptr = this.argPtrs.get(i);

				if (ptr.scoreList.getDocid(ptr.nextDoc) == nextDocid) {
					ptr.nextDoc++;
				}
			}


			for (int i = this.argPtrs.size() - 1; i >= 0; i--) {
				ArgPtr ptri = this.argPtrs.get(i);

				if (ptri.nextDoc >= ptri.scoreList.scores.size()) {
					this.argPtrs.remove(i);
				}
			}

			result.docScores.add(nextDocid, docScore);

		}

		freeArgPtrs();

		return result;
	}

	public int getSmallestCurrentDocid() {

		int nextDocid = Integer.MAX_VALUE;

		for (int i = 0; i < this.argPtrs.size(); i++) {
			ArgPtr ptri = this.argPtrs.get(i);
			if (ptri.scoreList.scores.size() == 0) {
				continue;
			}

			if (nextDocid > ptri.scoreList.getDocid(ptri.nextDoc))
				nextDocid = ptri.scoreList.getDocid(ptri.nextDoc);
		}

		return (nextDocid);
	}

	public QryResult evaluateRankedBoolean(RetrievalModel r) throws IOException {
		allocArgPtrs(r);
		QryResult result = new QryResult();
		double max = 0;
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

		while (this.argPtrs.size() > 0) {
			int nextDocid = getSmallestCurrentDocid();
			double docScore = 0;
			//  If an ArgPtr has reached the end of its list, remove it.
			//  The loop is backwards so that removing an arg does not
			//  interfere with iteration.
			for (int i = 0; i < this.argPtrs.size(); i++) {
				ArgPtr ptr = this.argPtrs.get(i);

				if (ptr.scoreList.getDocid(ptr.nextDoc) == nextDocid) {
					double score = ptr.scoreList.getDocidScore(ptr.nextDoc);
					if (score > docScore) {
						docScore = score;
					}
					ptr.nextDoc++;
				}
			}


			for (int i = this.argPtrs.size() - 1; i >= 0; i--) {
				ArgPtr ptri = this.argPtrs.get(i);

				if (ptri.nextDoc >= ptri.scoreList.scores.size()) {
					this.argPtrs.remove(i);
				}
			}

			result.docScores.add(nextDocid, docScore);

		}
		freeArgPtrs();

		return result;

	}

	public double getDefaultScore(RetrievalModel r, long docid) throws IOException {

		if (r instanceof RetrievalModelUnrankedBoolean)
			return (0.0);

		return 0.0;
	}

	public String toString() {

		String result = new String();

		for (int i = 0; i < this.args.size(); i++)
			result += this.args.get(i).toString() + " ";

		return ("#OR( " + result + ")");
	}
}