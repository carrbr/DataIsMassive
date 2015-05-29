package domain.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import domain.AVGPair;
import domain.Rating;

public class MovieInTime implements DelatAccess, Serializable {
	private static final long serialVersionUID = 1L;

	private final HashMap<Integer, AVGPair> movieBias;
	private final HashMap<Integer, HashMap<Integer, Double>> timeOffset;
	private final HashMap<Integer, Integer> maxDay;
	private final HashMap<Integer, Integer> minDay;
	private final HashMap<Integer, Double> overAllMovieTimeOffset;
	private int oMaxDay = Integer.MIN_VALUE, oMinDay = Integer.MAX_VALUE;
	private final int daysPerBucket = 7 * 8;

	public MovieInTime() {
		movieBias = new HashMap<>();
		timeOffset = new HashMap<>();
		maxDay = new HashMap<>();
		minDay = new HashMap<>();
		overAllMovieTimeOffset = new HashMap<>();
	}

	public void train(List<Rating> toTrain, BaseLearner base) {
		System.out.println("starting training on biasMovie");
		trainAllTimeBias(toTrain, base);
		System.out.println("starting time bias training movie");
		trainTimeBias(toTrain, base);
		// check out examples
		Random r = new Random();

		for (int j = 0; j < 5; j++) {
			int i = r.nextInt(toTrain.size());
			int movieId = toTrain.get(i).getMovieId();
			int maxDayMovie = maxDay.get(movieId);
			int minDayMovie = minDay.get(movieId);
			Double y0 = timeOffset.get(movieId)
					.get(minDayMovie / daysPerBucket);
			Double y1 = timeOffset.get(movieId)
					.get(maxDayMovie / daysPerBucket);
			double m = (maxDayMovie - minDayMovie) == 0 ? 0 : (y1 - y0)
					/ (maxDayMovie - minDayMovie);
			System.out.println("Movie " + movieId + " has m=" + m + " and y0="
					+ y0);
		}
	}

	private void trainTimeBias(List<Rating> toTrain, BaseLearner base) {
		HashMap<Integer, HashMap<Integer, AVGPair>> timeOffsetTemp = new HashMap<>();
		HashMap<Integer, AVGPair> overAllMovieTimeOffsetTemp = new HashMap<>();

		calculateAVGInTime(toTrain, base, timeOffsetTemp,
				overAllMovieTimeOffsetTemp);
		copyToShortClassInternal(timeOffsetTemp, overAllMovieTimeOffsetTemp);
	}

	private void copyToShortClassInternal(
			HashMap<Integer, HashMap<Integer, AVGPair>> timeOffsetTemp,
			HashMap<Integer, AVGPair> overAllMovieTimeOffsetTemp) {

		for (Map.Entry<Integer, HashMap<Integer, AVGPair>> movieBuckets : timeOffsetTemp
				.entrySet()) {

			HashMap<Integer, Double> finalBucket = new HashMap<>();
			timeOffset.put(movieBuckets.getKey(), finalBucket);

			for (Map.Entry<Integer, AVGPair> bucket : movieBuckets.getValue()
					.entrySet()) {
				finalBucket.put(bucket.getKey(), bucket.getValue().getAVG());
			}
		}

		for (Map.Entry<Integer, AVGPair> bucket : overAllMovieTimeOffsetTemp
				.entrySet()) {
			overAllMovieTimeOffset.put(bucket.getKey(), bucket.getValue()
					.getAVG());
		}

	}

	private void calculateAVGInTime(List<Rating> toTrain, BaseLearner base,
			HashMap<Integer, HashMap<Integer, AVGPair>> timeOffsetTemp,
			HashMap<Integer, AVGPair> overAllMovieTimeOffsetTemp) {

		for (Rating r : toTrain) {
			double offset = r.getRating() - base.getDelta(r)
					- movieBias.get(r.getMovieId()).getAVG();

			HashMap<Integer, AVGPair> movieSpecificTimeBiasMap = timeOffsetTemp
					.get(r.getMovieId());
			if (movieSpecificTimeBiasMap == null) {
				movieSpecificTimeBiasMap = new HashMap<>();
				timeOffsetTemp.put(r.getMovieId(), movieSpecificTimeBiasMap);
			}
			int bucketNo = r.getDateId() / daysPerBucket;
			AVGPair bucketBias = movieSpecificTimeBiasMap.get(bucketNo);
			if (bucketBias == null) {
				bucketBias = new AVGPair();
				movieSpecificTimeBiasMap.put(bucketNo, bucketBias);
			}

			bucketBias.add(offset);

			AVGPair bucketBiasOverall = overAllMovieTimeOffsetTemp
					.get(bucketNo);
			if (bucketBiasOverall == null) {
				bucketBiasOverall = new AVGPair();
				overAllMovieTimeOffsetTemp.put(bucketNo, bucketBias);
			}
			bucketBiasOverall.add(offset);
		}
	}

