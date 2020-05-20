package es.upm.babel.cclib.jmsg;


class AbortMessage<E,T> extends Message<E,T> {
  public AbortMessage(Offer<E,T> offer) {
    super(offer);
  }

  public String toString() {
    return "AbortMessage("+getOffer()+")";
  }

  public static <E,T> AbortMessage<E,T> newAbortMessage(Offer<E,T> offer) {
    return new AbortMessage<E,T>(offer);
  }
}
