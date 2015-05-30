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

		// split data/training.txt data/A 10
		// createRandom model/random
		// test model/random data/A_test data/A_test_result
		// rmse data/A_test data/A_test_result
		// test model/random data/test.txt data/result.txt
		// publish data/result.txt data/

		// createBI model/B0
		// trainBI model/B0 model/B1 data/training.txt 0
		// trainBI model/B2 model/B2 data/A_train 1
		// test model/B2 data/A_test data/A_test_result
		// rmse data/A_test data/A_test_result

		// testBI model/PM4 data/A_test data/A_test_result data/A_report
	}

	private static void handleInput(String[] args) throws Exception {
		if (args.length == 0)
			interactiveMode();
		else
			new MainTaskDelegation(args).exec();
	}

	private static void interactiveMode() throws Exception {

		System.out.println("Type '?' or 'help' to get help\n"
				+ "Type 'quit' or 'q' to quit, who would have guessed?");

		try (Scanner scan = new Scanner(System.in)) {
			while (true) {
				String[] args = readInCommand(scan);
				if (args.length > 0 && args[0].toLowerCase().matches("quit|q"))
					break;
				new MainTaskDelegation(args).exec();
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
