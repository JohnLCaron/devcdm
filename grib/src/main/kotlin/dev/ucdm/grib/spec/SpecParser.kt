package dev.ucdm.grib.spec

import java.io.File
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.PathMatcher
import java.util.*
import java.util.regex.Matcher
import java.util.regex.Pattern


/**
 * Parses the collection specification string.
 * The idea is that one copies the full path of an example dataset, then edits it.
 *
 * Example: "/data/ldm/pub/native/grid/NCEP/GFS/Alaska_191km/ ** /GFS_Alaska_191km_#yyyyMMdd_HHmm#\.grib1$"
 *   rootDir ="/data/ldm/pub/native/grid/NCEP/GFS/Alaska_191km"/
 *   subdirs=true (because ** is present)
 *   dateFormatMark="GFS_Alaska_191km_#yyyyMMdd_HHmm"
 *   regExp='GFS_Alaska_191km_.............\.grib1$
 *
 * Example: "Q:/grid/grib/grib1/data/agg/.*\.grb"
 *   rootDir ="Q:/grid/grib/grib1/data/agg/"/
 *   subdirs=false
 *   dateFormatMark=null
 *   useName=yes
 *   regexp= ".*\.grb" (anything ending with .grb)
 */
data class SpecParser(
    val spec: String,
    val rootDir: String,
    val subdirs : Boolean ,// recurse into subdirectories under the root dir
    val filterOnName : Boolean, // filter on name, else on entire path
    val regexp : Pattern?, // regexp filter
    val dateFormatMark: String?
)  {
    // experimental
    val pathMatcher: PathMatcher
        get() = if (spec.startsWith("regex:") || spec.startsWith("glob:")) { // experimental
            FileSystems.getDefault().getPathMatcher(spec)
        } else {
            BySpecp()
        }

    private inner class BySpecp : PathMatcher {
        override fun matches(path: Path): Boolean {
            val matcher: Matcher = regexp!!.matcher(path.fileName.toString())
            return matcher.matches()
        }
    }
}

/**
 * Single spec : "/topdir/**/#dateFormatMark#regExp"
 * This only allows the dateFormatMark to be in the file name, not anywhere else in the filename path,
 * and you cant use any part of the dateFormat to filter on.
 *
 * @param collectionSpec the collection Spec
 * @param errlog put error messages here, may be null
 */
fun createSpecParser(collectionSpec: String, errlog: Formatter?) : SpecParser {
    val spec = collectionSpec.trim { it <= ' ' }
    val posFilter: Int
    var rootDir : String
    var subdirs : Boolean?

    val posGlob = collectionSpec.indexOf("/**/")
    if (posGlob > 0) {
        rootDir = collectionSpec.substring(0, posGlob)
        posFilter = posGlob + 3
        subdirs = true
    } else {
        subdirs = false
        posFilter = collectionSpec.lastIndexOf('/')
        rootDir = if (posFilter > 0) collectionSpec.substring(0, posFilter) else System.getProperty("user.dir") // working directory
    }

    val locFile = File(rootDir)
    if (!locFile.exists() && errlog != null) {
        errlog.format(" Directory %s does not exist %n", rootDir)
    }

    // optional filter
    var regexp: Pattern? = null
    var filterSpec: String? = null
    var dateFormatMark: String? = null
    if (posFilter < collectionSpec.length - 2) filterSpec = collectionSpec.substring(posFilter + 1) // remove topDir
    if (filterSpec != null) {
        // optional dateFormatMark
        val posFormat = filterSpec.indexOf('#')
        if (posFormat >= 0) {
            // check for two hash marks
            val posFormat2 = filterSpec.lastIndexOf('#')
            if (posFormat != posFormat2) { // two hash
                dateFormatMark = filterSpec.substring(0, posFormat2) // everything up to the second hash
                filterSpec = filterSpec.replace("#", "") // remove hashes, replace with .
                val sb = StringBuilder(filterSpec)
                for (i in posFormat until posFormat2 - 1) sb.setCharAt(i, '.')
                val regExp = sb.toString()
                regexp = Pattern.compile(regExp)
            } else { // one hash
                dateFormatMark = filterSpec // everything
                val regExp = filterSpec.substring(0, posFormat) + "*"
                regexp = Pattern.compile(regExp)
            }
        } else { // no hash (dateFormatMark)
            dateFormatMark = null
            regexp = Pattern.compile(filterSpec)
        }
    } else {
        dateFormatMark = null
        regexp = null
    }
    val filterOnName = true

    return SpecParser(spec, rootDir, subdirs, filterOnName , regexp, dateFormatMark)
}