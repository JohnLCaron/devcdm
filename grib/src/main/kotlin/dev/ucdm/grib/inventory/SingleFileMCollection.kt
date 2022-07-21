package dev.ucdm.grib.inventory

import com.google.common.collect.ImmutableList
import dev.ucdm.core.calendar.CalendarDate
import java.io.IOException

/** A MCollection consisting of a single file  */
class SingleFileMCollection(file: MFile) : AbstractMCollection(file.shortName), MCollection {
    private val mfiles: List<MFile> = ImmutableList.of(file)

    override fun iterateOverMFiles(visitor: MCollection.Visitor) {
        for (mfile in mfiles) {
            visitor.visit(mfile)
        }
    }

    override fun getCollectionName(): String {
        return mfiles[0].shortName
    }

    override fun getLastModified(): CalendarDate? {
        return CalendarDate.of(mfiles[0].lastModified)
    }

    override fun getRoot(): String? {
        return try {
            mfiles[0].parent.path
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }
}