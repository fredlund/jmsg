package es.upm.babel.cclib.jmsg;

import java.util.function.Predicate;


class ReceiveAction<E> extends BasicAction<E> {
  private SynchronousChannel<E> channel;
  private Predicate<E> guard;
  
  public ReceiveAction(SynchronousChannel<E> channel) {
    super(channel);
    this.guard = null;
  }

  public ReceiveAction(SynchronousChannel<E> channel, Predicate<E> guard) {
    super(channel);
    this.guard = guard;
  }

  public Predicate<E> getGuard() {
    return guard;
  }

  public String toString() {
    return getChannel() + "?";
  }
}

