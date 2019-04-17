
/***
 ** The root language types, globals & native function definitions. It is expected that
 ** all virtual machine implementations support all functions within the root library; 
 ** these globals, types & functions define the universal types for the logic script
 ** reference language implementation.
 **
 **
 **
 **
 ** After compilation, the following shall be created:
 **	root.o	the object linker file
 **	root.lb	the bytecode
 **
 ** If the file shall be treated as a dynamic (globally used) reference library 
 ** which shall not be compiled once into a final file (script-bundle);
 ** after compilation, the following shall be created:
 **	root.ld		the combination of the object linker file  
 **				-and-
 **			the bytecode, 
 **			using the 'logic dynamic library' format
 **
 **
 ** If the symbols required are not included with the logic bundle (and hence
 ** must lie within a 'logic dynamic library' file, the host application must
 ** locate the specific 'logic dynamic library' file shall be loaded to satisfy
 ** the missing symbol(s). 
 ** 
 **/

/** Base type functions: */

/** returns the registered type of the handle object if known; otherwise returns string literal `handle` */
native function		TypeOf		takes 	handle ptr							returns string

/** returns if the type name provided is the type of the handle, or a parent type of the handle's type; if the type of 
 * the handle is not known then only `handle` shall return true. */
native function		InstanceOf	takes	handle ptr, string typename					returns boolean




/** Base types: */

/** Lambda function: wrapper around userfunction (code) function reference; whose function signature is 
 * `takes nothing returns nothing`. */
type 	lambdafunc 		extends 	handle

/** Boolean function: wrapper around userfunction (code) function reference; whose function signature is 
 * `takes nothing returns boolean`. */
type	booleanfunc		extends		handle

/** Coroutine object: pointer to a coroutine (creation handle), reference handle (call Coroutine_Current), etc */
type 	coroutine		extends 	handle


/** Regexp matcher object: pointer to a regular expression matcher instance */
type	regexp_matcher		extends		handle



globals

    real	MATH_PI	    = 3.14159265358979r

endglobals


/* Casting functions: (Integer,Real,Boolean) -> String, String -> (Integer,Real,Boolean) */

native function		Cast_I2S	takes   integer value   						returns string
native function		Cast_R2S	takes   real	value   						returns string
native function		Cast_B2S	takes   boolean value   						returns string

native function		Cast_S2I	takes   string value							returns integer
native function	 	Cast_S2R	takes   string value							returns real
native function	 	Cast_S2B	takes   string value							returns boolean

/* Internal console functions, debug */
native function		PrintConsole	takes   string message  						returns nothing
native function		PrintError	takes   string message  						returns nothing


/* Coroutine functions: */
native function		Coroutine_Create	takes	lambdafunc callable					returns coroutine
native function		Coroutine_Step		takes	lambdafunc callable					returns boolean
native function		Coroutine_InCo		takes	nothing							returns boolean
native function		Coroutine_Current	takes	nothing							returns coroutine

native function 	Coroutine_Stack		takes	coroutine co						returns integer

native function		Coroutine_PushB		takes	coroutine co, boolean val				returns boolean
native function 	Coroutine_PushI		takes	coroutine co, integer val				returns boolean
native function 	Coroutine_PushR		takes	coroutine co, real val					returns boolean
native function 	Coroutine_PushS		takes	coroutine co, string val				returns boolean

native function		Coroutine_PopB		takes	coroutine co						returns boolean
native function 	Coroutine_PopI		takes	coroutine co						returns integer
native function		Coroutine_PopR		takes	coroutine co						returns real
native function 	Coroutine_PopS		takes	coroutine co						returns string

/* Macro coroutine function shorthands: */
function 			Coroutine_SPushB 	takes	boolean val					returns boolean
	local coroutine selfCo = Coroutine_Current()
	return Coroutine_PushB ( selfCo, val )
endfunction

function 			Coroutine_SPushI 	takes	integer val					returns boolean
	local coroutine selfCo = Coroutine_Current()
	return Coroutine_PushI ( selfCo, val )
endfunction

function 			Coroutine_SPushR 	takes	real val					returns boolean
	local coroutine selfCo = Coroutine_Current()
	return Coroutine_PushR ( selfCo, val )
endfunction

function 			Coroutine_SPushS 	takes	string val					returns boolean
	local coroutine selfCo = Coroutine_Current()
	return Coroutine_PushS ( selfCo, val )
endfunction


/** String functions: */

/** Str_IndexOf: returns index of char or string m in string s, starting at offset o;
 * where o is negative, start at end of string and proceed in reverse order. */
native function		Str_IndexOf		takes string s, string m, integer o				returns integer

/** Str_SubStr: return substring of string s starting at offset o to integer l; or where l 
 * is zero return the remaining length of the string; or where l is negative, return the reverse
 * from offset o backwards to offset l. */
native function		Str_SubStr		takes string s, integer o, integer l				returns string

