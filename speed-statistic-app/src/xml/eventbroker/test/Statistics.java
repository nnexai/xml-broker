package xml.eventbroker.test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class Statistics {
	public List<Long> l;
	public long max;

	public Statistics(Collection<Long> c) {
		l = new ArrayList<Long>(c.size());
		for (long v : c) {
			l.add(v);
			if (max < v) {
				max = v;
			}
		}

		// auf 100 ms runden
		int roundTo = String.valueOf(Math.abs((long)(max+0.5))).length()-2;
		double roundFac = Math.pow(10, roundTo);
		max = (long) (Math.ceil(max / roundFac) * roundFac);
		// max = 1000000000;
	}

	List<JoinedDataPoint> reduced;
	int count = -1;

	public static class JoinedDataPoint {
		public final int count;
		public final long min;
		public final long max;
		public final double avg;
		public final double median;
		
		public JoinedDataPoint(double avg, long min, long max, double median, int count) {
			super();
			this.avg = avg;
			this.min = min;
			this.max = max;
			this.count = count;
			this.median = median;
		}
		
		@Override
		public String toString() {
			return avg+" [v"+min+",^"+max+",#"+count+"]";
		}
	}
	
	public List<JoinedDataPoint> getReduced(int count) {
		if (this.count != count) {
			if (l.size() > count) {
				reduced = new ArrayList<JoinedDataPoint>(count);

				double cntPerReduced = l.size() / (double) count;
				double currentD = 0;
				int currentI = 0;

				while (currentI < l.size()) {
					double current = 0;
					int cntPerPoint = 0;
					long min=Long.MAX_VALUE,max=Long.MIN_VALUE;
					currentD += cntPerReduced;
					
					long[] points = new long[(int)(cntPerReduced+1.5)];
					while (currentI < l.size() && currentI < currentD) {
						long v = l.get(currentI++);
						points[cntPerPoint++] = v; 
						current += v;
						min = Math.min(min, v);
						max = Math.max(max, v);
					}
					Arrays.sort(points, 0, cntPerPoint);
					double median = (cntPerPoint%2==0)?(0.5*(points[cntPerPoint/2-1]+points[cntPerPoint/2])):(points[cntPerPoint/2]);
					reduced.add(new JoinedDataPoint(current / (double)cntPerPoint, min, max, median, cntPerPoint));
				}

			} else {
				if (reduced == null || reduced.size() != l.size()) {
					reduced = new ArrayList<JoinedDataPoint>(l.size());
					for (long v : l)
						reduced.add(new JoinedDataPoint(v, v, v, v, 1));
					this.count = l.size();
				}
			}
			this.count = count;
		}
		return reduced;
	}

}
