# Notes on converting

## array

* Indent duplicated in PrintArray
* Misc -> NumericCompare 


# core

* AbstractIOServiceProvider -> IOServiceProvider
* remove FileCacheable, FileCacheIF, FileCache, FileFactory
* remove ncml
* consolidate service providers into spi package ???

## IOServiceProvider
* remove release, reacquire

## ncml
* 
## CdmFile
* remove toNcml, writeNcml
* remove release, reacquire

## CdmFiles
* move code to Uncompress class
* remove N3iosp special case getIosp()
* note registerIOProvider("dev.ucdm.core.api.internal.iosp.hdf5.H5iosp");

## RandomAccessFile
* remove acquire
* remove FileCacheable

# s3

* rename to aws ?
* thredds.filesystem.s3.ControllerS3
* thredds.inventory.s3.MFileS3


