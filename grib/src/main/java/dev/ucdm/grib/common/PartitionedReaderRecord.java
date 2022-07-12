package dev.ucdm.grib.common;

import dev.ucdm.grib.collection.GribCollection;
import dev.ucdm.grib.collection.Partitions;

import javax.annotation.Nonnull;
import dev.ucdm.array.Immutable;
import java.io.IOException;

// ver8  PartitionCollectionImmutable.DataRecord
@Immutable
public class PartitionedReaderRecord extends GribReaderRecord {
  final Partitions usePartition;
  final int partno; // partition index in usePartition

  public PartitionedReaderRecord(Partitions usePartition, int partno, GdsHorizCoordSys hcs,
                                 GribCollection.ReadRecord record) {
    super(-1, record, hcs);
    this.usePartition = usePartition;
    this.partno = partno;
  }

  @Override
  public int compareTo(@Nonnull GribReaderRecord o) {
    PartitionedReaderRecord op = (PartitionedReaderRecord) o;
    int rp = usePartition.name().compareTo(op.usePartition.name());
    if (rp != 0)
      return rp;
    int r = Integer.compare(partno, op.partno);
    if (r != 0)
      return r;
    r = Integer.compare(record.fileno(), o.record.fileno());
    if (r != 0)
      return r;
    return Long.compare(record.pos(), o.record.pos());
  }

  boolean usesSameFile(PartitionedReaderRecord o) {
    if (o == null)
      return false;
    int rp = usePartition.name().compareTo(o.usePartition.name());
    if (rp != 0)
      return false;
    int r = Integer.compare(partno, o.partno);
    if (r != 0)
      return false;
    r = Integer.compare(record.fileno(), o.record.fileno());
    return r == 0;
  }

  // debugging
  public void show() throws IOException {
    String dataFilename = usePartition.getFilename(partno, record.fileno());
    System.out.printf(" **DataReader partno=%d fileno=%d filename=%s startPos=%d%n", partno, record.fileno(),
            dataFilename, record.pos());
  }
}
