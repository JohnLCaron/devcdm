module dev.cdm.core {
  requires transitive dev.cdm.array;
  requires com.google.common;
  requires transitive java.net.http;
  requires org.jdom2;
  requires org.slf4j;
  requires org.jetbrains.annotations;

  exports dev.cdm.core.api;
  exports dev.cdm.core.calendar;
  exports dev.cdm.core.constants;
  exports dev.cdm.core.http;
  exports dev.cdm.core.io;
  exports dev.cdm.core.iosp;
  exports dev.cdm.core.spi;
  exports dev.cdm.core.util;
}