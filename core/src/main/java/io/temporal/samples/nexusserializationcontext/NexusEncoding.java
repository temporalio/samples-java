package io.temporal.samples.nexusserializationcontext;

enum NexusEncoding {
  NONE(""),
  HMAC("binary/nexus-hmac"),
  ZLIB("binary/nexus-zlib");

  private final String encodingName;

  NexusEncoding(String encodingName) {
    this.encodingName = encodingName;
  }

  String encodingName() {
    return encodingName;
  }
}
