package es.upm.babel.cclib.jmsg;


class BasicAction<E> {
  protected SynchronousChannel<E> channel;

  public BasicAction(SynchronousChannel<E> channel) {
    this.channel = channel;
  }

  public SynchronousChannel<E> getChannel() {
    return channel;
  }
}
