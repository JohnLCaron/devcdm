package dev.cdm.core.iosp;

import dev.cdm.array.ArrayType;
import dev.cdm.core.netcdf3.NetcdfFormatUtils;

/** Static utilities accessible to other modules */
public class IospUtils {
  public static final double NC_FILL_DOUBLE = NetcdfFormatUtils.NC_FILL_DOUBLE;
  public static final long NC_FILL_INT64 = NetcdfFormatUtils.NC_FILL_INT64;
  public static final String NETCDF4_DIMID = NetcdfFormatUtils.NETCDF4_DIMID;
  public static final String NETCDF4_COORDINATES = NetcdfFormatUtils.NETCDF4_COORDINATES;
  public static final String NETCDF4_STRICT = NetcdfFormatUtils.NETCDF4_STRICT;

  public static final String HDF5_DIMENSION_LIST = dev.cdm.core.hdf5.H5header.HDF5_DIMENSION_LIST;
  public static final String HDF5_DIMENSION_SCALE = dev.cdm.core.hdf5.H5header.HDF5_DIMENSION_SCALE;
  public static final String HDF5_DIMENSION_LABELS = dev.cdm.core.hdf5.H5header.HDF5_DIMENSION_LABELS;

  public static final String HDFEOS_CRS = dev.cdm.core.hdf4.HdfEos.HDFEOS_CRS;
  public static final String HDFEOS_CRS_Projection = dev.cdm.core.hdf4.HdfEos.HDFEOS_CRS_Projection;
  public static final String HDFEOS_CRS_UpperLeft = dev.cdm.core.hdf4.HdfEos.HDFEOS_CRS_UpperLeft;
  public static final String HDFEOS_CRS_LowerRight = dev.cdm.core.hdf4.HdfEos.HDFEOS_CRS_LowerRight;
  public static final String HDFEOS_CRS_ProjParams = dev.cdm.core.hdf4.HdfEos.HDFEOS_CRS_ProjParams;

  public static Number getFillValueDefault(ArrayType dtype) {
    return NetcdfFormatUtils.getFillValueDefault(dtype);
  }
}
