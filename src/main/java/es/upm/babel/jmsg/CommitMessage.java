package es.upm.babel.cclib.jmsg;


class CommitMessage<E,T> extends Message<E,T> {
  public CommitMessage(Offer<E,T> offer) {
    super(offer);
  }

  public String toString() {
    return "CommitMessage("+getOffer()+")";
  }

  public static <E,T> CommitMessage<E,T> newCommitMessage(Offer<E,T> offer) {
    return new CommitMessage<E,T>(offer);
  }
}
