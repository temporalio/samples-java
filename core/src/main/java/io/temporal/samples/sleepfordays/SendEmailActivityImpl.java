package io.temporal.samples.sleepfordays;

public class SendEmailActivityImpl implements SendEmailActivity {
  @Override
  public void sendEmail(String email) {
    System.out.println(email);
  }
}
