package dev.cdm.core.iosp;

import dev.cdm.array.ArrayType;
import dev.cdm.core.netcdf3.NetcdfFormatUtils;

// static utilities accessible to other modules
public class IospUtils {
  public static final double NC_FILL_DOUBLE = NetcdfFormatUtils.NC_FILL_DOUBLE;
  public static final String HDFEOS_CRS = dev.cdm.core.hdf4.HdfEos.HDFEOS_CRS;
  public static final String HDFEOS_CRS_Projection = dev.cdm.core.hdf4.HdfEos.HDFEOS_CRS_Projection;
  public static final String HDFEOS_CRS_UpperLeft = dev.cdm.core.hdf4.HdfEos.HDFEOS_CRS_UpperLeft;
  public static final String HDFEOS_CRS_LowerRight = dev.cdm.core.hdf4.HdfEos.HDFEOS_CRS_LowerRight;
  public static final String HDFEOS_CRS_ProjParams = dev.cdm.core.hdf4.HdfEos.HDFEOS_CRS_ProjParams;

  public static Number getFillValueDefault(ArrayType dtype) {
    return NetcdfFormatUtils.getFillValueDefault(dtype);
  }
}
