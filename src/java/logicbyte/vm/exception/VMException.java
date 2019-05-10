package logicbyte.vm.exception;

import logicbyte.bytecode.LogicBytecodeFile.BytecodeFunctionDecl.DebugLocationSegment;
import logicbyte.bytecode.LogicBytecodeFile.BytecodeFunctionDecl.DebugSegment;
import logicbyte.vm.LogicVM;
import logicbyte.vm.VMFunction;
import logicbyte.vm.VMInvokable;
import logicbyte.vm.VMInvokableAccess;

/**
 *
 * @author Lochie
 */
public class VMException extends Exception {

    public static class VMExceptionStackTraceElement {

	public String sourceName, funcName;
	public int debugLine, debugCol;

    }

    protected static final VMExceptionStackTraceElement[] EMPTY_STACK = new VMExceptionStackTraceElement[ 0 ];

    protected LogicVM vmInstanceObj;
    protected VMInvokable vmInvokerObj;
    protected VMExceptionStackTraceElement[] vmStackTrace;

    public VMException(String message) {
	this(null, null, message, null);
    }

    public VMException(String message, Throwable inner) {
	this(null, null, message, inner);
    }

    public VMException(LogicVM vm, String message) {
	this(vm, null, message, null);
    }

    public VMException(LogicVM vm, String message, Throwable inner) {
	this(vm, null, message, inner);
    }

    public VMException(LogicVM vm, VMInvokable invoker, String message, Throwable inner) {
	super(message, inner);
	this.vmInstanceObj = vm;
	this.vmInvokerObj = invoker;
	fillInVMStackTrace();
    }

    protected void fillInVMStackTrace() {
	if (vmInvokerObj == null)
	    this.vmStackTrace = EMPTY_STACK;
	else {
	    VMInvokableAccess proxy = vmInvokerObj.access();
	    if (proxy == null)
		this.vmStackTrace = EMPTY_STACK;
	    else {
		final int frames = proxy.getCallStackSize();
		if (frames == 0)
		    this.vmStackTrace = EMPTY_STACK;
		else {
		    this.vmStackTrace = new VMExceptionStackTraceElement[ frames ];
		    for (int i = 0; i < frames; i++) {
			VMFunction func = proxy.getFunctionOnCallStack(i);

			this.vmStackTrace[i].funcName = func.name();
			this.vmStackTrace[i].sourceName = func.closure().getBytecode().source();
			this.vmStackTrace[i].debugLine = this.vmStackTrace[i].debugCol = -1;

			if (func.bytecode().hasdebug()) {
			    final int programCount = proxy.getFunctionProgramCounter(i);
			    final DebugSegment debugInfo = func.bytecode().debug();
			    for (int j = 0, k = debugInfo.numlocations(); j < k; j++) {
				int addr = debugInfo.locaddr(j);
				if (programCount == addr) {
				    final DebugLocationSegment opDebug = debugInfo.loctag(j);
				    this.vmStackTrace[i].debugLine = opDebug.line;
				    this.vmStackTrace[i].debugCol = opDebug.col;
				    break;
				}
			    }
			}
		    }
		}
	    }
	}
    }

    public boolean hasVMStackTrace() {
	return (this.vmStackTrace != null && this.vmStackTrace != EMPTY_STACK);
    }

    public VMExceptionStackTraceElement[] getVMStackTrace() {
	return (this.vmStackTrace == null) ? EMPTY_STACK : this.vmStackTrace;
    }

}
