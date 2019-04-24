package logicbyte.vm.runtime.factory;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import logicbyte.loader.DynamicLibraryFile;
import logicbyte.vm.LogicVM;
import logicbyte.vm.factory.LogicVMFactory;
import logicbyte.vm.factory.VMClosureFactory;
import logicbyte.vm.runtime.BaseLogicVMInstance;
import logicbyte.vm.runtime.BaseVMJFunctionImpl;
import logicbyte.vm.runtime.func.BaseFuncs;
import logicbyte.vm.runtime.func.ConsoleFuncs;
import logicbyte.vm.runtime.func.CoroutineFuncs;
import logicbyte.vm.runtime.func.ListFuncs;
import logicbyte.vm.runtime.func.RegexpFuncs;

/**
 *
 * @author Lochie
 */
public class BaseVMFactory implements LogicVMFactory {

    @Override
    public LogicVM create(DynamicLibraryFile loader, VMClosureFactory closureFactory) throws Exception {
	final BaseLogicVMInstance alloc = new BaseLogicVMInstance(loader, closureFactory);
	applyStaticMethods(alloc, ConsoleFuncs.class);
	applyStaticMethods(alloc, BaseFuncs.class);
	ListFuncs.apply(alloc);
	RegexpFuncs.apply(alloc);
	CoroutineFuncs.apply(alloc);
	return alloc;
    }

    protected void applyStaticMethods(LogicVM inst, Class<?> klazz) throws Exception {
	Map<String, BaseVMJFunctionImpl> map = BaseVMJFunctionImpl.generateFunctionClassStatic(klazz);
	Iterator<Entry<String, BaseVMJFunctionImpl>> itx = map.entrySet().iterator();
	while (itx.hasNext()) {
	    Entry<String, BaseVMJFunctionImpl> func = itx.next();
	    inst.defineJFunc(func.getKey(), func.getValue());
	}
    }

}
