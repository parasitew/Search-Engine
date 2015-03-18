import java.io.IOException;

/**
 * Created by Tong on 3/14/15.
 */
public class QryopSlWsum extends QryopSl {
    public QryopSlWsum() {
        this.hasWeight = true;
    }

    @Override
    public void add(Qryop q) throws IOException {
        this.args.add(q);
    }

    public QryResult evaluate(RetrievalModel r) throws IOException {
        System.out.println("Evaluate WSum indri, size " + this.args.size());

        allocArgPtrs(r);

        double sumWeight = 0.0;
        for (int i = 0; i < this.argPtrs.size(); i++) {
            sumWeight += this.argPtrs.get(i).weight;
        }

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

            double docScore =0.0;

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

                docScore += (score * this.argPtrs.get(i).weight / sumWeight);
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

        if (r instanceof RetrievalModelIndri) {
            return getDefaultScoreIndri((RetrievalModelIndri) r, docid);
        }

        return 0.0;
    }

    private double getDefaultScoreIndri(RetrievalModelIndri r, long docid) throws IOException {

        double sumWeight = 0.0;
        for (int i = 0; i < this.args.size(); i++) {
            sumWeight += this.args.get(i).weight;
        }

        double defaultScore = 0.0;
        for (int i = 0; i < this.args.size(); i++) {
            defaultScore += (((QryopSl) this.args.get(i)).getDefaultScore(r, docid) *
                    this.args.get(i).weight / sumWeight);
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

            if (ptri.nextDoc < ptri.scoreList.scores.size() &&
                    nextDocid > ptri.scoreList.getDocid(ptri.nextDoc))
                nextDocid = ptri.scoreList.getDocid(ptri.nextDoc);
        }

        return (nextDocid);
    }

    @Override
    public String toString() {
        String result = new String();

        for (int i = 0; i < this.args.size(); i++)
            result += this.args.get(i).toString() + " ";

        return ("#WSUM( " + result + ")");
    }
}
