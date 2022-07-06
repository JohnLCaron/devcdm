package dev.cdm.dataset.coordsysbuild

import dev.cdm.array.ArrayType
import dev.cdm.core.api.Attribute
import dev.cdm.core.constants.AxisType
import dev.cdm.core.constants.CDM
import dev.cdm.core.constants.CF
import dev.cdm.core.constants._Coordinate
import dev.cdm.dataset.api.CdmDataset
import dev.cdm.dataset.cdmdsl.CdmdslDataset
import dev.cdm.dataset.cdmdsl.build
import dev.cdm.dataset.cdmdsl.cdmdsl

open class GiefConventions(name: String = "GIEF") : CoordinatesBuilder(name) {

    override fun augment(orgDataset: CdmDataset): CdmDataset {
        val globalAtts = orgDataset.attributes()

        // coordinate units
        val time_units: String = globalAtts.findAttributeString("time_units", null)
        val level_units: String = globalAtts.findAttributeString("level_units", null)
        val level_name: String = globalAtts.findAttributeString("level_name", null)

        // data variable units
        val unit_name: String = globalAtts.findAttributeString("unit_name", null)
        val parameter_name: String = globalAtts.findAttributeString("parameter_name", null)

        // lat/lon coordinates
        val translation: Attribute? = globalAtts.findAttributeIgnoreCase("translation")
        val affine: Attribute? = globalAtts.findAttributeIgnoreCase("affine_transformation")

        val startLat = translation?.getNumericValue(1)!!.toDouble()
        val incrLat = affine?.getNumericValue(6)!!.toDouble()

        val startLon = translation.getNumericValue(0)!!.toDouble()
        val incrLon = affine.getNumericValue(3)!!.toDouble()

        val cdmdsl: CdmdslDataset = cdmdsl {
            variable("latitude") {
                setType(ArrayType.DOUBLE.name)
                setDimensions("row")
                generateValues(startLat, incrLat)
                attribute(CDM.UNITS).setValue("degrees_north")
                attribute(CDM.LONG_NAME).setValue("latitide coordinate (synthesized)")
                attribute(_Coordinate.AxisType).setValue(AxisType.Lat.name)
            }
            variable("longitude") {
                setType(ArrayType.DOUBLE.name)
                setDimensions("column")
                generateValues(startLon, incrLon)
                attribute(CDM.UNITS).setValue("degrees_east")
                attribute(CDM.LONG_NAME).setValue("longitude coordinate (synthesized)")
                attribute(_Coordinate.AxisType).setValue(AxisType.Lon.name)
            }
            variable("level") {
                attribute(CDM.UNITS).setValue(level_units)
                attribute(CDM.LONG_NAME).setValue(level_name)
                attribute(_Coordinate.AxisType).setValue(AxisType.Height.name)
                attribute(_Coordinate.ZisPositive).setValue(CF.POSITIVE_UP)
            }
            variable("time") {
                attribute(CDM.UNITS).setValue(time_units)
                attribute(CDM.LONG_NAME).setValue("time coordinate")
                attribute(_Coordinate.AxisType).setValue(AxisType.Time.name)
            }

            orgDataset.variables.forEach() { v ->
                if (v.rank > 1) {
                    variable(v.shortName) {
                        attribute(CDM.UNITS).setValue(unit_name)
                        attribute(CDM.LONG_NAME).setValue(v.shortName + " " + parameter_name)
                        attribute(_Coordinate.Axes).setValue("time level latitude longitude")
                    }
                }
            }
        }
        return cdmdsl.build(orgDataset)
    }

}