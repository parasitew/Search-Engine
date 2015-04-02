import java.io.IOException;

/**
 * Created by Wei on 2/9/15.
 */
public class QryopSlSum extends QryopSl {
    @Override
    public double getDefaultScore(RetrievalModel r, long docid) throws IOException {
        return 0;
    }

    @Override
    public void add(Qryop q) throws IOException {
        this.args.add(q);
    }

    public QryResult evaluate(RetrievalModel r) throws IOException {
        System.out.println("Evaluate SUM");

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
            double docScore = 0.0;
            //  If an ArgPtr has reached the end of its list, remove it.
            //  The loop is backwards so that removing an arg does not
            //  interfere with iteration.
            for (int i = 0; i < this.argPtrs.size(); i++) {
                ArgPtr ptr = this.argPtrs.get(i);

                if (ptr.scoreList.getDocid(ptr.nextDoc) == nextDocid) {
                    double score = ptr.scoreList.getDocidScore(ptr.nextDoc);
                    docScore += score;
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

    @Override
    public String toString() {
        return null;
    }

}
