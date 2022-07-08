package dev.cdm.dataset.coordsysbuild

import dev.cdm.array.ArrayType
import dev.cdm.core.api.Attribute
import dev.cdm.core.api.CdmFile
import dev.cdm.core.constants.AxisType
import dev.cdm.core.constants.CDM
import dev.cdm.core.constants.CF
import dev.cdm.core.constants._Coordinate
import dev.cdm.dataset.api.CdmDataset
import dev.cdm.dataset.api.VariableDS
import dev.cdm.dataset.cdmdsl.CdmdslDataset
import dev.cdm.dataset.cdmdsl.build
import dev.cdm.dataset.cdmdsl.cdmdsl

open class CedricRadarConventions(name: String = "CEDRICRadar") : CFConventions(name) {

    override fun augment(orgDataset: CdmDataset): CdmDataset {
        var lat = orgDataset.findVariable("sensor_latitude")
        if (lat == null) {
            lat = orgDataset.findVariable("radar_latitude")
        }
        var lon = orgDataset.findVariable("sensor_longitude")
        if (lon == null) {
            lon = orgDataset.findVariable("radar_longitude")
        }
        val latv = lat!!.readScalarFloat().toDouble()
        val lonv = lon!!.readScalarFloat().toDouble()

        val cdmdsl: CdmdslDataset = cdmdsl {
            attribute( "Conventions").setValue("CEDRICRadar")
            attribute( "history").setValue("Generated from CHILL file; Modified by NcML to be CF compliant")

            dimension("time", 1)
            variable("time") {
                setDimensions("time")
                setType(ArrayType.DOUBLE.name)
                attribute(CDM.UNITS).setValue("seconds since 1970-01-01 00:00:00")
                attribute(_Coordinate.AxisType).setValue(AxisType.Time.name)
                attribute("process").setValue("CDM CedricRadarConventions: Added for CF compliance")
                setValues(0.0)
            }

            variable("y") {
                attribute(CDM.UNITS).setValue("km")
                attribute(_Coordinate.AxisType).setValue(AxisType.GeoY.name)
            }
            variable("x") {
                attribute(CDM.UNITS).setValue("km")
                attribute(_Coordinate.AxisType).setValue(AxisType.GeoX.name)
            }
            variable("z") {
                attribute(CDM.UNITS).setValue("km")
                attribute(_Coordinate.AxisType).setValue(AxisType.GeoZ.name)
            }

            variable("VREL") {
                attribute(CF.GRID_MAPPING).setValue("FlatEarth")
                attribute(_Coordinate.Axes).setValue("time z y x")
            }

            variable("ProjectionCTV") {
                attribute(CF.GRID_MAPPING_NAME).setValue(CDM.FlatEarth)
                attribute(CF.LONGITUDE_OF_PROJECTION_ORIGIN).setValue(lonv)
                attribute(CF.LATITUDE_OF_PROJECTION_ORIGIN).setValue(latv)
                attribute(_Coordinate.TransformType).setValue("Projection")
                attribute(_Coordinate.AxisTypes).setValue("GeoX GeoY")
            }
        }
        return cdmdsl.build(orgDataset)
    }

}

fun isCedricRadarConvention(ncfile: CdmFile): Boolean {
    val s = ncfile.findDimension("cedric_general_scaling_factor")
    val v = ncfile.findVariable("cedric_run_date")
    return v != null && s != null
}