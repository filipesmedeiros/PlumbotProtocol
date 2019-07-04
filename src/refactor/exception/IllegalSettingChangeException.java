package refactor.exception;

public class IllegalSettingChangeException extends Exception {
	
	public static final String MESSAGE = "This setting cannot be changed after the protocol execution has started: ";
	
	public IllegalSettingChangeException(String setting) {
		super(MESSAGE + setting);
	}

}
