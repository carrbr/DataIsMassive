package ui.task;

import helper.TextToRatingReader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

import domain.LearningCardNN;
import domain.Rating;
import domain.model.NeuronInputBackModel;

public class TrainNNTask extends TaskCommand {

	private String fileIn;
	private String fileOut;
	private String fileTrainSet;
	private int numOfQuestions;
	private LearningCardNN howToLearn;

	public TrainNNTask(String[] args) {
		super(args);

		if (!needsHelp & args.length == 4) {
			this.fileIn = args[0];
			this.fileOut = args[1];
			this.fileTrainSet = args[2];
			this.numOfQuestions = Integer.valueOf(args[3]);
			setUPLearningCard();
		}
	}

	private void setUPLearningCard() {
		howToLearn = new LearningCardNN();
		howToLearn.etaNN = .4;
		howToLearn.etaMovie = .5;
		howToLearn.etaUser = .5;

		howToLearn.resetMovie = false;
		howToLearn.resetUser = false;

		howToLearn.resetRateMovie = .005;
		howToLearn.resetRateUser = .007;

	}

	@Override
	public void exec() throws Exception {
		if (writeHelpIfNeeded())
			return;
		NeuronInputBackModel model = readInModel(fileIn);
		List<Rating> trainSet = readInTrainSet(fileTrainSet);

		model.trainParallel(trainSet, howToLearn, numOfQuestions);

		writeOutModel(fileOut, model);
	}

	private List<Rating> readInTrainSet(String fileTrainSet) throws IOException {
		TextToRatingReader reader = null;
		ArrayList<Rating> list = new ArrayList<>(50000);
		try {
			reader = new TextToRatingReader(fileTrainSet);
			Rating r = null;
			while ((r = reader.readNext()) != null) {
				list.add(r);
			}

		} finally {
			if (reader != null)
				reader.close();
		}

		return list;
	}

	private void writeOutModel(String fileOut, NeuronInputBackModel model)
			throws FileNotFoundException, IOException {
		try (ObjectOutputStream oos = new ObjectOutputStream(
				new FileOutputStream(new File(fileOut)))) {
			oos.writeObject(model);
		}

	}

	private NeuronInputBackModel readInModel(String fileIn)
			throws FileNotFoundException, IOException, ClassNotFoundException {
		try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(
				new File(fileIn)))) {
			return (NeuronInputBackModel) ois.readObject();
		}
	}

}
