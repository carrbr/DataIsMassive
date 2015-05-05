package ui.task;

import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;

import domain.model.NeuronInputBackModel;

public class CreateNeuralModelTask extends TaskCommand {

	private String fileOut;

	public CreateNeuralModelTask(String[] args) {
		super(args);

		if (!needsHelp && args.length == 1) {

			this.fileOut = args[0];
		}
	}

	@Override
	public void exec() throws Exception {
		if (writeHelpIfNeeded())
			return;

		ObjectOutputStream oos = null;

		try {
			oos = new ObjectOutputStream(
					new FileOutputStream(new File(fileOut)));

			oos.writeObject(new NeuronInputBackModel());

		} finally {
			if (oos != null)
				oos.close();
		}

	}

}
