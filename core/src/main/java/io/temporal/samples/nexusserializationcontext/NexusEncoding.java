package io.temporal.samples.nexusserializationcontext;

enum NexusEncoding {
  AES_GCM("binary/nexus-aes-gcm"),
  ZLIB("binary/nexus-zlib");

  private final String encodingName;

  NexusEncoding(String encodingName) {
    this.encodingName = encodingName;
  }

  String encodingName() {
    return encodingName;
  }
}
