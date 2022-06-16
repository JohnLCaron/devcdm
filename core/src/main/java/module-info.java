module dev.cdm.core {
  requires transitive dev.cdm.array;
  requires com.google.common;
  requires org.jdom2;
  requires org.slf4j;
  requires jsr305;
  requires re2j;

  exports dev.cdm.core.api;
  exports dev.cdm.core.calendar;
  exports dev.cdm.core.constants;
  exports dev.cdm.core.util;
}