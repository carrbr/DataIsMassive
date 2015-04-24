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

	}

	private static void handleInput(String[] args) throws Exception {
		if(args.length ==0){
			args = readInCommand();
		}
		
		new MainTaskDelegation(args).exec();
		
	}

	private static String[] readInCommand() {
		try (Scanner s = new Scanner(System.in)) {
			System.out.println("Type '?' or 'help' to get help");

			String command[] = null;
			do {
				command = s.nextLine().split(" ");
			} while (command.length == 0);

			return command;
		}
	}

}
