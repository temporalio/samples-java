package io.temporal.samples.nexusmessaging.callerpattern.handler;

import io.temporal.samples.nexusmessaging.callerpattern.service.Language;
import java.util.EnumMap;
import java.util.Map;

public class GreetingActivityImpl implements GreetingActivity {

  private static final Map<Language, String> GREETINGS = new EnumMap<>(Language.class);

  static {
    GREETINGS.put(Language.ARABIC, "مرحبا بالعالم");
    GREETINGS.put(Language.CHINESE, "你好，世界");
    GREETINGS.put(Language.ENGLISH, "Hello, world");
    GREETINGS.put(Language.FRENCH, "Bonjour, monde");
    GREETINGS.put(Language.HINDI, "नमस्ते दुनिया");
    GREETINGS.put(Language.PORTUGUESE, "Olá mundo");
    GREETINGS.put(Language.SPANISH, "Hola mundo");
  }

  @Override
  public String callGreetingService(Language language) {
    return GREETINGS.get(language);
  }
}
