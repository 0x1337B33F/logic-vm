package logicbyte.vm.factory;

import logicbyte.loader.DynamicLibraryFile;
import logicbyte.vm.LogicVM;

/**
 *
 * @author Lochie
 */
public interface LogicVMFactory {

    public LogicVM create(DynamicLibraryFile loader, VMClosureFactory closureFactory, VMTypeFactory typeFactory) throws Exception;

}
