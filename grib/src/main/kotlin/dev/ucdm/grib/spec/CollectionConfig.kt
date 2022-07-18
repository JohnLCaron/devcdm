package dev.ucdm.grib.spec

/*
3)<collection spec="/data/ldm/pub/native/grid/NCEP/NAM/Alaska_11km/.*grib2$"
              name="NAM_Alaska_11km"
4)            dateFormatMark="#NAM_Alaska_11km_#yyyyMMdd_HHmm"
5)            timePartition="file"
6)            olderThan="5 min"/>

3) The collection consists of all files ending with "grib2" in the directory "/data/ldm/pub/native/grid/NCEP/NAM/Alaska_11km/".

4) A date will be extracted from the filename, and the files will then be sorted by date. Important if the lexigraphic ordering is different that the date order.

5) Partitioning will happen at the file level.

6) Only include files whose lastModified date is more than 5 minutes old. This is to exclude files that are actively being created.
 */
data class CollectionConfig(
    val name: String,
    val path: String,
    val spec: String,
    val collectionName: String?,
    val dateFormatMark: String,
    val olderThan: String,
    val timePartition: String?,
)


