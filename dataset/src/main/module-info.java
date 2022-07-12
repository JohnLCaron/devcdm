module dev.cdm.dataset {
  requires transitive dev.cdm.array;
  requires transitive dev.cdm.core;
  requires com.google.common;
  requires org.jdom2;
  requires org.slf4j;
  requires tech.units.indriya;
  requires static org.jetbrains.annotations;
  requires kotlin.stdlib;

  exports dev.ucdm.dataset.api;
  exports dev.ucdm.dataset.geoloc;
  exports dev.ucdm.dataset.ncml;
  exports dev.ucdm.dataset.transform.vertical;
}