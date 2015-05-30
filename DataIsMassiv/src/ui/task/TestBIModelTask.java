package ui.task;

import helper.TextToRatingReader;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;

import domain.DataMonitor;
import domain.Rating;
import domain.model.BiasInteractionModel;

public class TestBIModelTask extends TaskCommand {

	private String modelFile;
	private String testFile;
	private String resultFile;
	private String monitorFile;

	public TestBIModelTask(String modelFile, String testFile,
			String resultFile, String monitor) {
		this.modelFile = modelFile;
		this.testFile = testFile;
		this.resultFile = resultFile;

	}

	public TestBIModelTask(String[] args) {
		super(args);

		if (!needsHelp && args.length == 4) {
			this.modelFile = args[0];
			this.testFile = args[1];
			this.resultFile = args[2];
			this.monitorFile = args[3];
		}
	}

	@Override
	public void exec() throws Exception {
		if (writeHelpIfNeeded())
			return;

		BiasInteractionModel model = readInModel();

		TextToRatingReader in = null;
		BufferedWriter out = null;

		try {
			in = new TextToRatingReader(testFile);
			out = new BufferedWriter(new FileWriter(new File(resultFile)));

			testEachLine(model, in, out);

		} finally {
			if (in != null)
				in.close();
			if (out != null)
				out.close();
		}
	}

	private void testEachLine(BiasInteractionModel model,
			TextToRatingReader in, BufferedWriter out) throws IOException {
		Rating rateTest = null;
		DataMonitor monitor = new DataMonitor();
		model.setMonitor(monitor);

		while ((rateTest = in.readNext()) != null) {
			out.write(model.predict(rateTest) + "\n");
		}
		writeOutMonitorReport(monitor);
	}

	private void writeOutMonitorReport(DataMonitor monitor) {
		try (BufferedWriter bw = new BufferedWriter(new FileWriter(new File(
				monitorFile)))){
			
			monitor.printReportTo(bw);

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private BiasInteractionModel readInModel() throws IOException,
			ClassNotFoundException, FileNotFoundException {
		try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(
				new File(modelFile)))) {
			return (BiasInteractionModel) ois.readObject();
		}
	}

}
