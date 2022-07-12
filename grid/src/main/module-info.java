module dev.cdm.grid {
  requires transitive dev.cdm.array;
  requires transitive dev.cdm.core;
  requires transitive dev.cdm.dataset;
  requires com.google.common;
  requires org.slf4j;
  requires tech.units.indriya;
  requires static org.jetbrains.annotations;

  exports dev.ucdm.grid.api;
}