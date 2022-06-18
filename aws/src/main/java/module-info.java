module dev.cdm.aws {
  requires transitive dev.cdm.array;
  requires transitive dev.cdm.core;
  requires com.google.common;
  requires org.slf4j;
  requires org.jetbrains.annotations;
  requires software.amazon.awssdk.auth;
  requires software.amazon.awssdk.core;
  requires software.amazon.awssdk.http.apache;
  requires software.amazon.awssdk.profiles;
  requires software.amazon.awssdk.regions;
  requires software.amazon.awssdk.services.s3;

  exports dev.cdm.aws.s3;
}