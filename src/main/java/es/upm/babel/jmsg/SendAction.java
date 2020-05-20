package es.upm.babel.cclib.jmsg;


class SendAction<E> extends BasicAction<E> {
  private E value;
  
  public SendAction(SynchronousChannel<E> channel, E value) {
    super(channel);
    this.value = value;
  }

  public E getValue() {
    return value;
  }

  public String toString() {
    return getChannel() + "!" + value;
  }
}

