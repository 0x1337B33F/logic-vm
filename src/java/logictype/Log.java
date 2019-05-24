package logictype;

import java.lang.reflect.Method;
import java.util.LinkedList;

/**
 *
 * @author Lochie
 */
public interface Log {

    /**
     * The logging level, enum.
     */
    public static enum Level {
	TRACE, DEBUG, INFO, WARN, ERR;
    }

    /**
     * The log receiver lambda interface that receives unformatted logging data.
     */
    public static interface LogReceiver {

	public void log(Level l, StackTraceElement[] stack, String format, Object... params);
    }

    /**
     * The log receiver lambda interface that receives pre-formatted logging data.
     */
    public static abstract class LogReceiverFormatted implements LogReceiver {

	private static Method formatter = null;

	static {
	    try {
		formatter = String.class.getMethod("format", new Class<?>[] {String.class, Object[].class});
	    } catch (NoSuchMethodException | SecurityException e) {
		throw new ExceptionInInitializerError(e);
	    }
	}

	public void log(Level l, StackTraceElement[] stack, String format, Object... params) {
	    Throwable t = null;
	    for (int i = 0, j = params.length; i < j; i++)
		if (params[i] instanceof Throwable) {
		    t = (Throwable) params[i];
		    break;
		}

	    try {
		if (t == null) {
		    Object[] params0 = new Object[ params.length + 1 ];
		    params0[0] = format;
		    System.arraycopy(params, 0, params0, 1, params.length);
		    tailcall(l, stack, (String) formatter.invoke(null, params0), null, params);
		} else {
		    Object[] params0 = new Object[ params.length ];
		    params0[0] = format;
		    boolean flag = false;
		    for (int i = 0, j = params.length, k = 1; i < j; i++)
			if (!flag && params[i] instanceof Throwable)
			    flag = true;
			else
			    params0[k++] = params[i];
		    tailcall(l, stack, (String) formatter.invoke(null, params0), t, params);
		}
	    } catch (ReflectiveOperationException e) {
		System.err.println("Exception when formatting log entry:");
		e.printStackTrace(System.err);
	    }
	}

	public abstract void tailcall(Level l, StackTraceElement[] stack, String message, Throwable cause, Object... params);
    }

    /**
     * The log implementation that does standard dispatching of log data.
     */
    public static class BaseLog implements Log {

	final LinkedList<LogReceiver> loggers = new LinkedList<>();

	public void addReceiver(LogReceiver r) {
	    synchronized ( this.loggers ) {
		this.loggers.add(r);
	    }
	}

	public void removeReceiver(LogReceiver r) {
	    synchronized ( this.loggers ) {
		this.loggers.remove(r);
	    }
	}

	void log(Level l, String format, Object params) {
	    final StackTraceElement[] stack = Thread.currentThread().getStackTrace();
	    final StackTraceElement[] stack0 = new StackTraceElement[ stack.length - 3 ];
	    System.arraycopy(stack, 3, stack0, 0, stack0.length);
	    synchronized ( this.loggers ) {
		for (int i = 0, j = this.loggers.size(); i < j; i++)
		    this.loggers.get(i).log(l, stack0, format, params);
	    }
	}

	@Override
	public void trace(String message, Object... params) {
	    log(Level.TRACE, message, params);
	}

	@Override
	public void debug(String message, Object... params) {
	    log(Level.DEBUG, message, params);
	}

	@Override
	public void info(String message, Object... params) {
	    log(Level.INFO, message, params);
	}

	@Override
	public void warn(String message, Object... params) {
	    log(Level.WARN, message, params);
	}

	@Override
	public void err(String message, Object... params) {
	    log(Level.ERR, message, params);
	}
    }

    /**
     * The log implementation that does nothing with the log data.
     */
    public static class VoidLog implements Log {

	/**
	 * The global singleton of the null log implementation.
	 */
	public static final VoidLog INSTANCE = new VoidLog();

	@Override
	public void trace(String message, Object... params) {
	}

	@Override
	public void debug(String message, Object... params) {
	}

	@Override
	public void info(String message, Object... params) {
	}

	@Override
	public void warn(String message, Object... params) {
	}

	@Override
	public void err(String message, Object... params) {
	}

    }

    public void trace(String message, Object... params);

    public void debug(String message, Object... params);

    public void info(String message, Object... params);

    public void warn(String message, Object... params);

    public void err(String message, Object... params);

}
