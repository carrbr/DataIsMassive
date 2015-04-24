package ui.task;

public abstract class TaskCommand {

	protected boolean needsHelp = false;

	public abstract void exec() throws Exception;

	public void writeHelp() {
		System.out.println(this.getClass().getName()
				+ " does not have Help implemented!");
	}

	public boolean needsHelp(String[] args) {
		if (args.length == 0)
			return true;
		return args[0].toLowerCase().matches("\\?|help");
	}

}