package es.upm.babel.cclib.jmsg;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;


/**
 * An action represents a desire to synchronize over a channel, either
 * sending a value, or receiveing a value.
 * An action<E,F> synchronizes over a synchronized channel of type E,
 * and returns a value of type F.
 * <p>
 * An action can have an optional continuation, which is invoked with (for sending actions) the
 * value sent, or (for receiving action) the value received. 
 * The continuation can return a value, or return nothing but change e.g. object instance variables. 
 * It is recommended to use Java lambda functions to program the continuations.
 * </p>
 * <p>
 * Examples:<br>
 * <ul>
 * <li>
 * Sends the value over the channel ch (which should be of type Integer):
 * <code>
 * Action.send(ch,2)
 * </code>
 * <li>
 * Receives any integer value greater than zero:
 * <code>
 * Action.receiveWithGuard(ch,value -> value > 0)
 * </code>
 * <li>
 * Send the value 2, and then executes the continuation:
 * <code>
 * Action.sendVoid(ch,value -> { System.out.println("sent "+value); })
 * </code>
 * <li>
 * Sends an integer i as a string to the channel chS, 
 * and returns the value sent+1:
 * <code>
 * Actions.send(i.toString(),value -> i+1);
 * <code>
 * <li>
 * Receives a string over the channel (which should be of type String)
 * and returns an integer corresponding to the string:
 * <code>
 * Action.receive(chS,value -> Integer.parseInt(ch))
 * </code>
 * </ul>
 * </p>
 */
public class Action<E,F> {
    private BasicAction<E> action;
    private Function<E,F> fun;

    protected Action(BasicAction<E> action, Function<E,F> fun) {
	this.action = action;
	this.fun = fun;
    }

    protected BasicAction<E> getBasicAction() {
	return this.action;
    }

    protected Function<E,F> getFun() {
	return this.fun;
    }

    protected F accept(E value) {
      if (fun==null) return null;
      return fun.apply(value);
    }

  private static <T> Function<T,T> id() {
      return value -> value;
    }

  private static <T,U> Function<T,U> whatever(Consumer<T> consumer) {
    return
      value ->
      {
        consumer.accept(value);
        return null;
      };
  }

  /**
   * Creates a sending action, capable of transmitting value, and returning the value itself
   * when executed.
   */
    public static <T> Action<T,T> send(SynchronousChannel<T> channel, T value) {
      return new Action<T,T>(new SendAction<T>(channel,value), id());
    }

  /**
   * Creates a receiving action, capable of receiving any value of type T, and returning
   * the received value when executed.
   */
    public static <T> Action<T,T> receive(SynchronousChannel<T> channel) {
      return new Action<T,T>(new ReceiveAction<T>(channel), id());
    }

  /**
   * Creates a receiving action, capable of receiving any value of type T which satisfies
   * the guard predicate, and returning
   * the received value when executed.
   */
    public static <T> Action<T,T> receiveWithGuard(SynchronousChannel<T> channel, Predicate<T> guard) {
      return new Action<T,T>(new ReceiveAction<T>(channel,guard), id());
    }

  /**
   * Creates a sending action, capable of transmitting value, and returning (when executed) the result of applying
   * the function argument to the sent value.
   */
    public static <T,U> Action<T,U> send(SynchronousChannel<T> channel, T value, Function<T,U> continuation) {
      return new Action<T,U>(new SendAction<T>(channel,value),continuation);
    }

  /**
   * Creates a sending action, capable of transmitting value,
   * executes the continuation, but returns nothing.
   */
    public static <T,U> Action<T,U> sendVoid(SynchronousChannel<T> channel, T value, Consumer<T> continuation) {
      return new Action<T,U>(new SendAction<T>(channel,value),whatever(continuation));
    }

  /**
   * Creates a receiving action, capable of receiving any value of type T which satisfies
   * the guard predicate, and returning (when executed)
   * the result of applying the function argument to the received value.
   */
    public static <T,U> Action<T,U> receiveWithGuard(SynchronousChannel<T> channel, Predicate<T> guard, Function<T,U> continuation) {
      return new Action<T,U>(new ReceiveAction<T>(channel,guard),continuation);
    }

  /**
   * Creates a receiving action, capable of receiving any value of type T which satisfies
   * the guard predicate, 
   * executes the continuation, but returns nothing.
   */
    public static <T,U> Action<T,U> receiveWithGuardVoid(SynchronousChannel<T> channel, Predicate<T> guard, Consumer<T> continuation) {
      return new Action<T,U>(new ReceiveAction<T>(channel,guard),whatever(continuation));
    }

  /**
   * Creates a receivng action, capable of receiving any value of type T, 
   * and returning (when executed) the result of applying
   * the function argument to the received value.
   */
    public static <T,U> Action<T,U> receive(SynchronousChannel<T> channel, Function<T,U> continuation) {
      return new Action<T,U>(new ReceiveAction<T>(channel),continuation);
    }

  /**
   * Creates a receivng action, capable of receiving any value of type T, 
   * executes the continuation, but returns nothing.
   */
    public static <T,U> Action<T,U> receiveVoid(SynchronousChannel<T> channel, Consumer<T> continuation) {
      return new Action<T,U>(new ReceiveAction<T>(channel),whatever(continuation));
    }
}
