import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Vector;

/**
 * Created by Wei on 1/27/15.
 */
public class QryopIlNear extends QryopIl {


	private int dist = 0;

	public QryopIlNear(int dist) {
		super();

		this.dist = dist;
	}

	@Override
	public void add(Qryop q) throws IOException {
		this.args.add(q);
	}

	public QryResult evaluate(RetrievalModel r) throws IOException {
		//  Initialization
		System.out.println("Evaluate NEAR.");
		allocArgPtrs(r);

		//syntaxCheckArgResults(this.argPtrs);

		QryResult result = new QryResult();
		result.invertedList.field = new String(this.argPtrs.get(0).invList.field);

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

			List<Integer> res = getPosition(positions);
			//  The ptr0Docid matched all query arguments, so save it.

			if (res.size() > 0) {
				result.invertedList.appendPosting(ptr0Docid, res);
			}
		}

		freeArgPtrs();

		return result;
	}

	/**
	 * Return the smallest unexamined docid from the ArgPtrs.
	 *
	 * @return The smallest internal document id.
	 */
	public int getNextCommonDocid() {

		int nextDocid = Integer.MAX_VALUE;

		for (int i = 0; i < this.argPtrs.size(); i++) {
			ArgPtr ptri = this.argPtrs.get(i);

			if (nextDocid > ptri.invList.getDocid(ptri.nextDoc)) {
				nextDocid = ptri.invList.getDocid(ptri.nextDoc);
			}
		}

		return (nextDocid);
	}

	/**
	 * syntaxCheckArgResults does syntax checking that can only be done
	 * after query arguments are evaluated.
	 *
	 * @param ptrs A list of ArgPtrs for this query operator.
	 * @return True if the syntax is valid, false otherwise.
	 */
	public Boolean syntaxCheckArgResults(List<ArgPtr> ptrs) {

		for (int i = 0; i < this.args.size(); i++) {

			if (!(this.args.get(i) instanceof QryopIl))
				QryEval.fatalError("Error:  Invalid argument in " +
						this.toString());
			else if ((i > 0) &&
					(!ptrs.get(i).invList.field.equals(ptrs.get(0).invList.field)))
				QryEval.fatalError("Error:  Arguments must be in the same field:  " +
						this.toString());
		}

		return true;
	}

	@Override
	public String toString() {
		return null;
	}

	/**
	 *
	 */
	private List<Integer> getPosition(Vector<Vector<Integer>> positions) {
		List<Integer> res = new ArrayList<Integer>();

		int[] index = new int[positions.size()];

		for (int i = 0; i < positions.size(); i++) {
			Collections.sort(positions.get(i));
			index[i] = 0;
		}

		FLAG:
		while (index[index.length - 1] < positions.get(index.length - 1).size()) {
			boolean isValid = true;

			for (int i = 0; i < index.length; i++) {
				if (index[i] >= positions.get(i).size()) {
					break FLAG;
				}
			}

			for (int i = positions.size() - 1; i >= 0; i--) {
				if (i > 0) {

					int val = positions.get(i).get(index[i]);
					int valPre = positions.get(i - 1).get(index[i - 1]);

					if (val - valPre > dist) {
						index[i - 1]++;

						isValid = false;
						continue FLAG;
					} else if (val < valPre){
						index[i]++;

						isValid = false;
						continue FLAG;
					}
				}
			}

			if (isValid) {
				List<Integer> l = new ArrayList<Integer>();
				for (int ii = 0; ii < index.length; ii++) {
					index[ii]++;
				}

				res.add(index[index.length - 1]);
			}
		}

		return res;
	}
}
