/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package dev.ucdm.grib.grib2.record;

/**
 * Read and Write Grib2 index (gbx9).
 * Hides GribIndexProto
 *
 * sample use:
 * 
 * <pre>
 * GribIndex index = new GribIndex();
 * if (!index.readIndex(path))
 *   index.makeIndex(path);
 * 
 * for (Grib2SectionGridDefinition gds : index.getGds()) {
 *   if (gdsSet.get(gds.calcCRC()) == null)
 *     gdsSet.put(gds.calcCRC(), gds);
 * }
 * 
 * for (Grib2Record gr : index.getRecords()) {
 *   gr.setFile(fileno);
 * 
 *   Grib2Pds pds = gr.getPDSsection().getPDS();
 *   int discipline = gr.getDiscipline();
 * 
 *   int id = gr.cdmVariableHash();
 *   Grib2ParameterBean bean = pdsSet.get(id);
 *   if (bean == null) {
 *     bean = new Grib2ParameterBean(gr);
 *     pdsSet.put(id, bean);
 *     params.add(bean);
 *   }
 *   bean.addRecord(gr);
 * }
 * </pre>
 *
 * @author caron
 * @since 4/1/11
 */
public class Grib2Index {
  public static final int ScanModeMissing = 9999;
}
