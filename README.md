# Context

A project that demonstrates concepts around context functions to demonstrate
their utility as well as a launching point to describe the features languages
might want to adopt in order to help make writing reactive frameworks, like 
Jetpack Compose, more natural.

These concepts are demonstrated in the Kotlin programming language but the
concepts presented are not limited to Kotlin.

## Definitions

### Context
Context, in this project, refers to any implicit parameters passed to a 
function. For example, in Kotlin, the context for a method is  the instance of
the method being invoked.

### Context Function
A *context function* is a function that only reads values provided to it in its 
*context* or its parameters and only mutates object, if it mutates at all, 
objects visible through its context or its parameters and only if the 
reference to the object is *unique*.

### Immutable
An immutable object is an object that will not change. This is as opposed to
read only that prevents the holder of the reference from being able to change 
the object.

An object can be considered immutable if none of the changes made to the object
are visible through the reference. This allows, for example, lazy evaluation
of any visible property as long as all reader of the property are assured to
see the same value.

### Pure functions
The term *pure function*, in this project, means specifically a referentially
transparent function meaning that an invocation of the function can be
replaced by just its result (if either known ahead of time or cached from a 
previous invocation) and the result of the program would be identical. That 
is, the function has no side effects (ignoring indirect side effects such as
execution time, memory usage, and power consumption)

### Unique reference
A *unique reference* is the only live reference to an object (live, as for
example, not eligible for collection by a garbage collector).

Specifically, a function is still a *pure function* if it modifies objects that
it receive *unique references* to as this can be replaced by a function returning
copies of the objects that contains the modifications and the *unique references*
then replaced by the copies. If the copying version of the function is pure then 
*unique reference* mutating version is also pure. This transformation can be in 
both directions. That is, if a function is *pure* if its *unique references* are 
replaced by copies contain the changes, then it is *pure* without that 
transformation.

More informally, mutations of an object through a *unique reference* is not
considered a side effect. 

A subset of *unique references* can be proven to be unique using 
[*uniqueness typing*](https://en.wikipedia.org/wiki/Uniqueness_type) and is 
used for similar reasons in
[Clean](https://clean.cs.ru.nl/download/html_report/CleanRep.2.2_11.htm) and
have been added experimentally to 
[Haskell](https://ghc.gitlab.haskell.org/ghc/doc/users_guide/exts/linear_types.html).

As Kotlin does not support *uniqueness typing*, the unique references in this
project are assumed to be unique instead of proven to be unique.

## Significant types

### Context
The `Context<T>` class is a class that can be used either using the 
experimental Kotlin `context` syntax or using extension functions. It allows 
implementing referentially transparent functions for IO (demonstrated by the 
`TextFile` example). It can be used to produce a DSL for a pure builder
(demonstrated by the `html` function that implements a small subset of the
functionality of `kotlinx.html`).

Modifications to the `context` are permitted by a context function if the 
`Context<T>` contains a *unique reference* to the `context` instance. If the
context reference is not *unique* it should be immutable.

### TreeContext
The `TreeContext` allows a DSL syntax to be used to construct and 
update the tree. How to use this is implemented by `Document` which, when
`update()` is called will update the tree to current state of any 
`mutableStateOf()` values read when constructing the tree. 

#### TreeContext and Compose
The `TreeContext` is an implementation of the Jetpack Compose's `Composer` 
that is missing several Compose's performance and safety features (e.g.
the `TreeContext` does not support skipping or restarting, two of the
most important performance features of the `Composer`. It also does not
implement a change list, but, instead, mutates the resulting tree immediately
which is one of Compose's most important safety features). The purpose of this
is not to replace Compose but to demonstrate how a Compose like library can be
written using *pure context functions*. The implementation is intended to be
illustrative, not definitive.

The examples use the snapshot system of Compose as it is a self-contained 
part of the Compose runtime that does not depend on or use the `Composer`.