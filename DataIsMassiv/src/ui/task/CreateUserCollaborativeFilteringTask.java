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
/*TODO remove??????????????????????????????????????
 * I see very little reason to actually serialize this to a file as it takes very little time to run it anyways
 * 
 * I'm just going to go ahead and run my tests from here without the serialization and deserialization steps in
 * in the middle.
 */
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
