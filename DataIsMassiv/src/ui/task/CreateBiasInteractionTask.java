package ui.task;

import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;

import domain.model.BiasInteractionModel;

public class CreateBiasInteractionTask extends TaskCommand {

	private String fileOut;
	private int featureSize;

	public CreateBiasInteractionTask(String[] args) {
		super(args);

		if (!needsHelp && (args.length == 1 || args.length == 2)) {

			this.fileOut = args[0];
			if (args.length == 2)
				this.featureSize = Integer.parseInt(args[1]);
			else
				this.featureSize = 50;
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

			oos.writeObject(new BiasInteractionModel(featureSize));
			System.out.println("new "
					+ BiasInteractionModel.class.getCanonicalName()
					+ " created");

		} finally {
			if (oos != null)
				oos.close();
		}

	}

}
