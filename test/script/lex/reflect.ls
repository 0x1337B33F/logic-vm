
/***
 ** The reflection functions library
 **/

imports
	type coroutine
    from root.coroutine
endimports

/** The type used to express assembled data */
type	bytecode	    extends handle
/** The type used to express invokable references */
type	invoke	    extends handle

/** Reflect_Compile: compile code chunk provided to bytecode ref */
native function	    Reflect_Compile	    takes string chunk					returns bytecode
/** Reflect_Current: get current bytecode ref */
native function	    Reflect_Current	    takes nothing					returns bytecode

/** Reflect_GetFunc: get function in target bytecode ref by name */
native function	    Reflect_GetFunc	    takes bytecode chunk, string name			returns code
/** Reflect_GetFuncNumArgs: get function arg count */
native function	    Reflect_GetFuncNumArgs  takes code func					returns integer
/** Reflect_GetFuncArgType: get function arg type in slot */
native function	    Reflect_GetFuncArgType  takes code func, integer slot			returns string
/** Reflect_GetFuncRetnType: get function return type */
native function	    Reflect_GetFuncRetnType takes code func					returns string
/** Reflect_NewFuncInvoke: get new function invokable */
native function	    Reflect_NewFuncInvoke   takes code func					returns invoke

/** Reflect_InvokeSetArgB: set boolean argument for invoke in slot */
native function	    Reflect_InvokeSetArgB   takes invoke e, integer slot, boolean b		returns nothing
/** Reflect_InvokeSetArgI: set integer argument for invoke in slot */
native function	    Reflect_InvokeSetArgI   takes invoke e, integer slot, integer i		returns nothing
/** Reflect_InvokeSetArgR: set real argument for invoke in slot */
native function	    Reflect_InvokeSetArgR   takes invoke e, integer slot, real r		returns nothing
/** Reflect_InvokeSetArgS: set string argument for invoke in slot */
native function	    Reflect_InvokeSetArgS   takes invoke e, integer slot, string s		returns nothing
/** Reflect_InvokeSetArgH: set handle argument for invoke in slot */
native function	    Reflect_InvokeSetArgH   takes invoke e, integer slot, handle h		returns nothing

/** Reflect_InvokeApply: emit new executable coroutine from invoker using args set (SetArgT) */
native function	    Reflect_InvokeApply	    takes invoke e					returns coroutine

/** Reflect_GetGlobalType: get type of global */
native function	    Reflect_GetGlobalType   takes bytecode chunk, string name			returns string
/** Reflect_GetGlobalArray: if global is array, returns array size; else, 0 */
native function	    Reflect_GetGlobalArray  takes bytecode chunk, string name			returns integer

/** Reflect_GetGlobalValB: get boolean value of global */
native function	    Reflect_GetGlobalValB    takes bytecode chunk, string name			returns boolean
/** Reflect_GetGlobalValI: get integer value of global */
native function	    Reflect_GetGlobalValI    takes bytecode chunk, string name			returns integer
/** Reflect_GetGlobalValR: get real value of global */
native function	    Reflect_GetGlobalValR    takes bytecode chunk, string name			returns real
/** Reflect_GetGlobalValS: get string value of global */
native function	    Reflect_GetGlobalValS    takes bytecode chunk, string name			returns string
/** Reflect_GetGlobalValH: get handle value of global */
native function	    Reflect_GetGlobalValH    takes bytecode chunk, string name			returns handle

/** Reflect_GetGlobalArrValB: get boolean value of global in array idx */
native function	    Reflect_GetGlobalArrValB	takes bytecode chunk, string name, integer idx	returns boolean
/** Reflect_GetGlobalArrValB: get integer value of global in array idx */
native function	    Reflect_GetGlobalArrValI	takes bytecode chunk, string name, integer idx	returns integer
/** Reflect_GetGlobalArrValB: get real value of global in array idx */
native function	    Reflect_GetGlobalArrValR	takes bytecode chunk, string name, integer idx	returns real
/** Reflect_GetGlobalArrValB: get string value of global in array idx */
native function	    Reflect_GetGlobalArrValS	takes bytecode chunk, string name, integer idx	returns string
/** Reflect_GetGlobalArrValB: get handle value of global in array idx */
native function	    Reflect_GetGlobalArrValH	takes bytecode chunk, string name, integer idx	returns handle

exports
    type	bytecode, invoke
    function    Reflect_Compile, Reflect_Current, Reflect_GetFunc, Reflect_GetFuncNumArgs, Reflect_GetFuncArgType,
		Reflect_GetFuncRetnType, Reflect_NewFuncInvoke, Reflect_InvokeSetArgB, Reflect_InvokeSetArgI,
		Reflect_InvokeSetArgR, Reflect_InvokeSetArgS, Reflect_InvokeSetArgH, Reflect_InvokeApply
    as	reflect.function
endexports

exports
    function    Reflect_GetGlobalType, Reflect_GetGlobalArray, 
		Reflect_GetGlobalValB, Reflect_GetGlobalValI, Reflect_GetGlobalValR, Reflect_GetGlobalValS, Reflect_GetGlobalValH, 
		Reflect_GetGlobalArrayValB, Reflect_GetGlobalArrayValI, Reflect_GetGlobalArrayValR, Reflect_GetGlobalArrayValS, 
		Reflect_GetGlobalArrayValueH
    as	reflect.globals
endexports