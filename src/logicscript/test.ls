/**
 ** Test script to confirm that importing works correctly, specifically
 ** from the root types library (root.ls).
 **/

imports
    function	    
	PrintConsole, PrintError
    from    root.types
endimports


imports
    function 
	TypeOf, InstanceOf 
    from root.types 
endimports

imports
    type 
	lambdafunc, booleanfunc 
    from root.types 
endimports

imports
    function * 
    from root.casting 
endimports

imports
    global  MATH_PI
    from root.math
endimports


function	TestBasicFunc001    takes nothing	returns nothing
    call PrintConsole ( Str_Concat( "The value of pi is approximately ", Cast_R2S ( MATH_PI ) ) )
    call PrintConsole ( Str_Concat( "The type of the value of pi is ", TypeOf ( MATH_PI ) ) )
endfunction

function        SampleFunction      takes   nothing     returns     nothing
    call TestFunction()
    call TestOtherFunction()
endfunction


/* Direct invokes, etc: */
function        TestFunction        takes   nothing     returns     nothing
    call PrintConsole ( "Hello, world!" )
endfunction


/* Pointers, function types, code types, etc: */
function        TestOtherFunction   takes   nothing     returns     nothing
    local code pointer = function TestPointerFunction
    callvar pointer()
endfunction


function        TestPointerFunction takes   nothing     returns     nothing
    call PrintConsole ( "Invocation from user-code function pointer dereference!" )
endfunction


function	TestPerformRaise    takes   nothing	returns	    nothing
    raise "The quick brown fox jumps over the lazy dog."
endfunction


function	TestStackUnwind000  takes   nothing	returns	    nothing
    call TestPerformRaise()
endfunction

function	TestStackUnwind001  takes   nothing	returns	    nothing
    try
	call TestStackUnwind000()
    catch handle object do
	call PrintConsole ( "This should not occur" )
    catch integer intval do
	call PrintConsole ( "This should not occur" )
    catch real realval do
	call PrintConsole ( "This should not occur" )
    endtry
endfunction

function    TestStackUnwind002	    takes   nothing	returns	    nothing
    call TestStackUnwind001()
endfunction


function	TestTryCatch	    takes   nothing	returns	    nothing
    
    try
	call TestStackUnwind002()
    catch string message do
	call PrintConsole ( Str_Concat("TestTryCatch: ", message) )
    endtry

endfunction

/* Test if generating function pointers to native functions works properly:: */

function    TestNativeFunctionPointer000    takes nothing returns nothing
    local code pointer = function PrintConsole
    call TestNativeFunctionPointer001 ( pointer )
endfunction

function    TestNativeFunctionPointer001    takes code funcptr returns nothing
    callvar funcptr ( "native function pointer call" )
endfunction


/* Test if native functions can invoke function pointers: */


native function	    TestNativePassedFunctionPointerNativeCall	    takes code pointer returns string

function    TestNativePassedFunctionPointer000	takes nothing returns nothing
    local code pointer = function TestNativePassedFunctionPointer002
    local string result

    try
	set result = TestNativePassedFunctionPointerNativeCall ( pointer )
    catch string message do
	call PrintConsole ( Str_Concat( "caught: ", message ) )
	return
    endtry

    call PrintConsole ( Str_Concat ( "result: ", thrown ) )

endfunction

function    TestNativePassedFunctionPointer001	takes nothing returns string
    call PrintConsole ( "invocation of function pointer from native call" )
    return "the invocation yielded this string"
endfunction

function    TestNativePassedFunctionPointer002	takes nothing returns string
    call PrintConsole ( "invocation of function pointer from native call" )
    raise "testing that raising through native call functions"
    return "this should not occur"
endfunction


/* Test that native functions can throw primitive values: */

native function	    TestNativeThrowsExceptionAlways	takes nothing returns nothing

function    TestNativeFunctionThrowsPrimitive	takes nothing returns nothing
    try
	call TestNativeThrowsExceptionAlways()
    catch integer meaning do
	call PrintConsole ( Str_Concat( "The meaning of life is ", Cast_I2S ( meaning ) ) )
    endtry
endfunction