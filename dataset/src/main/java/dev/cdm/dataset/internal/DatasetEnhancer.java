package dev.cdm.dataset.internal;

import dev.cdm.array.ArrayType;
import dev.cdm.core.api.Group;
import dev.cdm.core.api.Variable;
import dev.cdm.dataset.api.CdmDataset;
import dev.cdm.dataset.api.CdmDataset.Enhance;
import dev.cdm.dataset.api.SequenceDS;
import dev.cdm.dataset.api.StructureDS;
import dev.cdm.dataset.api.VariableDS;

import java.io.IOException;
import java.util.EnumSet;
import java.util.Set;

/** Helper class to enhance NetcdfDataset with scale/offset/missing */
public class DatasetEnhancer {

  public static boolean enhanceNeeded(Set<Enhance> want, Set<Enhance> have) {
    if (want == null)
      return false;
    if (have == null && !want.isEmpty())
      return true;
    for (Enhance mode : want) {
      if (!have.contains(mode))
        return true;
    }
    return false;
  }

  private final CdmDataset.Builder<?> dsBuilder;
  protected final Set<Enhance> wantEnhance;

  public DatasetEnhancer(CdmDataset.Builder<?> ds, Set<Enhance> wantEnhance) {
    this.dsBuilder = ds;
    this.wantEnhance = wantEnhance == null ? EnumSet.noneOf(Enhance.class) : wantEnhance;
  }

  /*
   * Enhancement use cases
   * 1. open NetcdfDataset(enhance).
   * 2. NcML - must create the NetcdfDataset, and enhance when its done.
   *
   * Enhance mode is set when
   * 1) the NetcdfDataset is opened
   * 2) enhance(EnumSet<Enhance> mode) is called.
   *
   * Possible remove all direct access to Variable.enhance
   */

  public CdmDataset.Builder<?> enhance() throws IOException {
    enhanceGroup(dsBuilder.rootGroup);
    dsBuilder.addEnhanceModes(wantEnhance);
    return dsBuilder;
  }

  private void enhanceGroup(Group.Builder group) {
    for (Variable.Builder<?> vb : group.vbuilders) {
      if (vb instanceof StructureDS.Builder<?>) {
        enhanceStructure((StructureDS.Builder<?>) vb, wantEnhance);
      } else if (vb instanceof SequenceDS.Builder<?>) {
        enhanceSequence((SequenceDS.Builder<?>) vb, wantEnhance);
      } else if (vb instanceof VariableDS.Builder) {
        enhanceVariable((VariableDS.Builder<?>) vb, wantEnhance);
      } else {
        throw new IllegalStateException("Not a VariableDS " + vb);
      }
    }

    for (Group.Builder gb : group.gbuilders) {
      enhanceGroup(gb);
    }
  }

  public static void enhanceStructure(StructureDS.Builder<?> sdb, Set<Enhance> wantEnhance) {
    for (Variable.Builder<?> vb : sdb.vbuilders) {
      if (vb instanceof StructureDS.Builder) {
        enhanceStructure((StructureDS.Builder<?>) vb, wantEnhance);
      } else if (vb instanceof SequenceDS.Builder) {
        enhanceSequence((SequenceDS.Builder<?>) vb, wantEnhance);
      } else if (vb instanceof VariableDS.Builder) {
        enhanceVariable((VariableDS.Builder<?>) vb, wantEnhance);
      } else {
        throw new IllegalStateException("Not a VariableDS " + vb.shortName);
      }
    }
  }

  public static void enhanceSequence(SequenceDS.Builder<?> sdb, Set<Enhance> wantEnhance) {
    for (Variable.Builder<?> vb : sdb.vbuilders) {
      if (vb instanceof StructureDS.Builder) {
        enhanceStructure((StructureDS.Builder<?>) vb, wantEnhance);
      } else if (vb instanceof SequenceDS.Builder) {
        enhanceSequence((SequenceDS.Builder<?>) vb, wantEnhance);
      } else if (vb instanceof VariableDS.Builder) {
        enhanceVariable((VariableDS.Builder<?>) vb, wantEnhance);
      } else {
        throw new IllegalStateException("Not a VariableDS " + vb.shortName);
      }
    }
  }

  public static void enhanceVariable(VariableDS.Builder<?> vb, Set<Enhance> wantEnhance) {
    Set<Enhance> varEnhance = EnumSet.copyOf(wantEnhance);

    // varEnhance will only contain enhancements not already applied to orgVar.
    if (vb.orgVar instanceof VariableDS) {
      for (Enhance orgVarEnhancement : ((VariableDS) vb.orgVar).getEnhanceMode()) {
        varEnhance.remove(orgVarEnhancement);
      }
    }

    // enhance() may have been called previously, with a different enhancement set.
    // So, we need to reset to default before we process this new set.
    // if (vb.orgDataType != null) {
    // vb.setDataType(vb.orgDataType);
    // }

    if (varEnhance.contains(Enhance.ConvertEnums) && vb.dataType.isEnum()) {
      vb.setArrayType(ArrayType.STRING);
    }
    vb.addEnhanceMode(varEnhance);
  }
}
