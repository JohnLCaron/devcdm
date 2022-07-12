module dev.cdm.core {
  requires transitive dev.cdm.array;
  requires com.google.common;
  requires transitive java.net.http;
  requires org.jdom2;
  requires org.slf4j;
  requires org.jetbrains.annotations;

  exports dev.ucdm.core.api;
  exports dev.ucdm.core.calendar;
  exports dev.ucdm.core.constants;
  exports dev.ucdm.core.http;
  exports dev.ucdm.core.io;
  exports dev.ucdm.core.iosp;
  exports dev.ucdm.core.util;
  exports dev.ucdm.core.write;
}