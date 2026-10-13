package io.temporal.samples.nexussecurecredentialpropagation.common;

/** Generates the demo keysets shared by the starter, caller Worker, and handler Worker. */
public class GenerateDemoKeys {
  public static void main(String[] args) throws Exception {
    SampleKeysets.fromDefaultDirectory().generate();
    System.out.println(
        "Generated demo keysets. They are stored unencrypted and are for local demos only.");
  }
}
