package ui;

import helper.TextToRatingReader;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;

import domain.Rating;
import domain.model.AbstractRatingModel;

public class TestModelTask implements TaskCommand {

	private String modelFile;
	private String testFile;
	private String resultFile;

	public TestModelTask(String modelFile, String testFile, String resultFile) {
		this.modelFile = modelFile;
		this.testFile = testFile;
		this.resultFile = resultFile;

	}

	@Override
	public void exec() throws Exception {
		AbstractRatingModel model = readInModel();
		
		TextToRatingReader in = null;
		BufferedWriter out = null;
		
		try{
			in = new TextToRatingReader(testFile);
			out = new BufferedWriter(new FileWriter(new File(resultFile)));
			Rating rateTest = null;
			while((rateTest = in.readNext()) != null){
				out.write(model.predict(rateTest)+"\n");
			}
			
		}finally{
			if(in != null) in.close();
			if(out != null) out.close();
		}
	}

	private AbstractRatingModel readInModel() throws IOException, ClassNotFoundException,
			FileNotFoundException {
		try(ObjectInputStream ois = new ObjectInputStream(new FileInputStream(new File(modelFile)))){
			return (AbstractRatingModel) ois.readObject();
		}
	}

}
