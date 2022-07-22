package dev.ucdm.grib.inventory

import dev.ucdm.core.calendar.CalendarDate
import java.io.IOException

/** A MCollection consisting of a single file  */
data class SingleFileMCollection(val mfile: MFile) : AbstractMCollection(mfile.shortName), MCollection {

    override fun iterateOverMFiles(visitor: MCollection.Visitor) {
            visitor.visit(mfile)
    }

    override fun getLastModified(): CalendarDate? {
        return CalendarDate.of(mfile.lastModified)
    }

    override fun getRoot(): String {
        return try {
            mfile.parent.path
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }
}