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

		// createBI model/LFFT7.model 300
		// trainBI model/LFFT7.model model/LFFT7.model data/training.txt 0
		// trainBI model/LFFT7.model model/LFFT7.model data/training.txt 1 1
		// trainBI model/LFFT7.model model/LFFT7.model data/training.txt 1 0.75
		// trainBI model/LFFT7.model model/LFFT7.model data/training.txt 1 0.5
		// trainBI model/LFFT7.model model/LFFT7.model data/training.txt 1 0.25
		// trainBI model/LFFT7.model model/LFFT7.model data/training.txt 1 0.01
		// trainBI model/LFFT2.model model/LFFT2.model data/A_train2 1 1
		// test model/LFFT.model data/A_test data/A_test_result
		// rmse data/A_test data/A_test_result
		
		// testBI model/PM4 data/A_test data/A_test_result data/A_report

		// split data/training.txt data/UCF 11
		// createUCF model/UCF107_101.model data/UCF107_101_train 100
		// test model/UCF107_101.model data/UCF107_101_test data/UCF107_101_test_result
		// rmse data/UCF107_101_test data/UCF107_101_test_result

		// split data/training.txt data/M 11
		// createMCF model/MCF.model data/UCF107_101_train 100
		// test model/MCF.model data/UCF107_101_test data/MCF107_101_test_result
		// rmse data/UCF107_101_test data/MCF107_101_test_result

		// createUCF model/BUCF.model data/UCF107_101_train 100 back
		// test model/BUCF.model data/UCF107_101_test data/BUCF107_101_test_result
		// rmse data/UCF107_101_test data/BUCF107_101_test_result

		// createMCF model/BMCF.model data/UCF107_101_train 100 back
		// test model/BMCF.model data/UCF107_101_test data/BMCF107_101_test_result
		// rmse data/UCF107_101_test data/BMCF107_101_test_result

		// combine data/combine_result data/UCF.log data/MCF.log data/BMCF.log
		// rmse data/UCF107_101_test data/combine_result
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
