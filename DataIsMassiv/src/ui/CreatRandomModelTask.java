package ui;

import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;

import domain.model.RandomModel;

public class CreatRandomModelTask implements TaskCommand {

	private String fileOut;

	CreatRandomModelTask(String fileOut) {
		this.fileOut = fileOut;

	}

	@Override
	public void exec() throws Exception {
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
