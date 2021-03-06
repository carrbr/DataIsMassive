package ui.task;

import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;

import domain.model.UserBackwardsCollaborativeFilteringModel;
import domain.model.UserCollaborativeFilteringModel;

public class CreateUserCollaborativeFilteringTask extends TaskCommand {

	private String fileOut;
	private String trainingFile;
	private int numSimilarUsers;
	private boolean backwards;

	public CreateUserCollaborativeFilteringTask(String[] args) {
		super(args);

		if (!needsHelp && args.length >= 3) {

			this.fileOut = args[0];
			this.trainingFile = args[1];
			this.numSimilarUsers = Integer.parseInt(args[2]);
			if (args.length >= 4 && args[3].equals("back")) {
				this.backwards = true;
			} else {
				this.backwards = false;
			}
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

			if (!this.backwards) {
				oos.writeObject(new UserCollaborativeFilteringModel(
						this.trainingFile, this.numSimilarUsers, 0.02, 10, 0.5));
			} else {
				oos.writeObject(new UserBackwardsCollaborativeFilteringModel(
						this.trainingFile, this.numSimilarUsers, 0.02, 10, 0.5));
			}

		} finally {
			if (oos != null)
				oos.close();
		}

	}

}
