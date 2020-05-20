package es.upm.babel.cclib.jmsg;


class LockDownMessage<E,T> extends Message<E,T> {
  public LockDownMessage(Offer<E,T> offer) {
    super(offer);
  }

  public String toString() {
    return "LockDownMessage("+getOffer()+")";
  }

  public static <E,T> LockDownMessage<E,T> newLockDownMessage(Offer<E,T> offer) {
    return new LockDownMessage<E,T>(offer);
  }
}
