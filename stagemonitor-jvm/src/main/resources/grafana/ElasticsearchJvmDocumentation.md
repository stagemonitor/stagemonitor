# JVM dashboard
This dashboard contains information about memory usage and garbage collection metrics of the JVM.

## Heap Usage and Non-Heap Usage
Both heap and non heap usage have three metrics with the same meaning. The following documentation is taken from <https://docs.oracle.com/javase/7/docs/api/java/lang/management/MemoryUsage.html>

* *committed*:
represents the amount of memory (in bytes) that is guaranteed to be available for use by the Java virtual machine. The amount of committed memory may change over time (increase or decrease). The Java virtual machine may release memory to the system and committed could be less than init. committed will always be greater than or equal to used.

* *used*:
represents the amount of memory currently used (in bytes).

* *max*:
represents the maximum amount of memory (in bytes) that can be used for memory management. Its value may be undefined. The maximum amount of memory may change over time if defined. The amount of used and committed memory will always be less than or equal to max if max is defined. A memory allocation may fail if it attempts to increase the used memory such that used > committed even if used <= max would still be true (for example, when the system is low on virtual memory).

## Heap Usage
Memory used by the JVM to store runtime data (objects).

## Non-Heap Usage
Memory used by the JVM to store loaded classes and other meta-data.

## GC (Garbage Collector)
The garbage collector reclaims memory occupied by objects that are no longer referenced.
Collector types are explained here:
<http://jvmmemory.com/>

For more in depth information consult the official documentation:
<https://docs.oracle.com/javase/8/docs/technotes/guides/vm/gctuning/toc.html>

## Memory pools

Heap:
* *Eden Space*: The pool from which memory is initially allocated for most objects.
* *Survivor Space*: The pool containing objects that have survived the garbage collection of the Eden space.
* *Old Generation*: The pool containing objects that have existed for some time in the survivor space.

Non Heap:
* *Code Cache*: The HotSpot Java VM also includes a code cache, containing memory that is used for compilation and storage of native code.
* *Compressed class space*: Class information referred to by the objects in the JavaHeap
* *Metaspace*: Method and other information excluding the above information

