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

	private static void handleInput(String [] args) throws Exception {
		Scanner s = new Scanner(System.in);
		
		// check if we are running a batch job from a script or an interactive, user job
		if (args.length == 0) { // no args -> interactive, user job
			// let the user perform as many actions as they like without having
			// to restart the program every time
			while (true) {
				args = readInCommand(s);	
				// does user want to quit?
				if (args.length > 0 && args[0].equalsIgnoreCase("quit")) {
					break;
				}
				new MainTaskDelegation(args).exec();
			}
		} else { // batch job with command line args
			new MainTaskDelegation(args).exec();
		}
		
		s.close();
	}

	private static String[] readInCommand(Scanner s) {
		String command[] = null;
		String line = null;
		
		System.out.println("Type '?' or 'help' to get help");

		if (s.hasNext()) {
			line = s.nextLine();
		}
		command = line.split(" ");


		return command;
	}

}
