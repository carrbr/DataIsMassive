package domain.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealVector;

import domain.LearningCardNN;
import domain.Neural3LayerNetwork;
import domain.Neural3LayerNetwork.LearnResponse;
import domain.Rating;

public class NeuronInputBackModel extends AbstractRatingModel implements
		Serializable {
	private static final long serialVersionUID = 1L;
	private final Neural3LayerNetwork nn;

	private final HashMap<Integer, RealVector> user;
	private volatile ReadWriteLock rwlUser = new ReentrantReadWriteLock(true);
	private final HashMap<Integer, RealVector> movies;
	private volatile ReadWriteLock rwlMovies = new ReentrantReadWriteLock(true);
	private final int vecTime;
	private final int vecUser;
	private final int vecMovies;

	/**
	 * a unknown User will have the following 'initial' interest. The networks
	 * job is to recognize a new guy
	 */
	private final double[] newUserVec;
	private final double[] newMovieVec;

	public NeuronInputBackModel() {
		vecTime = 18;
		vecUser = 30;
		vecMovies = 30;
		int inputCount = vecTime + vecUser + vecMovies;
		nn = new Neural3LayerNetwork(inputCount, 50, 20, 1);
		user = new HashMap<>();
		movies = new HashMap<>();
		newUserVec = new double[vecUser];
		newMovieVec = new double[vecMovies];
		generateNewVec();
	}

	private void generateNewVec() {
		for (int i = 0; i < vecUser; i++) {
			newUserVec[i] = 0;
		}

		for (int i = 0; i < vecMovies; i++) {
			newMovieVec[i] = 0;
		}

	}

	private RealVector getMovieVector(int movieID) {
		rwlMovies.readLock().lock();
		RealVector v = movies.get(movieID);
		rwlMovies.readLock().unlock();
		if (v == null) {
			rwlMovies.writeLock().lock();
			if (movies.containsKey(movieID)) {
				v = movies.get(movieID);
				rwlMovies.writeLock().unlock();
				return v;
			}
			v = MatrixUtils.createRealVector(newUserVec);
			movies.put(movieID, v);
			rwlMovies.writeLock().unlock();
		}
		return v;
	}

	private RealVector getUserVector(int usrID) {
		rwlUser.readLock().lock();
		RealVector v = user.get(usrID);
		rwlUser.readLock().unlock();

		if (v == null) {
			rwlUser.writeLock().lock();
			// check if other thread has written it in the mean time
			if (user.containsKey(usrID)) {
				v = user.get(usrID);
				rwlUser.writeLock().unlock();
				return v;
			}

			v = MatrixUtils.createRealVector(newUserVec);
			user.put(usrID, v);
			rwlUser.writeLock().unlock();
		}
		return v;
	}

	private RealVector getMovieVector(int movieId, LearningCardNN card,
			Random random) {
		if (card.resetRateMovie > random.nextDouble()) {
			rwlMovies.writeLock().lock();
			movies.remove(movieId);
			rwlMovies.writeLock().unlock();
		}

		if (card.fakeInput > random.nextDouble()) {
			rwlMovies.readLock().lock();
			boolean exits = !movies.containsKey(movieId);
			rwlMovies.readLock().unlock();
			if (exits) {
				rwlMovies.writeLock().lock();
				movies.put(movieId, MatrixUtils.createRealVector(newMovieVec));
				rwlMovies.writeLock().unlock();
			}
			return MatrixUtils.createRealVector(newMovieVec);
		}
		return getMovieVector(movieId);
	}

	private RealVector getUserVector(int userId, LearningCardNN card,
			Random random) {
		if (card.resetRateUser > random.nextDouble()) {
			rwlUser.writeLock().lock();
			user.remove(userId);
			rwlUser.writeLock().unlock();
		}
		if (card.fakeInput > random.nextDouble()) {
			rwlUser.readLock().lock();
			boolean exits = !user.containsKey(userId);
			rwlUser.readLock().unlock();
			if (exits) {
				rwlUser.writeLock().lock();
				movies.put(userId, MatrixUtils.createRealVector(newUserVec));
				rwlUser.writeLock().unlock();
			}
			return MatrixUtils.createRealVector(newUserVec);
		}
		return getUserVector(userId);
	}

	private void mixMovieVector(int movieId, RealVector deltaMovie, double d) {
		rwlMovies.writeLock().lock();
		RealVector v = movies.get(movieId);
		v = v.add(deltaMovie.mapMultiply(d));
		movies.put(movieId, v);
		rwlMovies.writeLock().unlock();
		;
	}

	private void mixUserVector(int userId, RealVector deltaUser, double d) {
		rwlUser.writeLock().lock();
		RealVector v = user.get(userId);
		v = v.add(deltaUser.mapMultiply(d));
		user.put(userId, v);
		rwlUser.writeLock().unlock();
	}

	static private RealVector generateTimeVector(int time) {
		// First in 4 is frequeny, other 3 are phase translation
		double[][] para = { { 4984, 0, 1.1, 2.5 }, { 1460.97, .5, 1.8, 3.3 },
				{ 365.243, .2, 1.35, 2.6 }, { 29.6, 0, .9, 1.6 },
				{ 7, .2, .7, 2.1 }, { 3, .1, 1.5, 2.9 } };
		double[] data = new double[para.length * 3];
		for (int i = 0; i < para.length; i++) {
			double localPhase = time / para[i][0] * 2 * Math.PI;
			data[i * 3 + 0] = Math.sin(localPhase + para[i][1]) / 2 + .5;
			data[i * 3 + 1] = Math.sin(localPhase + para[i][2]) / 2 + .5;
			data[i * 3 + 2] = Math.sin(localPhase + para[i][3]) / 2 + .5;
		}
		return MatrixUtils.createRealVector(data);
	}

	private double predictionRescaleNetworkToMovieScale(double netScala) {
		return ((int) (netScala * 40 + 10)) / 10.0;
	}

	private RealVector getPerfectResponseFor(Rating rating,
			LearningCardNN card, Random random) {
		double[] r = new double[1];
		r[0] = rating.getRating();
		r[0] -= 1;
		r[0] /= 4;
		return MatrixUtils.createRealVector(r);
	}

	private void correctInputOf(Rating rating, RealVector correctionResponse,
			LearningCardNN card) {

		RealVector deltaUser = correctionResponse
				.getSubVector(vecTime, vecUser);

		mixUserVector(rating.getUserId(), deltaUser, card.etaUser);

		RealVector deltaMovie = correctionResponse.getSubVector(vecTime
				+ vecUser, vecMovies);

		mixMovieVector(rating.getMovieId(), deltaMovie, card.etaMovie);

	}

	private float rate(Rating rating) {
		RealVector in = getNetworkInputFor(rating);
		RealVector out = nn.respond(in);
		return (float) predictionRescaleNetworkToMovieScale(out.getEntry(0));
	}

	private RealVector getNetworkInputFor(Rating r) {
		RealVector vTime = generateTimeVector(r.getDateId());
		RealVector vUser = getUserVector(r.getUserId());
		RealVector vMovie = getMovieVector(r.getMovieId());
		RealVector in = vTime.append(vUser).append(vMovie);
		return in;
	}

	private RealVector getNetworkInputFor(Rating r, LearningCardNN card,
			Random random) {
		RealVector vTime = generateTimeVector(r.getDateId());
		RealVector vUser = getUserVector(r.getUserId(), card, random);
		RealVector vMovie = getMovieVector(r.getMovieId(), card, random);
		RealVector in = vTime.append(vUser).append(vMovie);
		return in;
	}

	@Override
	public Rating predict(Rating r) {
		return r.reRate(rate(r));
	}

	private class LearningThread extends Thread {

		private boolean running = true;
		private int stepcount;
		private Semaphore barrierLearner;
		private Semaphore updateWriteBlock;
		private LearningCardNN card;
		private List<Rating> trainSet;
		private LearnResponse allResponses;

		public LearningThread(int sepcount, Semaphore barrierLearner,
				Semaphore updateWriteBlock, LearningCardNN card,
				List<Rating> trainSet) {
			this.stepcount = sepcount;
			this.barrierLearner = barrierLearner;
			this.updateWriteBlock = updateWriteBlock;
			this.card = card;
			this.trainSet = trainSet;
		}

		@Override
		public void run() {
			try {
				Random random = new Random();
				System.out.println("worker Thread: " + this.getName()
						+ " has rand:" + random.nextLong());
				barrierLearner.acquire(); // Wait for initial start signal
				while (isRunning()) {
					learnCycle(random);
					updateWriteBlock.release();
					barrierLearner.acquire();
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		private synchronized void learnCycle(Random random)
				throws InterruptedException {

			LearnResponse allResponses = new LearnResponse();

			for (int i = 0; i < stepcount; i++) {

				Rating r = trainSet.get(random.nextInt(trainSet.size()));

				LearnResponse lr = nn.learn(
						getNetworkInputFor(r, card, random),
						getPerfectResponseFor(r, card, random));

				correctInputOf(r, lr.inputDelta, card);

				allResponses.mergeWith(lr);

			}
			allResponses.avgSumFactor(stepcount);
			setNetworkResponses(allResponses);
		}

		private synchronized void setNetworkResponses(LearnResponse allResponses) {
			this.allResponses = allResponses;
		}

		public synchronized LearnResponse getNetworkResponses() {
			return allResponses;
		}

		public synchronized boolean isRunning() {
			return running;
		}

		public synchronized void setRunning(boolean running) {
			this.running = running;
		}

	}

	public void trainParallel(List<Rating> trainSet, LearningCardNN howToLearn,
			int counts) throws InterruptedException {
		long timeStart = System.currentTimeMillis();
		long timeLastUpdate = timeStart;

		if (howToLearn.resetMovie)
			movies.clear();
		if (howToLearn.resetUser)
			user.clear();

		int nThreads = 4;
		int nGradientGroup = 7;

		Semaphore barrierLearner = new Semaphore(0);
		Semaphore updateWriteBlock = new Semaphore(0);

		ArrayList<LearningThread> threads = new ArrayList<>();

		for (int i = 0; i < nThreads; i++) {
			threads.add(new LearningThread(nGradientGroup, barrierLearner,
					updateWriteBlock, howToLearn, trainSet));
		}
		for (LearningThread t : threads) {
			t.start();
		}

		int totalDoneLearnCycles = 0;
		while (totalDoneLearnCycles < counts) {
			try {
				barrierLearner.release(nThreads); // Let workers Work
				updateWriteBlock.acquire(nThreads); // Wait till result is in

				LearnResponse response = new LearnResponse();
				for (LearningThread t : threads) {
					response.mergeWith(t.getNetworkResponses());
				}
				response.avgSumFactor(nThreads);
				nn.writeLayerUpdate(response, howToLearn);

				totalDoneLearnCycles += nGradientGroup * nThreads;

				double secondsSinceStart = (System.currentTimeMillis() - timeStart) / 1000.0;
				double timeSinceLastUpdate = System.currentTimeMillis()
						- timeLastUpdate;
				if (timeSinceLastUpdate > 10000) {
					timeLastUpdate = System.currentTimeMillis();
					System.out.println(((double) totalDoneLearnCycles) / counts
							* 100 + " % learn cycles since "
							+ secondsSinceStart + " seconds ago");
				}

			} catch (InterruptedException e) {
				stopThreads(threads);
				barrierLearner.release(nThreads);
				throw e;
			}
		}

		stopThreads(threads);
		barrierLearner.release(nThreads);
		for (LearningThread t : threads) {
			t.join();
		}

		System.out.println("Done after: "
				+ ((double) System.currentTimeMillis() - timeStart) / 1000.0
				+ " seconds");

	}

	private void stopThreads(ArrayList<LearningThread> threads) {
		for (LearningThread t : threads) {
			t.setRunning(false);
		}
	}
}
