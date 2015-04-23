package ui.task;

public class MainTaskDelegation extends TaskCommand {

	private String[] args;

	public MainTaskDelegation(String[] args) {
		super(args);
		this.args = args;
	}

	@Override
	public void exec() throws Exception {
		if (writeHelpIfNeeded())
			return;

		String[] reduced = removeFirst();
		if (match(args, "split")) {
			new SplitDataTask(reduced).exec();
		} else if (match(args, "createRandom")) {
			new CreateRandomModelTask(reduced).exec();
		} else if (match(args, "test")) {
			new TestModelTask(reduced).exec();
		} else if (match(args, "rmse")) {
			new RMSETask(reduced).exec();
		} else if (match(args, "publish")) {
			new PublishResultTask(reduced).exec();
		} else {
			System.out.println("No matching command found");
			writeHelp();
		}

	}

	private String[] removeFirst() {
		String[] reduced = new String[args.length - 1];
		System.arraycopy(args, 1, reduced, 0, args.length - 1);
		return reduced;
	}

	private boolean match(String[] args, String s) {
		return args[0].toLowerCase().equals(s.toLowerCase());
	}

	@Override
	public void writeHelp() {
		System.out.println("Use the following commands:");
		System.out.println("split ?");
		System.out.println("creatRandnom ?");
		System.out.println("test ?");
		System.out.println("rmse ?");
		System.out.println("publish ?");
		System.out.println("quit");
	}

}
