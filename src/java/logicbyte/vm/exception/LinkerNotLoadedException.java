package logicbyte.vm.exception;

import logicbyte.bytecode.LinkerObjectFile;
import logicbyte.vm.LogicVM;

/**
 *
 * @author Lochie
 */
public class LinkerNotLoadedException extends VMException {

    private final LinkerObjectFile targetLinker;

    public LinkerNotLoadedException(LogicVM vm, LinkerObjectFile targetLinker) {
	super(vm, null, null);
	this.targetLinker = targetLinker;
    }

    public final LinkerObjectFile getLinker() {
	return this.targetLinker;
    }
}