/** Str_Split: split string s by char or string token t, consuming token t in split;
 * returns array string[] split elements */
native function		Str_Split		takes string s, string t					returns string

/** Str_Equals: compare strings a and b, i indicating case-insensitive. */
native function		Str_Equals		takes string a, string b, boolean i				returns boolean

/** Str_Concat: concatenate strings a and b to new string */
native function 	Str_Concat		takes string a, string b					returns string


/** Math functions: */

/** Math_Deg2Rad: degrees to radians, real */
function		Math_Deg2Rad		    takes	real deg					returns real
    return ( MATH_PI / 180.0r ) * deg
endfunction

/** Math_Rad2Deg: radians to degrees, real */
function		Math_Rad2Deg		    takes	real rad					returns real
    return ( 180.0r / MATH_PI ) * rad
endfunction

/** Math_IMax: max of integers (a, b) */
function		Math_IMax		    takes	integer a, integer b				returns integer
    if a > b then return a else return b endif
endfunction

/** Math_RMax: max of reals (a, b) */
function		Math_RMax		    takes	real a, real b					returns real
    if a > b then return a else return b endif
endfunction

/** Math_IMin: min of integers (a, b) */
function		Math_IMin		    takes	integer a, integer b				returns integer
    if a > b then return b else return a endif
endfunction

/** Math_RMin: min of reals (a, b) */
function		Math_RMin		    takes	real a, real b					returns real
    if a > b then return b else return a endif
endfunction

/** regexp functions: */

/** StrRegExp_CreateMatcher: create a pattern matcher based on the regexp pattern provided;
 * throws an exception if the pattern is invalid. */
native function		StrRegExp_CreateMatcher		takes 	string pattern					returns regexp_matcher

/** StrRegExp_Update: update the matcher handle to match against the input string s; the
 * matcher shall be placed at the first full match, the return shall indicate whether there
 * is at least one match (MATCH_ANY). */
native function		StrRegExp_Update			takes	regexp_matcher m, string s		returns boolean

/** StrRegExp_Matches: return if the current match pointer position in the string is valid;
 * after _Update this shall indicate if the pattern matches; if _NextMatch or _PrevMatch is
 * used then _Matches shall indicate if the pointer position is a legal one. */
native function		StrRegExp_Matches			takes	regexp_matcher m			returns boolean

/** StrRegExp_MatchNumGroups: return the number of matcher pattern groups which are capturing
 * in the current match pointer index. If the pointer index is invalid, -1 is returned. */
native function		StrRegExp_MatchNumGroups	takes	regexp_matcher m				returns integer

/** StrRegExp_MatchGroup: get the string value of the capturing pattern group at index i in the
 * current match pointer index. If the pointer index is invalid, an exception will be thrown. */
native function		StrRegExp_MatchGroup		takes	regexp_matcher m, integer i			returns string

/** StrRegExp_MatchString: get the entire matching string sub-string at the current match pointer
 * index. If the pointer index is invalid, an exception will be thrown. */
native function		StrRegExp_MatchString		takes	regexp_matcher m				returns string

/** StrRegExp_NextMatch: move to the next full match in the matched string; returns if the match
 * pointer is now at a valid match or not. */
native function		StrRegExp_NextMatch		takes 	regexp_matcher m				returns boolean

/** StrRegExp_PrevMatch: move to the previous full match in the matched string; returns if the
 * match pointer is now at a valid match or not. */
native function		StrRegExp_PrevMatch		takes	regexp_matcher m				returns boolean

/** StrRegExp_NumMatches: return the number of full matches in the matched string; if the string does
 * not matched returns 0, otherwise, returns the full number of full matches (1...n) in the target 
 * string value. */
native function		StrRegExp_NumMatches		takes	regexp_matcher m				returns integer



/** The root types & type-support library: */
exports
    type	lambdafunc, booleanfunc

    function
	    TypeOf,		/* obj_gettype ( ptr ) */
	    InstanceOf,		/* obj_instanceof ( ptr, type ) */
	    PrintConsole,	/* print_console ( str ) */
	    PrintError		/* print_error ( str ) */
    as		root.types
    endexports

/** usage: imports function TypeOf, InstanceOf from root.types endimports */


/** The root math library: */
exports
    global	MATH_PI
    function
	    Math_Rad2Deg, Math_Deg2Rad,
	    Math_IMax, Math_RMax, Math_IMin, Math_RMin
    as		root.math
    endexports
    


/** The value-casting library: */
exports
    function 
	    Cast_I2S, 		/** val_tostringi ( v ) */
	    Cast_R2S, 		/** val_tostringr ( v ) */
	    Cast_B2S,		/** val_tostringb ( v ) */

	    Cast_S2I, 		/** val_parsestri ( v ) */
	    Cast_S2R, 		/** val_parsestrr ( v ) */
	    Cast_S2B		/** val_parsestrb ( v ) */
    as		root.casting
    endexports

