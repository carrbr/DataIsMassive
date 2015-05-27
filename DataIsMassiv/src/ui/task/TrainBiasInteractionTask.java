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

import domain.Rating;
import domain.model.BiasInteractionModel;

public class TrainBiasInteractionTask extends TaskCommand {

	private String fileIn;
	private String fileOut;
	private String fileTrainSet;
	private int mode;

	public TrainBiasInteractionTask(String[] args) {
		super(args);

		if (!needsHelp & args.length == 4) {
			this.fileIn = args[0];
			this.fileOut = args[1];
			this.fileTrainSet = args[2];
			this.mode = Integer.parseInt(args[3]);
		}else{
			needsHelp = true;
		}
	}

	@Override
	public void exec() throws Exception {
		if (writeHelpIfNeeded())
			return;
		BiasInteractionModel model = readInModel(fileIn);
		List<Rating> trainSet = readInTrainSet(fileTrainSet);

		if(mode == 0)
			model.train(trainSet);
		if(mode == 1)
			model.trainNN(trainSet);

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

	private void writeOutModel(String fileOut, BiasInteractionModel model)
			throws FileNotFoundException, IOException {
		try (ObjectOutputStream oos = new ObjectOutputStream(
				new FileOutputStream(new File(fileOut)))) {
			oos.writeObject(model);
		}

	}

	private BiasInteractionModel readInModel(String fileIn)
			throws FileNotFoundException, IOException, ClassNotFoundException {
		try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(
				new File(fileIn)))) {
			return (BiasInteractionModel) ois.readObject();
		}
	}

}
