/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package dev.cdm.dataset.api;

import com.google.common.collect.ImmutableList;
import dev.cdm.array.ArrayType;
import dev.cdm.array.StructureData;
import dev.cdm.core.api.Group;
import dev.cdm.core.api.Sequence;
import dev.cdm.core.api.Structure;
import dev.cdm.core.api.Variable;
import dev.cdm.dataset.internal.SequenceArrayEnhancer;

import javax.annotation.concurrent.Immutable;
import java.util.Iterator;
import java.util.List;

/** An "enhanced" Sequence */
@Immutable
public class SequenceDS extends Sequence implements StructureEnhanced {

  /** An iterator over all the data in the sequence, enhanced if needed. */
  @Override
  public Iterator<StructureData> iterator() {
    return new SequenceArrayEnhancer(this, orgSeq.iterator());
  }

  @Override
  public Variable getOriginalVariable() {
    return orgSeq;
  }

  @Override
  public String getOriginalName() {
    return orgName;
  }

  @Override
  public List<CoordinateSystem> getCoordinateSystems() {
    return ImmutableList.of();
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////
  private final Sequence orgSeq;
  private final String orgName; // in case Sequence was renamed, and we need the original name

  protected SequenceDS(Builder<?> builder, Group parentGroup) {
    super(builder, parentGroup);
    this.orgSeq = builder.orgSeq;
    this.orgName = builder.orgName;
  }

  @Override
  public Builder<?> toBuilder() {
    return addLocalFieldsToBuilder(builder());
  }

  // Add local fields to the passed - in builder.
  protected Builder<?> addLocalFieldsToBuilder(Builder<? extends Builder<?>> b) {
    b.setOriginalSequence(this.orgSeq).setOriginalName(this.orgName);
    return (Builder<?>) super.addLocalFieldsToBuilder(b);
  }

  /** Get a Builder of SequenceDS. */
  public static Builder<?> builder() {
    return new Builder2();
  }

  private static class Builder2 extends Builder<Builder2> {
    @Override
    protected Builder2 self() {
      return this;
    }
  }

  public static abstract class Builder<T extends Builder<T>> extends Sequence.Builder<T> {
    private Sequence orgSeq;
    private String orgName;
    private boolean built;

    public T setOriginalSequence(Sequence orgVar) {
      this.orgSeq = orgVar;
      return self();
    }

    public T setOriginalName(String orgName) {
      this.orgName = orgName;
      return self();
    }

    /** Copy metadata from orgVar. */
    public T copyFrom(Sequence orgVar) {
      super.copyFrom(orgVar);
      for (Variable v : orgVar.getVariables()) {
        Variable.Builder<?> newVar;
        if (v instanceof Sequence) {
          newVar = SequenceDS.builder().copyFrom((Sequence) v);
        } else if (v instanceof Structure) {
          newVar = StructureDS.builder().copyFrom((Structure) v);
        } else {
          newVar = VariableDS.builder().copyFrom(v);
        }
        addMemberVariable(newVar);
      }
      setOriginalSequence(orgVar);
      setOriginalName(orgVar.getShortName());
      return self();
    }

    /** Normally this is called by Group.build() */
    public SequenceDS build(Group parentGroup) {
      if (built)
        throw new IllegalStateException("already built");
      built = true;
      this.setArrayType(ArrayType.SEQUENCE);
      return new SequenceDS(this, parentGroup);
    }
  }

}
