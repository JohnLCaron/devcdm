package dev.ucdm.dataset.internal;

import dev.ucdm.array.ArrayType;
import dev.ucdm.core.api.Group;
import dev.ucdm.core.api.Variable;
import dev.ucdm.dataset.api.CdmDataset;
import dev.ucdm.dataset.api.CdmDataset.Enhance;
import dev.ucdm.dataset.api.SequenceDS;
import dev.ucdm.dataset.api.StructureDS;
import dev.ucdm.dataset.api.VariableDS;

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

    if (varEnhance.contains(Enhance.ConvertEnums) && vb.dataType.isEnum()) {
      vb.setArrayType(ArrayType.STRING);
    }
    vb.addEnhanceMode(varEnhance);
  }
}
