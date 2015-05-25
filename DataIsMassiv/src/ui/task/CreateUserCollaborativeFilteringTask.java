package ui.task;

import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;

import domain.model.UserCollaborativeFilteringModel;

public class CreateUserCollaborativeFilteringTask extends TaskCommand {

	private String fileOut;
	private String trainingFile;
	private int numSimilarUsers;

	public CreateUserCollaborativeFilteringTask(String[] args) {
		super(args);

		if (!needsHelp && args.length == 3) {

			this.fileOut = args[0];
			this.trainingFile = args[1];
			this.numSimilarUsers = Integer.parseInt(args[2]);
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

			oos.writeObject(new UserCollaborativeFilteringModel(this.trainingFile, this.numSimilarUsers));

		} finally {
			if (oos != null)
				oos.close();
		}
		

	}

}
