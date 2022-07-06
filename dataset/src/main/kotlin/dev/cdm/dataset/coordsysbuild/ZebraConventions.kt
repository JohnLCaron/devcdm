package dev.cdm.dataset.coordsysbuild

import dev.cdm.core.api.CdmFile
import dev.cdm.core.constants.AxisType
import dev.cdm.core.constants.CDM
import dev.cdm.core.constants.CF
import dev.cdm.core.constants._Coordinate
import dev.cdm.dataset.api.CdmDataset
import dev.cdm.dataset.cdmdsl.CdmdslDataset
import dev.cdm.dataset.cdmdsl.build
import dev.cdm.dataset.cdmdsl.cdmdsl

open class ZebraConventions(name: String = "Zebra") : DefaultConventions(name) {

    override fun augment(orgDataset: CdmDataset): CdmDataset {
        val unitsFromBase = orgDataset.findAttributeFromFullName("base_time@units")

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