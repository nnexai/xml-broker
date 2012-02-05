package xml.eventbroker.test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class Statistics {
	
	public static class DataPoint {
		public final long v;
		public final int pos;
		
		public DataPoint(long value, int position) {
			this.v = value;
			this.pos = position;
		}
	}
	
	public List<DataPoint> l;
	public long max;
	public long max_offset;

	public Statistics(Collection<DataPoint> c) {
		l = new ArrayList<DataPoint>(c.size());
		int index = 0;
		for (DataPoint p : c) {
			l.add(p);
			max = Math.max(max, p.v);
			max_offset = Math.max(max_offset, Math.abs((index++)-p.pos));
		}

		// auf 100 ms runden
		int roundTo = String.valueOf(Math.abs((long)(max+0.5))).length()-2;
		double roundFac = Math.pow(10, roundTo);
		max = (long) (Math.ceil(max / roundFac) * roundFac);
		
		// auf schöne zahlen runden
		roundTo = String.valueOf(max_offset).length()-1;
		roundFac = Math.pow(10, roundTo);
		max_offset = (long) (Math.ceil(max_offset / roundFac) * roundFac);
	}

	List<JoinedDataPoint> reduced;
	int count = -1;

	public static class JoinedDataPoint {
		public final int count;
		public final long min;
		public final long max;
		public final double avg;
		public final double median;
		public final double avg_offset;
		public final long min_offset;
		public final long max_offset;
		
		
		public JoinedDataPoint(double avg, long min, long max, double median, int count, double avg_offset, long min_offset, long max_offset) {
			super();
			this.avg = avg;
			this.min = min;
			this.max = max;
			this.count = count;
			this.median = median;
			this.avg_offset = avg_offset;
			this.min_offset = min_offset;
			this.max_offset = max_offset;
		}
		
		@Override
		public String toString() {
			return avg+" [v"+min+",^"+max+",#"+count+"]";
		}
	}
	
	public synchronized List<JoinedDataPoint> getReduced(int count) {
		if (this.count != count) {
			if (l.size() > count) {
				reduced = new ArrayList<JoinedDataPoint>(count);

				double cntPerReduced = l.size() / (double) count;
				double currentD = 0;
				int currentI = 0;

				while (currentI < l.size()) {
					double avg = 0;
					int cntPerPoint = 0;
					long min=Long.MAX_VALUE,max=Long.MIN_VALUE;
					double avg_offset = 0;
					long min_offset=Long.MAX_VALUE,max_offset=Long.MIN_VALUE;
					
					currentD += cntPerReduced;
					
					long[] points = new long[(int)(cntPerReduced+1.5)];
					while (currentI < l.size() && currentI < currentD) {
						DataPoint p = l.get(currentI);
						points[cntPerPoint++] = p.v; 
						avg += p.v;
						min = Math.min(min, p.v);max = Math.max(max, p.v);

						int offset = currentI-p.pos;
						avg_offset += offset;
						min_offset = Math.min(min_offset, offset);max_offset = Math.max(max_offset, offset);
						currentI++;
					}
					avg/=(double)cntPerPoint;
					avg_offset/=(double)cntPerPoint;

					Arrays.sort(points, 0, cntPerPoint);
					double median = (cntPerPoint%2==0)?(0.5*(points[cntPerPoint/2-1]+points[cntPerPoint/2])):(points[cntPerPoint/2]);
					reduced.add(new JoinedDataPoint(avg, min, max, median, cntPerPoint, avg_offset, min_offset, max_offset));
				}

			} else {
				if (reduced == null || reduced.size() != l.size()) {
					reduced = new ArrayList<JoinedDataPoint>(l.size());
					int currentPos = 0;
					for (DataPoint p : l) {
						int offset = (currentPos++)-p.pos;
						reduced.add(new JoinedDataPoint(p.v, p.v, p.v, p.v, 1, offset, offset, offset));
					}
					this.count = l.size();
				}
			}
			this.count = count;
		}
		return reduced;
	}

}
