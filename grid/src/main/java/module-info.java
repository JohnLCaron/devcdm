module dev.cdm.grid {
  requires transitive dev.cdm.array;
  requires transitive dev.cdm.core;
  requires transitive dev.cdm.dataset;
  requires com.google.common;
  requires org.slf4j;
  requires static org.jetbrains.annotations;

  exports dev.cdm.grid.api;
}