	private void trainAllTimeBias(List<Rating> toTrain, BaseLearner base) {
		for (Rating r : toTrain) {
			checkSetMinMax(r);

			AVGPair avg = movieBias.get(r.getMovieId());
			if (avg == null) {
				avg = new AVGPair();
				movieBias.put(r.getMovieId(), avg);
			}
			avg.add(base.getDelta(r) - r.getRating());
		}
	}

	private void checkSetMinMax(Rating r) {
		checkSetMax(r);

		checkSetMin(r);

	}

	private void checkSetMin(Rating r) {
		if (r.getDateId() < oMinDay)
			oMinDay = r.getDateId();

		if (!minDay.containsKey(r.getMovieId())) {
			minDay.put(r.getMovieId(), r.getDateId());
		} else {
			int currentMax = minDay.get(r.getMovieId());
			if (r.getDateId() < currentMax) {
				minDay.put(r.getMovieId(), r.getDateId());
			}
		}
	}

	private void checkSetMax(Rating r) {
		if (r.getDateId() > oMaxDay)
			oMaxDay = r.getDateId();

		if (!maxDay.containsKey(r.getMovieId())) {
			maxDay.put(r.getMovieId(), r.getDateId());
		} else {
			int currentMax = maxDay.get(r.getMovieId());
			if (r.getDateId() > currentMax) {
				maxDay.put(r.getMovieId(), r.getDateId());
			}
		}
	}

	private int getMin(Rating r) {
		if (!minDay.containsKey(r.getMovieId())) {
			minDay.put(r.getMovieId(), r.getDateId());
			return r.getDateId();
		} else
			return minDay.get(r.getDateId());
	}

	private int getMax(Rating r) {
		if (!maxDay.containsKey(r.getMovieId())) {
			maxDay.put(r.getMovieId(), r.getDateId());
			return r.getDateId();
		} else
			return maxDay.get(r.getDateId());
	}

	@Override
	public double getDelta(Rating rating) {
		AVGPair avg = movieBias.get(rating.getMovieId());
		if (avg == null) {
			avg = new AVGPair();
			movieBias.put(rating.getMovieId(), avg);
		}
		return avg.getAVG() + calculateTimeOffset(rating);
	}

	private double calculateTimeOffset(Rating rating) {
		int currentDay = rating.getDateId();
		int max = getMax(rating);
		int min = getMin(rating);
		if (min <= currentDay && currentDay <= max)
			return calculateMiddleOffset(rating);
		if (min > currentDay)
			return getAvgOfTimeBuket(rating.getMovieId(), min);

		return calculateFutureOffset(rating);
	}

	private double getAvgOfTimeBuket(int movieId, int day) {
		HashMap<Integer, Double> hm = timeOffset.get(movieId);
		if (hm == null) {
			if (oMinDay <= day && day <= oMaxDay)
				return overAllMovieTimeOffset.get(day / daysPerBucket);
			if (day < oMinDay)
				return overAllMovieTimeOffset.get(oMinDay / daysPerBucket);
			if (day > oMaxDay)
				return overAllMovieTimeOffset.get(oMaxDay / daysPerBucket);
		}
		Double d = hm.get(day / daysPerBucket);
		if (d == null)
			return hm.get(maxDay.get(movieId) / daysPerBucket);
		return d;
	}

	private double calculateFutureOffset(Rating rating) {
		int max = getMax(rating);
		// TODO triangulate linear
		return getAvgOfTimeBuket(rating.getMovieId(), max);
	}

	private double calculateMiddleOffset(Rating rating) {
		int day = rating.getDateId();
		int xCurrent = (day / daysPerBucket) * daysPerBucket + daysPerBucket
				/ 2;
		double yCurrent = getAvgOfTimeBuket(rating.getMovieId(), day);

		int xNext = xCurrent + daysPerBucket;
		double yNext = getAvgOfTimeBuket(rating.getMovieId(), xNext);

		return (yNext - yCurrent) / daysPerBucket * (day - xCurrent) + yCurrent;
	}

}
