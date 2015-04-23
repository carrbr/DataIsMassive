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

		handleInput();

		// split data/training.txt data/A 10
		// createRandom model/random
		// test model/random data/A_test data/A_test_result
		// rmse data/A_test data/A_test_result
		// test model/random data/test.txt data/result.txt
		// publish data/result.txt data/

	}

	private static void handleInput() throws Exception {
		Scanner s = new Scanner(System.in);
		// let the user perform as many actions as they like without having
		// to restart the program every time
		while (true) {
			String[] args = readInCommand(s);	
			// does user want to quit?
			if (args.length > 0 && args[0].equalsIgnoreCase("quit")) {
				break;
			}
			new MainTaskDelegation(args).exec();
		}
	}

	private static String[] readInCommand(Scanner s) {
		String command[] = null;
		String line = null;
		
		System.out.println("Type '?' or 'help' to get help");

		if (s.hasNext()) {
			line = s.nextLine();
		}
		if (!line.equals("")) {
			command = line.split(" ");
		}

		return command;
	}

}
