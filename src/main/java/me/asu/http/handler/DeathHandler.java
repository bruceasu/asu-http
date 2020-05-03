package me.asu.http.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

/**
 * A DeathHandler should only be called if something bad occurs.
 *
 * The DeathHandler is used on the backend to send a 500 message to the
 * browser if all of the other handlers fail to do things. Which they
 * shouldn't.
 *
 * It's also set as the initial wildcard handler, meaning if there aren't any
 * other handlers available, it'll be used.
 */
class DeathHandler implements HttpHandler
{
  private int code;


  public static ArrayList<String> errorMessages;

  /**
   * Creates a new DeathHandler...
   */
  public DeathHandler() {
    this(500);
  }


  public DeathHandler(int statusCode) {
    super();
    setupErrorMessages();
    code = statusCode;
  }

  @Override
  public void handle(HttpExchange exchange) throws IOException
  {
    String message = errorMessages.get(
            new Random().nextInt(errorMessages.size()));
    exchange.sendResponseHeaders(code, message.length());
  }

  /**
   * Setup error messages that could be sent to the client
   */
  private static void setupErrorMessages() {
    errorMessages = new ArrayList<String>();

    errorMessages.add("Well, that went well...");
    errorMessages.add("That's not a good sound.");
    errorMessages.add("Oh God, oh God, we're all gonna die.");
    errorMessages.add("What a crazy random happenstance!");
    errorMessages.add("Uh, everything's under control. Situation normal.");
    errorMessages.add("Uh, we had a slight weapons malfunction, but, uh... "
            + "everything's perfectly all right now. We're fine. We're all "
            + "fine here now, thank you. How are you?");
    errorMessages.add("Definitely feeling aggressive tendency, sir!");
    errorMessages.add("If they move, shoot 'em.");

  }
}
