/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package dev.ucdm.dataset.internal;

import com.google.common.base.Preconditions;
import dev.ucdm.array.Array;
import dev.ucdm.array.Immutable;
import dev.ucdm.array.StructureData;
import dev.ucdm.array.StructureDataStorageBB;
import dev.ucdm.array.StructureMembers;
import dev.ucdm.dataset.api.SequenceDS;

import java.nio.ByteBuffer;
import java.util.Iterator;

/**
 * Enhance StructureData, for SequenceDS.
 */
@Immutable
public class SequenceArrayEnhancer implements Iterator<StructureData> {
  private final SequenceDS topStructure;
  private final Iterator<StructureData> orgIterator;
  private final StructureMembers members;

  public SequenceArrayEnhancer(SequenceDS topStructure, Iterator<StructureData> orgIterator) {
    this.topStructure = topStructure;
    this.orgIterator = orgIterator;
    this.members = topStructure.makeStructureMembersBuilder().setStandardOffsets().build();
  }

  @Override
  public boolean hasNext() {
    return this.orgIterator.hasNext();
  }

  @Override
  public StructureData next() {
    StructureData sdata = this.orgIterator.next();
    return enhance(sdata);
  }

  private StructureData enhance(StructureData orgData) {
    StructureMembers orgMembers = orgData.getStructureMembers();
    ByteBuffer bbuffer = ByteBuffer.allocate(members.getStorageSizeBytes());
    StructureDataStorageBB storage = new StructureDataStorageBB(members, bbuffer, 1);

    for (StructureMembers.Member member : members) {
      StructureMembers.Member orgMember = orgMembers.findMember(member.getName());
      Preconditions.checkNotNull(orgMember, member.getName());
      Array<?> data = orgData.getMemberData(orgMember.getName());
      StructureDataArrayEnhancer.convertNestedData(topStructure, 0, member, storage, bbuffer, data);
    }
    return storage.get(0);
  }

}
