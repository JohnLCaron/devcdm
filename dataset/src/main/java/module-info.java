module dev.cdm.dataset {
  requires transitive dev.cdm.array;
  requires transitive dev.cdm.core;
  requires com.google.common;
  requires org.jdom2;
  requires org.slf4j;
  requires static org.jetbrains.annotations;

  exports dev.cdm.dataset.api;
  exports dev.cdm.dataset.geoloc;
  exports dev.cdm.dataset.ncml;
  exports dev.cdm.dataset.transform.vertical;
}