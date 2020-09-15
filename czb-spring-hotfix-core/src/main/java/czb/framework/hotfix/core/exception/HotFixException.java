package czb.framework.hotfix.core.exception;

public class HotFixException extends RuntimeException{

    public HotFixException() {
    }

    public HotFixException(String message) {
        super(message);
    }

    public HotFixException(String message, Throwable cause) {
        super(message, cause);
    }
}