/** usage_tostr: imports function Cast_I2S, Cast_R2S, Cast_B2S from root.casting endimports */
/** usage_frstr: imports function Cast_S2I, Cast_S2R, Cast_S2B from root.casting endimports */
/** usage_casting: imports function Cast_I2S, Cast_R2S, Cast_B2S, Cast_S2I, Cast_S2R, Cast_S2B from root.casting endimports */


/** The coroutine library: */
exports
    type	coroutine			/** the global typename */

    function 
	    Coroutine_Create,			/** co_create: 		usercode    -> coroutine handle */
	    Coroutine_Step,			/** co_step: 		coroutine   -> step into -> return error */
	    Coroutine_InCo,			/** co_in_co:			    -> in coroutine? */
	    Coroutine_Current,			/** co_get_co:			    -> get current coroutine handle */
	    Coroutine_Stack			/** go_get_stacksz:		    -> get number of obj on co_stack */

    function
	    Coroutine_PushB,			/** co_stack_pushb: push boolean 	=> co_stack */
	    Coroutine_PushI,			/** co_stack_pushi: push integer 	=> co_stack */
	    Coroutine_PushR,			/** co_stack_pushr: push real 		=> co_stack */
	    Coroutine_PushS			/** co_stack_pushs: push string 	=> co_stack */

    function
	    Coroutine_PopB,			/** co_stack_popb:  -> pop o => checktype(o) == bool -> ret b */
	    Coroutine_PopI,			/** co_stack_popi:  -> pop o => checktype(o) == int -> ret i */
	    Coroutine_PopR,			/** co_stack_popr:  -> pop o => checktype(o) == real -> ret r */
	    Coroutine_PopS			/** co_stack_pops:  -> pop o => checktype(o) == string -> ret r */

    as		root.coroutine
    endexports

/** usage: imports function Coroutine_Create, Coroutine_Step, Coroutine_InCo, Coroutine_Current, Coroutine_Stack from root.coroutine endimports */
/** stack_u: imports function Coroutine_PushB, Coroutine_PushI, Coroutine_PushR, Coroutine_PushS from root.coroutine endimports */
/** stack_o: imports function Coroutine_PopB, Coroutine_PopI, Coroutine_PopR, Coroutine_PopS from root.coroutine endimports */

/** The coroutine.self library: */
exports
    function 
	    Coroutine_SPushB,			/** ufunc_macro of:	co_stack_pushb ( co_get_co(), v ) */
	    Coroutine_SPushI,			/** ufunc_macro of:	co_stack_pushi ( co_get_co(), v ) */
	    Coroutine_SPushR,			/** ufunc_macro of:	co_stack_pushr ( co_get_co(), v ) */
	    Coroutine_SPushS			/** ufunc_macro of:	co_stack_pushs ( co_get_co(), v ) */

    as root.coroutine.self
    endexports

/** stack_u.self: imports function Coroutine_SPushB, Coroutine_SPushI, Coroutine_SPushR, Coroutine_SPushS from root.coroutine.self endimports */

/** The strings library: */
exports
	
    function
	    Str_IndexOf,
	    Str_SubStr,
	    Str_Split,
	    Str_Equals,
	    Str_Concat

    as		root.strings
    endexports

/** usage: imports function Str_IndexOf, Str_SubStr, Str_Split, Str_Equals, Str_Concat from root.strings endimports */

/** The regexp library: */
exports
	
	type		regexp_matcher
    function
	    StrRegExp_CreateMatcher,		/* regexp_create_matcher:	string				-> regexp_matcher */
	    StrRegExp_FreeMatcher,		/* regexp_dealloc_matcher:	regexp_matcher			-> nothing */
	    StrRegExp_Update,			/* regexp_matcher_apply:	regexp_matcher, string 		-> boolean */

	    StrRegExp_Matches,			/* regexp_matcher_matches:	regexp_matcher			-> boolean */

	    StrRegExp_MatchNumGroups,		/* regexp_matcher_groupsz:	regexp_matcher			-> integer */
	    StrRegExp_MatchGroup,		/* regexp_matcher_group:	regexp_matcher, integer		-> string */
	    StrRegExp_MatchString,		/* regexp_matcher_string:	regexp_matcher			-> string */

	    StrRegExp_NextMatch,		/* regexp_matcher_next:		regexp_matcher			-> boolean */
	    StrRegExp_PrevMatch,		/* regexp_matcher_prev:		regexp_matcher			-> boolean */

	    StrRegExp_NumMatches		/* regexp_matcher_count:	regexp_matcher			-> integer */
    as		root.string.regexp
    endexports

/** usage: imports function StrRegExp_CreateMatcher, StrRegExp_FreeMatcher, StrRegExp_Update, StrRegExp_Matches, StrRegExp_MatchNumGroups,
StrRegExp_MatchGroups, StrRegExp_MatchString, StrRegExp_NextMatch, StrRegExp_PrevMatch, StrRegExp_NumMatches from root.string.regexp endimports */