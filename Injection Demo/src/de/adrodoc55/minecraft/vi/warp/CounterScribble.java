package de.adrodoc55.minecraft.vi.warp;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.energyxxer.inject.v2.InjectionConnection;

public class CounterScribble {
  private static int i;

  public static void main(String[] args) throws IOException, InterruptedException {
    Path logFile = Paths.get("C:/Users/Adrian/AppData/Roaming/.minecraft/logs/latest.log");
    Path worldDir = Paths.get("C:/Users/Adrian/AppData/Roaming/.minecraft/saves/RedstoneReady");
    String identifier = "promise";
    InjectionConnection connection = new InjectionConnection(logFile, worldDir, identifier);
    connection.addChatListener(e -> {
      if ("c".equals(e.getMessage())) {
        connection.injectCommand("say " + i++);
      }
    });
  }
}
