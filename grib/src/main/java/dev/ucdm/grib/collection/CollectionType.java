package dev.ucdm.grib.collection;

public enum CollectionType { // must match with GribCollectionProto.Dataset.Type
    SRC, // GC: Single Runtime Collection [ntimes]
    MRC, // GC: Multiple Runtime Collection [nruns, ntimes]
    MRUTC, // GC: Multiple Runtime, Unique Time Collection [ntimes] (there are no overlapping times)

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

  /* maybe better:
  * SRC,  // GC: Single Runtime Collection runtime, time(time)
  * MRUTC, // GC: Multiple Runtime, Unique Time Collection runtime(time), time(time) (times are unique), nruntimes = ntimes, eg obs

  * MRC, // GC: Multiple Runtime Collection runtime(runtime), time(runtime, time); problem is that time(runtime, time) gets too big. so instead of
       an array its a function getTimes(runtime) : CoordTime
  * TwoDOrth // runtime(runtime), offset(time), time(runtime, time) = runtime(runtime) + offset(time)
  * TwoDReg // runtime(runtime), offset(hour, time), time(runtime, time) = runtime(runtime) + offset(hour, time)
  *
  GridTimeCoordinateSystem.Type {
            Observation,    // Observational data, no runtime or unique time MRUTC time(time), optional runtime(time)
            SingleRuntime,  // Single runtime.  SRC runtime, time(time)
            Offset,         // Multiple runtimes all having the same time offsets (orthogonal). TwoDOrth
            OffsetRegular,  // All runtimes, grouped by time since 0z, have the same offsets (regular) TwoDReg
            OffsetIrregular // Runtimes that have irregular offsets. MRC: time(runtime, time) gets too big
            }
   */