package dev.ucdm.grib.collection;

public enum CollectionType { // must match with GribCollectionProto.Dataset.Type
    SRC, // GC: Single Runtime Collection [ntimes]
    MRC, // GC: Multiple Runtime Collection [nruns, ntimes]
    // MRSTC, // GC: Multiple Runtime Single Time Collection [nruns, 1]
    MRUTC, // GC: Multiple Runtime, Unique Time Collection [ntimes] (there are no overlapping times)

    // MRSTP, // PC: Multiple Runtime Single Time Partition [nruns, 1]
    TwoD, // PC: TwoD time partition [nruns, ntimes]
    Best, // PC: Best time partition [ntimes]
    BestComplete, // PC: Best complete time partition (not done) [ntimes]
    MRUTP; // PC: Multiple Runtime Unique Time Partition [ntimes]

    public boolean isSingleRuntime() {
      return this == SRC;
    }

    public boolean isUniqueTime() {
      return this == MRUTC || this == MRUTP || this == SRC;
    }

    public boolean isTwoD() {
      return this == MRC || this == TwoD;
    }

    public boolean isPartition() {
      return this == TwoD || this == MRUTP;
    }
  }