# Host dashboard
This dashboard displays various information about the host systems.

## CPU Utilisation
This panel contains information about the CPU utilisation.
A low value for idle could be an indicator for a too high workload.
On the other hand, a high idle value indicates too low workload.

## CPU Load
This panel works like *CPU Utilisation* but is split by cpu cores.
*Load* has to stay most of the time lower than *cores*.

Examples for a single core CPU:
* A CPU Load of 1 on a single core CPU indicates that the CPU is busy all the time.
* A CPU Load of 0.5 on a single core CPU indicates that the CPU has some computational power left to serve further requests.
* A CPU Load of 2 on a single core CPU indicates that the CPU has a too high workload and is not able to complete all incoming requests.

Examples for a dual core CPU:
* A CPU Load of 1 on a dual core CPU indicates that the CPU has some computational power left to serve further requests.
* A CPU Load of 3 on a dual core CPU indicates that the CPU has a too high workload and is not able to complete all incoming requests.
* A CPU Load of 2 on a dual core CPU indicates that the CPU is busy all the time.

Further information can be found here: <http://blog.scoutapp.com/articles/2009/07/31/understanding-load-averages>

## Memory Usage
The memory usage in percent.

## Swap Usage
Swap usage bigger than zero could indicate too little memory.

## Swap Pages per second
Paging in and out per second. Paging is moving specific memory regions of running processes to disk. Swapping is commonly referred to as moving an entire process out to disk.

Further information can be found here: <http://www.logicmonitor.com/blog/2014/07/17/the-right-way-to-monitor-virtual-memory-on-linux/>

## Disk I/O per second
A positive y-value corresponds to read access, a y-value below zero represents write access.

## Disk Usage
The used disk space in percent.

## Disk Queue (max)
This panel displays the queued access to the hard disk drives.
A value staying close to one is an indicator that the disk access speed is a bottle neck.

## Network I/O per second
The read/write network bandwidth per interface. Reading (receiving) data corresponds to a positive y-value, while writing (transferring) data is represented by a negative y-value.

## Network packet errors per second
Count of detected erroneous packets per second.
A high count of detected erroneous packets may indicate a broken network interface or an attack.

## Network packet dropped per second
Count of dropped packets per second.
The reason for dropping packets are operating system / distribution specific.
A high count should be investigated further.
