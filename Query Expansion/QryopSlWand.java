import java.io.IOException;

/**
 * Created by Tong on 3/11/15.
 */
public class QryopSlWand extends QryopSl {
    public QryopSlWand() {
        this.hasWeight = true;
    }

    @Override
    public double getDefaultScore(RetrievalModel r, long docid) throws IOException {
        double sumWeight = 0.0;
        for (int i = 0; i < this.args.size(); i++) {
            sumWeight += this.args.get(i).weight;
        }

        double defaultScore = 1.0;
        for (int i = 0; i < this.args.size(); i++) {
            defaultScore *= Math.pow(((QryopSl) this.args.get(i)).getDefaultScore(r, docid),
                    this.args.get(i).weight / sumWeight);
        }

        return defaultScore;
    }

    @Override
    public void add(Qryop q) throws IOException {
        this.args.add(q);
    }

    public QryResult evaluate(RetrievalModel r) throws IOException {
        System.out.println("Evaluate WAND indri.");

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

                // System.out.println("weight: " + this.argPtrs.get(i).weight + "   " + sumWeight);
                docScore *= Math.pow(score, this.argPtrs.get(i).weight / sumWeight);
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

    @Override
    public String toString() {

        String result = new String();

        for (int i = 0; i < this.args.size(); i++)
            result += this.args.get(i).toString() + " ";

        return ("#WAND( " + result + ")");
    }

    private class TermDocEntry implements Comparable<TermDocEntry> {
        private int termId;
        private int docId;

        public TermDocEntry() {
        }

        public TermDocEntry(int termId, int docId) {
            this.termId = termId;
            this.docId = docId;
        }

        public int getTermId() {
            return termId;
        }

        public void setTermId(int termId) {
            this.termId = termId;
        }

        public int getDocId() {
            return docId;
        }

        public void setDocId(int docId) {
            this.docId = docId;
        }

        @Override
        public int compareTo(TermDocEntry that) {
            return this.docId - that.docId;
        }
    }
}
