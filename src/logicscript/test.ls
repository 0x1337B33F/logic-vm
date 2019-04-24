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


globals
    
    array integer   TestIntegerArray000 [ 16 ]
    array integer   TestIntegerArray001	[ 8 ]
    array real	    TestRealArray000	[ 8 ]

endglobals


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


/* Test global arrays: */

function	TestSetGlobalArrayIntVal	takes nothing returns nothing
    set	    TestIntegerArray000[ 0 ] = 1
    set	    TestIntegerArray000[ 1 ] = 1
    
    for i in 2 to 16 by 1 do
	set	TestIntegerArray000[ i ] = TestIntegerArray000[ i - 1 ] + TestIntegerArray000 [ i - 2 ]
    endfor
    
endfunction

function	TestGetGlobalArrayIntVal	takes nothing returns nothing
    for i in 0 to 16 by 1 do
	call PrintConsole ( Str_Concat ( Cast_I2S ( i ), Str_Concat( ": ", Cast_I2S ( TestIntegerArray000 [ i ] ) ) ) )
    endfor
endfunction


/* Test yield support: */

function	    TestFunctionYieldDirect	takes nothing returns nothing
    for i in 0 to 5 by 1 do
	yield i
    endfor
endfunction


/* Test coroutines: */

function	CoFuncReturnsValue	    takes nothing returns string
    return "invoke from coroutine wrapper"
endfunction

function	CoFuncYieldsValues	    takes nothing returns nothing
    yield "correct"
    yield "battery"
    yield "horse"
    yield "staple"
endfunction

function	CoFuncReturnsYieldsValues   takes nothing returns nothing
    yield "apple"
    yield "orange"
    yield "tomato"
    return "fruits?"
endfunction

function	CoFuncRaisesError	    takes nothing returns nothing
    raise "raised error from coroutine"
endfunction

imports
	type coroutine
	function Coroutine_Create, Coroutine_IsComplete, Coroutine_GetSResult, Coroutine_IsError, Coroutine_ThrowError, Coroutine_GetError
    from root.coroutine
endimports

function	TestCoFuncCaptureError	    takes nothing returns nothing
    local coroutine co = Coroutine_Create ( function CoFuncRaisesError )
    
    loop
	exitwhen Coroutine_IsComplete ( co )
	
	call Coroutine_Step ( co )
    endloop

    if Coroutine_IsError ( co ) then
	call PrintConsole ( Str_Concat( "Coroutine error: ", Coroutine_GetError ( co ) ) )
    endif
endfunction


endfunction

function	TestCoFuncCall		    takes nothing returns nothing
    local coroutine co0 = Coroutine_Create ( function CoFuncReturnsYieldsValues )
    local coroutine co1 = Coroutine_Create ( function CoFuncYieldsValues )

    local string yielded

    call PrintConsole ( "async start ")

    loop
	exitwhen Coroutine_IsComplete ( co0 ) and Coroutine_IsComplete ( co1 )
	
	if not Coroutine_IsComplete ( co0 ) then  call Coroutine_Step ( co0 ) endif
	if not Coroutine_IsComplete ( co1 ) then  call Coroutine_Step ( co1 ) endif
	
	if not Coroutine_IsComplete ( co0 ) then
	    set yielded = Coroutine_GetSResult ( co0 )
	    call PrintConsole ( Str_Concat ( "Yield 0: ", yielded ) )
	else
	    set yielded = Coroutine_GetSResult ( co0 )
	    call PrintConsole ( Str_Concat ( "Complete 0: ", yielded ) )
	endif

	if not Coroutine_IsComplete ( co1 ) then
	    set yielded = Coroutine_GetSResult ( co1 )
	    call PrintConsole ( Str_Concat ( "Yield 1: ", yielded ) )
	endif

    endloop

    call PrintConsole ( "async done" )

endfunction


/* Test type safety: */

function	TestHandleTypeCasting	    takes nothing returns nothing
    local coroutine co
    local handle ptr
    local coroutine other

    set co = Coroutine_Create ( function CoFuncReturnsValue )
    set ptr = co
    set other = ptr
    
    call PrintConsole ( Str_Concat ( "The type of ptr after assignment is ", TypeOf ( ptr ) ) )
    call PrintConsole ( Str_Concat ( "ptr instanceof coroutine? ", Cast_B2S ( InstanceOf ( ptr, "coroutine" ) ) ) )
    call PrintConsole ( Str_Concat ( "ptr instanceof list? ", Cast_B2S ( InstanceOf ( ptr, "list" ) ) ) )
    call PrintConsole ( Str_Concat ( "The type of other after assignment is ", TypeOf ( other ) ) )
endfunction

