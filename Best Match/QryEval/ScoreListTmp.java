/**
 *  This class implements the document score list data structure
 *  and provides methods for accessing and manipulating them.
 *
 *  Copyright (c) 2015, Carnegie Mellon University.  All Rights Reserved.
 */

import org.apache.lucene.document.Document;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

public class ScoreListTmp {

	//  A little utilty class to create a <docid, score> object.

	List<ScoreListEntry> scores = new ArrayList<ScoreListEntry>();

	static String getExternalDocid(int iid) throws IOException {
		Document d = QryEval.READER.document(iid);
		String eid = d.get("externalId");
		return eid;
	}

	/**
	 * Append a document score to a score list.
	 *
	 * @param docid An internal document id.
	 * @param score The document's score.
	 * @return void
	 */
	public void add(int docid, double score) {
		scores.add(new ScoreListEntry(docid, score));
	}

	/**
	 * Get the n'th document id.
	 *
	 * @param n The index of the requested document.
	 * @return The internal document id.
	 */
	public int getDocid(int n) {
		return this.scores.get(n).docid;
	}

	/**
	 * Get the score of the n'th document.
	 *
	 * @param n The index of the requested document score.
	 * @return The document's score.
	 */
	public double getDocidScore(int n) {
		return this.scores.get(n).score;
	}

	/**
	 * Sort with max heap.
	 */
	public void sortScoreList() {
		int size = Math.min(100, this.scores.size());
		PriorityQueue<ScoreListEntry> queue = new PriorityQueue<ScoreListEntry>(size, new ScoreListComparator());

		for (ScoreListEntry entry : this.scores) {
			queue.offer(entry);
		}

		List<ScoreListEntry> res = new ArrayList<ScoreListEntry>();

		while (size-- > 0) {
			res.add(queue.poll());
		}

		this.scores = res;
	}

	public class ScoreListComparator implements Comparator<ScoreListEntry> {

		@Override
		public int compare(ScoreListEntry o1, ScoreListEntry o2) {
			String externalDocid1 = "";
			String externalDocid2 = "";
			int res = Double.compare(o1.score, o2.score);

			if (res != 0) {
				return -res;
			}

			try {
				externalDocid1 = getExternalDocid(o1.docid);
				externalDocid2 = getExternalDocid(o2.docid);
			} catch (IOException e) {
				e.printStackTrace();
			}

			return externalDocid1.compareTo(externalDocid2);
		}
	}

	protected class ScoreListEntry {
		private int docid;
		private double score;

		private ScoreListEntry(int docid, double score) {
			this.docid = docid;
			this.score = score;
		}
	}
}
