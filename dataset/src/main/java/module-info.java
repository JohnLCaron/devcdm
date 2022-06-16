module dev.cdm.dataset {
  requires transitive dev.cdm.array;
  requires transitive dev.cdm.core;
  requires com.google.common;
  requires org.jdom2;
  requires org.slf4j;
  requires jsr305;
  requires re2j;

  exports dev.cdm.dataset.api;
}