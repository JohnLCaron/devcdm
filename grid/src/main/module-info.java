module dev.ucdm.grid {
  requires transitive dev.ucdm.array;
  requires transitive dev.ucdm.core;
  requires transitive dev.ucdm.dataset;
  requires com.google.common;
  requires org.slf4j;
  requires tech.units.indriya;
  requires static org.jetbrains.annotations;

  exports dev.ucdm.grid.api;
}