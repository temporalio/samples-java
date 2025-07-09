package io.temporal.samples.packetdelivery;

public class Packet {
  private int id;
  private String content;

  public Packet() {}

  public Packet(int id, String content) {
    this.id = id;
    this.content = content;
  }

  public int getId() {
    return id;
  }

  public String getContent() {
    return content;
  }
}
