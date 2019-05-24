package logicbyte.vm.runtime.factory;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import logicbyte.loader.DynamicLibraryFile;
import logicbyte.vm.LogicVM;
import logicbyte.vm.factory.LogicVMFactory;
import logicbyte.vm.factory.VMClosureFactory;
import logicbyte.vm.factory.VMTypeFactory;
import logicbyte.vm.runtime.BaseLogicVMInstance;
import logicbyte.vm.runtime.BaseVMJFunctionImpl;
import logicbyte.vm.runtime.func.BaseFuncs;

/**
 * <p>
 * Creates basic virtual machine instances with no additional mutations. Includes only the basic library of core functions, such as those to
 * get the type of a handle reference, the instanceof comparator, the string functions and the basic type-casting functions.</p>
 *
 * <p>
 * Additional functions can be loaded as required as follows:
 * <pre>
 * BaseVMFactory.applyStaticMethods( vm, ConsoleFuncs.class );
 * ListFuncs.apply( vm, namespace );
 * RegexpFuncs.apply( vm, namespace );
 * CoroutineFuncs.apply( vm, namespace );</pre>
 * </p>
 *
 * @author Lochie
 */
public class BaseVMFactory implements LogicVMFactory {

    @Override
    public LogicVM create(DynamicLibraryFile loader, VMClosureFactory closureFactory, VMTypeFactory typeFactory) throws Exception {
	final BaseLogicVMInstance alloc = new BaseLogicVMInstance(loader, closureFactory, typeFactory);
	applyStaticMethods(alloc, BaseFuncs.class);
	return alloc;
    }

    public static final void applyStaticMethods(LogicVM inst, Class<?> klazz) throws Exception {
	Map<String, BaseVMJFunctionImpl> map = BaseVMJFunctionImpl.generateFunctionClassStatic(klazz);
	Iterator<Entry<String, BaseVMJFunctionImpl>> itx = map.entrySet().iterator();
	while (itx.hasNext()) {
	    Entry<String, BaseVMJFunctionImpl> func = itx.next();
	    inst.defineJFunc(func.getKey(), func.getValue());
	}
    }

}
