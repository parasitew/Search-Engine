import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Vector;

/**
 * Created by Tong on 3/10/15.
 */
public class QryopIlWindow extends QryopIl {
    private int windowSize = 0;

    public QryopIlWindow() {
    }

    public QryopIlWindow(int windowSize) {
        this.windowSize = windowSize;
    }

    @Override
    public void add(Qryop q) throws IOException {
        this.args.add(q);
    }

    @Override
    public QryResult evaluate(RetrievalModel r) throws IOException {
        //  Initialization
        System.out.println("Evaluate WINDOW.");
        allocArgPtrs(r);

        //syntaxCheckArgResults(this.argPtrs);

        QryResult result = new QryResult();
        result.invertedList.field = this.argPtrs.get(0).invList.field;

        ArgPtr ptr0 = this.argPtrs.get(0);

        // Pick a doc in the first list.
        EVALUATEDOCUMENTS:
        for (; ptr0.nextDoc < ptr0.invList.postings.size(); ptr0.nextDoc++) {
            Vector<Vector<Integer>> positions = new Vector<Vector<Integer>>();
            int ptr0Docid = ptr0.invList.getDocid(ptr0.nextDoc);

            positions.add(ptr0.invList.postings.get(ptr0.nextDoc).positions);
            //  Do the other query arguments have the ptr0Docid?

            for (int j = 1; j < this.argPtrs.size(); j++) {
                ArgPtr ptrj = this.argPtrs.get(j);

                while (true) {
                    if (ptrj.nextDoc >= ptrj.invList.postings.size()) {// No more docs can match
                        break EVALUATEDOCUMENTS;
                    } else if (ptrj.invList.getDocid(ptrj.nextDoc) > ptr0Docid) {// The ptr0docid can't match.
                        continue EVALUATEDOCUMENTS;
                    } else if (ptrj.invList.getDocid(ptrj.nextDoc) < ptr0Docid) {// Not yet at the right doc.
                        ptrj.nextDoc++;
                    } else {// ptrj matches ptr0Docid
                        positions.add(ptrj.invList.postings.get(ptrj.nextDoc).positions);

                        break;
                    }
                }
            }

            //  The ptr0Docid matched all query arguments, so save it.

            List<Integer> res = getPosition(positions, windowSize);

            if (res.size() > 0) {
                result.invertedList.appendPosting(ptr0Docid, res);
            }
        }

        freeArgPtrs();
        return result;
    }

    // Find the max index given positions of the terms in a doc.
    private List<Integer> getPosition(Vector<Vector<Integer>> positions, int windowSize) {
        List<Integer> res = new ArrayList<Integer>();
        int[] index = new int[positions.size()];

        for (int i = 0; i < positions.size(); i++) {
            Collections.sort(positions.get(i));
            index[i] = 0;
        }

        FLAG:
        while (true) {
            int min = Integer.MAX_VALUE;
            int max = Integer.MIN_VALUE;

            int minIndex = 0;
            int maxIndex = 0;

            for (int i = 0; i < positions.size(); i++) {
                int val = positions.get(i).get(index[i]);

                if (val < min) {
                    min = val;
                    minIndex = i;
                }

                if (val > max) {
                    max = val;
                    maxIndex = i;
                }
            }

            int size = 1 + max - min;

            if (size > windowSize) {
                index[minIndex]++;

                if (index[minIndex] > positions.get(minIndex).size() - 1) {
                    break FLAG;
                }
            } else if (size <= windowSize) {
                res.add(max);

                for (int i = 0; i < index.length; i++) {
                    index[i]++;

                    if (index[i] > positions.get(i).size() - 1) {
                        break FLAG;
                    }
                }
            }
        }

        return res;
    }

    @Override
    public String toString() {
        return null;
    }
}
