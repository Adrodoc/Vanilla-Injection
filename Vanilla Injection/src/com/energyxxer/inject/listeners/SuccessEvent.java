package com.energyxxer.inject.listeners;

import static com.google.common.base.Preconditions.checkNotNull;

import java.time.LocalTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

/**
 * An event class that indicates that a certain command has been run successfully, specifying its
 * time of invocation, its invoker, and its output message.
 */
public class SuccessEvent {
  private static final Pattern pattern =
      Pattern.compile("\\[(\\d\\d):(\\d\\d):(\\d\\d)\\] \\[Server thread/INFO\\]: \\[(.+?): (.+)]");
  // private static final int TIMESTAMP_LENGTH = "[00:00:00]".length();
  // private static final String PREFIX = " [Server thread/INFO]: [";

  /**
   * Attempts to create a {@link SuccessEvent} from the given log line matching this pattern:
   *
   * <pre>
   * [<em>timestamp</em>] [Server thread/INFO]: [<em>invoker</em>: <em>message</em>]
   * </pre>
   *
   * @param line the log line to create a SuccessEvent from
   *
   * @return a {@link SuccessEvent} representing the specified log line, if it matches the pattern,
   *         {@code null} otherwise
   */
  public static @Nullable SuccessEvent createFromLogLine(String line) {
    Matcher matcher = pattern.matcher(line);
    if (matcher.matches()) {
      int hour = Integer.parseInt(matcher.group(1));
      int minute = Integer.parseInt(matcher.group(2));
      int second = Integer.parseInt(matcher.group(3));
      LocalTime timestamp = LocalTime.of(hour, minute, second);
      String invoker = matcher.group(4);
      String message = matcher.group(5);
      return new SuccessEvent(timestamp, invoker, message);
    }

    // int colonIndex = line.indexOf(": ", TIMESTAMP_LENGTH + PREFIX.length());
    // if (line.length() >= TIMESTAMP_LENGTH + PREFIX.length()//
    // && line.charAt(0) == '['//
    // && line.charAt(TIMESTAMP_LENGTH - 1) == ']'//
    // && line.startsWith(PREFIX, TIMESTAMP_LENGTH)//
    // && colonIndex != -1//
    // && line.endsWith("]")//
    // ) {
    // String timestamp = line.substring(0, TIMESTAMP_LENGTH);
    // String invoker = line.substring(TIMESTAMP_LENGTH, TIMESTAMP_LENGTH + PREFIX.length());
    // String message = line.substring(colonIndex + 2, line.length() - 1);
    // return new SuccessEvent(timestamp, invoker, message);
    // }
    return null;
  }

  /**
   * The command's execution time as obtained from the log file.
   */
  private final LocalTime timestamp;
  /**
   * The command's invoker name.
   */
  private final String invoker;
  /**
   * The command's output message as obtained from the log file.
   */
  private final String message;

  /**
   * Creates a log event from the given line.
   *
   * @param timestamp The timestamp of the command's execution as printed to the log file.
   * @param invoker The name of the executing command block minecart.
   * @param message The command's success message.
   */
  private SuccessEvent(LocalTime timestamp, String invoker, String message) {
    this.timestamp = checkNotNull(timestamp, "timestamp == null!");
    this.invoker = checkNotNull(invoker, "invoker == null!");
    this.message = checkNotNull(message, "message == null!");
  }

  /**
   * Gets the timestamp of the message.
   *
   * @return The timestamp, as printed to the log.
   */
  public LocalTime getTimestamp() {
    return timestamp;
  }

  /**
   * Gets the name of the entity that ran this command.
   *
   * @return The name of the entity.
   */
  public String getInvoker() {
    return invoker;
  }

  /**
   * Gets the output message from the command block.
   *
   * @return The command's success message.
   */
  public String getMessage() {
    return message;
  }

  @Override
  public String toString() {
    return timestamp + " [" + invoker + ": " + message + "]";
  }

  /**
   * Attempts to create a SuccessEvent from the given log line matching this pattern: <br>
   * <code>[<em>timestamp</em>] [Server thread/INFO]: [<em>invoker</em>: <em>message</em>]</code>
   *
   * @param line The log line to create a SuccessEvent from.
   * @param name The name of the entity to run the command.
   *
   * @return A SuccessEvent representing this log line, if it matches the pattern. Returns
   *         <code>null</code> otherwise.
   */
  @Deprecated
  public static SuccessEvent createFromLogLine(String line, String name) {
    String header = "[Server thread/INFO]: [" + name + ": ";
    int index = line.indexOf(header);
    if (index >= 0 && index < 15) {
      String timestamp = line.substring(0, index - 1);
      String str = line.substring(index + header.length());
      return new SuccessEvent(LocalTime.parse(timestamp), name, str.substring(0, str.length() - 1));
    }
    return null;
  }
}
