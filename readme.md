# server-logicvm

demonstration of a fully-compiled, dynamically linked language with both coroutine support
and cross-boundary language calls implemented in pure JVM. includes the tokenizer (JavaCC), 
the reference AST structure the tokenizer unfolds, the assembler, the shortcuts, the final 
function assembler, and, a reference implementation for a virtual machine which can fully 
interpret the bytecode and dynamically link needed symbols on the fly.

## license

this code isn't open-source; you don't have permission to copy it verbatim or in any part, 
you don't have permission to use it commercially or do what you want with it. rather, it's 
designed as a example of how you might design a virtual machine and a language, and some 
(but not all) of the shortcuts, cheats, and other decisions you can use to help you build 
a machine which can execute the instructions given. it is also not a reference guide on 
how to do things: this code was originally written nearly ten years ago, and there are 
things that could be done better.

## the usual disclaimers

I reserve all rights to do what I want with the original source, language, the VM and 
anything else. you can't directly copy anything either verbatim or otherwise -- you should 
look at the code, review it and make sensible decisions on how to build compilers and 
virtual (or physical!) machines.

this work is offered as a portfolio piece of some age, and is therefore "as is" -- should 
it set your house on fire, kick puppies, cause damage to your computer, invoke chaos, or 
summon Satan himself, it is your problem to deal with.