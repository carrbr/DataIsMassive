package ui.task;

public abstract class TaskCommand {

	protected boolean needsHelp = false;

	public TaskCommand() {
	}

	public TaskCommand(String args[]) {
		needsHelp = needsHelp(args);
	}

	public abstract void exec() throws Exception;

	/**
	 * If help is needed it will be written and the subclass will know if the
	 * task needs to actually run or not.
	 * 
	 * @return
	 */
	protected boolean writeHelpIfNeeded() {
		if (needsHelp) {
			writeHelp();
			return true;
		}
		return false;
	}

	/**
	 * subclasses should over ride this to give information to the user on how
	 * to use the task.
	 */
	public void writeHelp() {
		System.out.println(this.getClass().getName()
				+ " does not have Help implemented!");
	}

	/**
	 * Standard way of identifying a problem needs to be overwritten if 0
	 * arguments are allowed
	 * 
	 * @param args
	 * @return
	 */
	public boolean needsHelp(String[] args) {
		if (args.length == 0)
			return true;
		return args[0].toLowerCase().matches("\\?|help");
	}

}