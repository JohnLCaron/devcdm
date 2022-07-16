package dev.ucdm.dataset.coordsysbuild

import dev.ucdm.core.api.CdmFile
import dev.ucdm.core.constants.AxisType
import dev.ucdm.core.constants.CDM
import dev.ucdm.core.constants.CF
import dev.ucdm.core.constants._Coordinate
import dev.ucdm.dataset.api.CdmDataset
import dev.ucdm.dataset.cdmdsl.CdmdslDataset
import dev.ucdm.dataset.cdmdsl.build
import dev.ucdm.dataset.cdmdsl.cdmdsl

open class ZebraConventions(name: String = "Zebra") : DefaultConventions(name) {

    override fun augment(orgDataset: CdmDataset): CdmDataset {
        val unitsFromBase = orgDataset.findAttribute("base_time@units")

        val cdmdsl: CdmdslDataset = cdmdsl {
            attribute(CF.CONVENTIONS).setValue("Zebra")

            variable("latitude") {
                attribute(_Coordinate.AxisType).setValue(AxisType.Lat.name)
            }
            variable("longitude") {
                attribute(_Coordinate.AxisType).setValue(AxisType.Lon.name)
            }
            variable("altitude") {
                attribute(_Coordinate.AxisType).setValue(AxisType.Height.name)
                attribute(_Coordinate.ZisPositive).setValue(CF.POSITIVE_UP)
            }
            variable("time_offset") {
                if (unitsFromBase != null && unitsFromBase.isString) {
                    unitsFromBase.stringValue?.let { attribute(CDM.UNITS).setValue(it) }
                }
                attribute("process").setValue("CDM ZebraConventions = base_time + time_offset")
                attribute(_Coordinate.AxisType).setValue(AxisType.Time.name)
                attribute(_Coordinate.AliasForDimension).setValue("time")
            }
        }
        return cdmdsl.build(orgDataset)
    }

}

fun isZebraConvention(ncfile: CdmFile): Boolean {
    val s = ncfile.rootGroup.findAttributeString("Convention", "none")
    return s.startsWith("Zebra")
}