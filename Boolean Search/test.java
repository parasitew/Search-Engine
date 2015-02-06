import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;


	public QryResult evaluate(RetrievalModel r) throws IOException {
		allocArgPtrs(r);
		syntaxCheckArgResults(this.argPtrs);
	
		QryResult result = new QryResult();
		result.invertedList.field = this.argPtrs.get(0).invList.field;
		ArgPtr ptr0 = this.argPtrs.get(0);
	
		ITERATE_DOCS:
		for (; ptr0.nextDoc < ptr0.invList.postings.size(); ptr0.nextDoc++) {
			int ptr0Docid = ptr0.invList.getDocid(ptr0.nextDoc);
	
			for (int j = 1; j < this.argPtrs.size(); ++j) {
				ArgPtr ptrj = this.argPtrs.get(j);
	
				while (true) {
					if (ptrj.nextDoc >= ptrj.invList.postings.size()) {
						break ITERATE_DOCS;     // No more docs can match
					} else if (ptrj.invList.getDocid(ptrj.nextDoc) > ptr0Docid) {
						continue ITERATE_DOCS;  // This ptr0docid can't match.
					} else if (ptrj.invList.getDocid(ptrj.nextDoc) < ptr0Docid) {
						ptrj.nextDoc++;         // Not yet at the right doc.
					} else {
						break;                  // ptrj matches ptr0Docid
					}
				}
			}
	
			List<Integer> positions = new ArrayList<Integer>();
			// record current position when iterating the same doc for other daat pointers
			int[] daatPtrPos = new int[this.argPtrs.size()];
			int prevPos;
			ITERATE_POSTING:
			for (int ptr0Pos : ptr0.invList.postings.get(ptr0.nextDoc).positions) {
				prevPos = ptr0Pos;
	
				ITERATE_DAAT_PTR:
				for (int j = 1; j < this.argPtrs.size(); ++j) {
					ArgPtr ptrj = this.argPtrs.get(j);
					Vector<Integer> ptrjPostings = ptrj.invList.postings.get(ptrj.nextDoc).positions;
					int ptrjPostingSize = ptrjPostings.size();
	
					for (; daatPtrPos[j] < ptrjPostingSize; ++daatPtrPos[j]) {
						int ptrjPos = ptrjPostings.get(daatPtrPos[j]);
						if (ptrjPos > prevPos) {
							if (ptrjPos - prevPos <= dist) {
								prevPos = ptrjPos;               // find good position in this
								continue ITERATE_DAAT_PTR;       // doc, process next daatPtr.
							} else {                           // otherwise check next ptr0Pos,
								continue ITERATE_POSTING;        // since this one is impossible
							}
						} else // try ptrjPos until greater than ptr0Pos
						{
							continue;
						}
					}
					// if all positions of ptrj are smaller than ptr0Pos,
					// this doc cannot satisfy NEAR's requirements,
					// therefore stop iterating posting and record current result
					break ITERATE_POSTING;
				}
				// all docIds have positions matching the requirement, record the pos
				positions.add(ptr0Pos);
				// advance position pointers in other daat ptr
				for (int j = 1; j < daatPtrPos.length; ++j) {
					++daatPtrPos[j];
				}
			}
			if (!positions.isEmpty()) {
				result.invertedList.appendPosting(ptr0Docid, positions);
			}
		}
		freeArgPtrs();
		return result;
	}

public class QryopIlNear extends QryopIl {

  /**
   * the only parameter for near operator, indicating the distance
   * allowed between each word
   */
  private final int distance;

  public QryopIlNear(int distance) {
    this.distance = distance;
  }

  /**
   * Appends an argument to the list of query operator arguments.
   *
   * @param q The query argument (query operator) to append.
   * @return void
   */
  @Override
  public void add(Qryop q) {
    this.args.add(q);
  }

