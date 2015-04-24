package ui.task;

import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;

import domain.model.RandomModel;

public class CreatRandomModelTask extends TaskCommand {

	private String fileOut;

	CreatRandomModelTask(String fileOut) {
		this.fileOut = fileOut;

	}

	public CreatRandomModelTask(String[] args) {
		needsHelp = needsHelp(args);

		if (!needsHelp && args.length == 1) {
			this.fileOut = args[0];
		}
	}

	@Override
	public void exec() throws Exception {
		if (needsHelp) {
			writeHelp();
			return;
		}

		ObjectOutputStream oos = null;

		try {
			oos = new ObjectOutputStream(
					new FileOutputStream(new File(fileOut)));

			oos.writeObject(new RandomModel());

		} finally {
			if (oos != null)
				oos.close();
		}
	}

}
