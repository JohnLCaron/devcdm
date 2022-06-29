module dev.cdm.dataset {
  requires transitive dev.cdm.array;
  requires transitive dev.cdm.core;
  requires com.google.common;
  requires org.jdom2;
  requires org.slf4j;
  requires tech.units.indriya;
  requires static org.jetbrains.annotations;
  requires kotlin.stdlib;

  exports dev.cdm.dataset.api;
  exports dev.cdm.dataset.geoloc;
  exports dev.cdm.dataset.ncml;
  exports dev.cdm.dataset.transform.vertical;
}