package es.upm.babel.cclib.jmsg;


class Message<E,T> {
  private Offer<E,T> offer;

  public Message(Offer<E,T> offer) {
    this.offer = offer;
  }

  public Offer<E,T> getOffer() {
    return this.offer;
  }
}
