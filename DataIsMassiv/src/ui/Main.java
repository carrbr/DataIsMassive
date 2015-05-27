/**
 * 
 */
package ui;

import java.util.Scanner;

import ui.task.MainTaskDelegation;

/**
 * Start off point, Task register here to be chosen
 * 
 * @author Elias Geisseler
 *
 */
public class Main {

	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {

		handleInput(args);

		// split data/training.txt data/A 10 0
		// createRandom model/random
		// test model/random data/A_test data/A_test_result
		// rmse data/A_test data/A_test_result
		// test model/random data/test.txt data/result.txt
		// publish data/result.txt data/

		// createNN model/NN
		// test model/NN data/A_test data/A_test_result
		// rmse data/A_test data/A_test_result
		// trainNN model/NN model/NN2 data/A_test 500000
		// test model/NN2 data/A_test data/A_test_result
		// rmse data/A_test data/A_test_result
		// trainNN model/NN3_1 model/NN3_2 data/training.txt 21000000
		
		// split data/training.txt data/UCF 11
		// createUCF model/UCF.model data/A_train 100
		// test model/UCF.model data/UCF_test data/UCF_test_result
		// rmse data/UCF_test data/UCF_test_result
		
		// split data/training.txt data/M 11
		// createMCF model/MCF.model data/M_train 100
		// test model/MCF.model data/M_test data/M_test_result
		// rmse data/M_test data/M_test_result
	}

	private static void handleInput(String[] args) throws Exception {
		if (args.length == 0)
			interactiveMode();
		else
			new MainTaskDelegation(args).exec();
	}

	private static void interactiveMode() throws Exception {

		System.out.print("Type '?' or 'help' to get help\n"
				+ "Type 'quit' or 'q' to quit, who would have guessed?\n~$ ");

		try (Scanner scan = new Scanner(System.in)) {
			while (true) {
				String[] args = readInCommand(scan);
				if (args.length > 0 && args[0].toLowerCase().matches("quit|q"))
					break;
				new MainTaskDelegation(args).exec();
				System.out.print("~$ ");
			}
		}
	}

	private static String[] readInCommand(Scanner scan) {

		String command[] = null;
		do {
			command = scan.nextLine().split(" ");
		} while (command[0].equals(""));
		return command;
	}

}
