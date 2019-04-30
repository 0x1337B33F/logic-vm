package logicbyte.vm.runtime.func;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import logicbyte.vm.LogicVM;
import logicbyte.vm.VMDebugCallback;
import logicbyte.vm.VMInvokable;
import logicbyte.vm.runtime.BaseVMJFunctionImpl;
import logicbyte.vm.runtime.BaseVMJFunctionImpl.VMJFunctionLambdaInterface;
import logicbyte.vm.exception.ThrowObjectException;

/**
 *
 * @author Lochie
 */
public class RegexpFuncs {

    static class RegexpWrapperImpl {

	final Pattern patternObj;
	volatile Matcher matcherObj;

	RegexpWrapperImpl(Pattern patternObj) {
	    this.patternObj = patternObj;
	}

    }

    abstract static class RegexpFuncInternal implements VMJFunctionLambdaInterface {

	@Override
	public Object call(LogicVM vm, VMInvokable executor, VMDebugCallback debug, Object[] args) throws ThrowObjectException, Exception {
	    if (args.length == 0)
		throw new ThrowObjectException("matcher not provided");
	    if (!(args[0] instanceof RegexpWrapperImpl))
		throw new ThrowObjectException("argument 1 not regexp_matcher");
	    return tailcall((RegexpWrapperImpl) args[0], args);
	}

	public abstract Object tailcall(RegexpWrapperImpl matcher, Object[] args) throws ThrowObjectException;
    }

    public static RegexpWrapperImpl newMatcher(LogicVM vm, VMInvokable executor, VMDebugCallback debug, Object[] args) throws ThrowObjectException {
	if (args.length == 0)
	    throw new ThrowObjectException("pattern not provided");
	if (!(args[0] instanceof String))
	    throw new ThrowObjectException("argument 1 not string");
	return new RegexpWrapperImpl(Pattern.compile((String) args[0]));
    }

    public static void apply(LogicVM vm) throws NoSuchMethodException {

	vm.defineJFunc("StrRegExp_CreateMatcher", BaseVMJFunctionImpl.generateFunctionStatic(RegexpFuncs.class, "newMatcher"));

	vm.defineJFunc("StrRegExp_Update", BaseVMJFunctionImpl.generateFunctionLambda(new RegexpFuncInternal() {
	    @Override
	    public Object tailcall(RegexpWrapperImpl matcher, Object[] args) throws ThrowObjectException {
		if (args.length != 2)
		    throw new ThrowObjectException("subject not provided");
		if (!(args[1] instanceof String))
		    throw new ThrowObjectException("argument 2 not string");
		matcher.matcherObj = matcher.patternObj.matcher((String) args[1]);
		return matcher.matcherObj.matches();
	    }
	}));

	vm.defineJFunc("StrRegExp_Matches", BaseVMJFunctionImpl.generateFunctionLambda(new RegexpFuncInternal() {
	    @Override
	    public Object tailcall(RegexpWrapperImpl matcher, Object[] args) throws ThrowObjectException {
		if (matcher.matcherObj == null)
		    throw new ThrowObjectException("matcher not ready");
		return matcher.matcherObj.matches();
	    }
	}));

	vm.defineJFunc("StrRegExp_MatchNumGroups", BaseVMJFunctionImpl.generateFunctionLambda(new RegexpFuncInternal() {
	    @Override
	    public Object tailcall(RegexpWrapperImpl matcher, Object[] args) throws ThrowObjectException {
		if (matcher.matcherObj == null)
		    throw new ThrowObjectException("matcher not ready");
		return matcher.matcherObj.groupCount();
	    }
	}));

	vm.defineJFunc("StrRegExp_MatchGroup", BaseVMJFunctionImpl.generateFunctionLambda(new RegexpFuncInternal() {
	    @Override
	    public Object tailcall(RegexpWrapperImpl matcher, Object[] args) throws ThrowObjectException {
		if (matcher.matcherObj == null)
		    throw new ThrowObjectException("matcher not ready");
		if (args.length != 2)
		    throw new ThrowObjectException("index not provided");
		if (!(args[1] instanceof Integer))
		    throw new ThrowObjectException("argument 2 not integer");
		return matcher.matcherObj.group((Integer) args[1]);
	    }
	}));

	vm.defineJFunc("StrRegExp_MatchString", BaseVMJFunctionImpl.generateFunctionLambda(new RegexpFuncInternal() {
	    @Override
	    public Object tailcall(RegexpWrapperImpl matcher, Object[] args) throws ThrowObjectException {
		if (matcher.matcherObj == null)
		    throw new ThrowObjectException("matcher not ready");
		return matcher.matcherObj.group();
	    }
	}));

	vm.defineJFunc("StrRegExp_NextMatch", BaseVMJFunctionImpl.generateFunctionLambda(new RegexpFuncInternal() {
	    @Override
	    public Object tailcall(RegexpWrapperImpl matcher, Object[] args) throws ThrowObjectException {
		if (matcher.matcherObj == null)
		    throw new ThrowObjectException("matcher not ready");
		return matcher.matcherObj.find();
	    }
	}));

    }

}
