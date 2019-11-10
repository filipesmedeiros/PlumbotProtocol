package exceptions;

public class SingletonIsNullException extends Exception {
	
	public static final String MESSAGE = "Trying to get a singlton object that hasn't been initalized, of class ";

	public SingletonIsNullException(String className) {
		super(MESSAGE + className);
	}
}
