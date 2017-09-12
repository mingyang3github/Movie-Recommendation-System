import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.Mapper.Context;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;

public class Recommendation {
	public static class RecommendationMapper extends Mapper<LongWritable, Text, IntWritable, Text> {

		@Override
		public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
			String[] line = value.toString().trim().split("\t");
			int user = Integer.parseInt(line[0].split(":")[0]);
			int movie = Integer.parseInt(line[0].split(":")[1]);
			double rating = Double.parseDouble(line[1]);
			context.write(new IntWritable(user), new Text(movie + ":" + rating));
		}
	}

	public static class RecommendationReducer extends Reducer<IntWritable, Text, IntWritable, Text> {

		// reduce method
		@Override
		public void reduce(IntWritable key, Iterable<Text> values, Context context)
				throws IOException, InterruptedException {

			// Top K: recommend top k movies with highest calculated ratings for each user
			int K = 3;
			PriorityQueue<Movie> heap = new PriorityQueue<Movie>();

			//movie_id:rating
			while(values.iterator().hasNext()) {
				String[] tokens = values.iterator().next().toString().trim().split(":");
				int movie_id = Integer.parseInt(tokens[0]);
				double rating = Double.parseDouble(tokens[1]);
				if(heap.size() < K) {
					heap.offer(new Movie(movie_id, rating));
				} else {
					if(heap.peek().getRating() < rating) {
						heap.poll();
						heap.offer(new Movie(movie_id, rating));
					}
				}
			}

			while(!heap.isEmpty()) {
				Movie movie = heap.poll();
				context.write(key, new Text(movie.getMovieId() + ":" + movie.getRating()));
			}
		}
	}

	public static void main(String[] args) throws Exception {

		Configuration conf = new Configuration();

		Job job = Job.getInstance(conf);
		job.setMapperClass(RecommendationMapper.class);
		job.setReducerClass(RecommendationReducer.class);

		job.setJarByClass(Recommendation.class);

		job.setInputFormatClass(TextInputFormat.class);
		job.setOutputFormatClass(TextOutputFormat.class);
		job.setOutputKeyClass(IntWritable.class);
		job.setOutputValueClass(Text.class);

		TextInputFormat.setInputPaths(job, new Path(args[0]));
		TextOutputFormat.setOutputPath(job, new Path(args[1]));

		job.waitForCompletion(true);
	}
}