  /**
   * Evaluates the query operator, including any child operators and
   * returns the result.
   *
   * @param r A retrieval model that controls how the operator behaves.
   * @return The result of evaluating the query.
   * @throws IOException
   */
  @Override
  public QryResult evaluate(RetrievalModel r) throws IOException {
    allocDaaTPtrs(r);
    syntaxCheckArgResults(this.daatPtrs);

    QryResult result = new QryResult();
    result.invertedList.field = this.daatPtrs.get(0).invList.field;
    DaaTPtr ptr0 = this.daatPtrs.get(0);

    ITERATE_DOCS:
    for (; ptr0.nextDoc < ptr0.invList.postings.size(); ptr0.nextDoc++) {
	int ptr0Docid = ptr0.invList.getDocid(ptr0.nextDoc);

	for (int j = 1; j < this.daatPtrs.size(); ++j) {
	  DaaTPtr ptrj = this.daatPtrs.get(j);

	  while (true) {
	    if (ptrj.nextDoc >= ptrj.invList.postings.size()) {
		break ITERATE_DOCS;     // No more docs can match
	    } else if (ptrj.invList.getDocid(ptrj.nextDoc) > ptr0Docid) {
		continue ITERATE_DOCS;  // This ptr0docid can't match.
	    } else if (ptrj.invList.getDocid(ptrj.nextDoc) < ptr0Docid) {
		ptrj.nextDoc++;         // Not yet at the right doc.
	    } else {
		break;                  // ptrj matches ptr0Docid
	    }
	  }
	}

	List<Integer> positions = new ArrayList<Integer>();
	// record current position when iterating the same doc for other daat pointers
	int[] daatPtrPos = new int[this.daatPtrs.size()];
	int prevPos;
	ITERATE_POSTING:
	for (int ptr0Pos : ptr0.invList.postings.get(ptr0.nextDoc).positions) {
	  prevPos = ptr0Pos;

	  ITERATE_DAAT_PTR:
	  for (int j = 1; j < this.daatPtrs.size(); ++j) {
	    DaaTPtr ptrj = this.daatPtrs.get(j);
	    Vector<Integer> ptrjPostings = ptrj.invList.postings.get(ptrj.nextDoc).positions;
	    int ptrjPostingSize = ptrjPostings.size();

	    for (; daatPtrPos[j] < ptrjPostingSize; ++daatPtrPos[j]) {
		int ptrjPos = ptrjPostings.get(daatPtrPos[j]);
		if (ptrjPos > prevPos) {
		  if (ptrjPos - prevPos <= distance) {
		    prevPos = ptrjPos;               // find good position in this
		    continue ITERATE_DAAT_PTR;       // doc, process next daatPtr.
		  } else {                           // otherwise check next ptr0Pos,
		    continue ITERATE_POSTING;        // since this one is impossible
		  }
		} else // try ptrjPos until greater than ptr0Pos
		{
		  continue;
		}
	    }
	    // if all positions of ptrj are smaller than ptr0Pos,
	    // this doc cannot satisfy NEAR's requirements,
	    // therefore stop iterating posting and record current result
	    break ITERATE_POSTING;
	  }
	  // all docIds have positions matching the requirement, record the pos
	  positions.add(ptr0Pos);
	  // advance position pointers in other daat ptr
	  for (int j = 1; j < daatPtrPos.length; ++j) {
	    ++daatPtrPos[j];
	  }
	}
	if (!positions.isEmpty()) {
	  result.invertedList.appendPosting(ptr0Docid, positions);
	}
    }
    freeDaaTPtrs();
    return result;
  }

  /**
   * Return a string version of this query operator.
   *
   * @return The string version of this query operator.
   */
  @Override
  public String toString() {
    String result = "";

    for (Qryop arg : this.args) {
	result += arg.toString() + " ";
    }

    return "#NEAR/" + distance + "( " + result + ")";
  }

  /**
   * syntaxCheckArgResults does syntax checking.
   * All daatptr should be QryopIl, and should be in the same field.
   *
   * @param ptrs A list of DaaTPtrs for this query operator.
   * @return True if the syntax is valid, false otherwise.
   */
  private Boolean syntaxCheckArgResults(List<DaaTPtr> ptrs) {

    for (int i = 0; i < this.args.size(); i++) {

	if (!(this.args.get(i) instanceof QryopIl)) {
	  QryEval.fatalError("Error:  Invalid argument in " +
		    this.toString());
	} else if ((i > 0) &&
		  (!ptrs.get(i).invList.field.equals(ptrs.get(0).invList.field))) {
	  QryEval.fatalError("Error:  Arguments must be in the same field:  " +
		    this.toString());
	}
    }

    return true;
  }
}
