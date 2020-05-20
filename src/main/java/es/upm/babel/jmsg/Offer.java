package es.upm.babel.cclib.jmsg;

class Offer<E,T> {
  private Action<E,T> action;
  private int selectIndex;
  private long threadId;
  private long timestamp;
  E resolvedValue;
  
  public Offer(Action<E,T> action) {
    this.action = action;
    this.selectIndex = -1;
    this.threadId = Thread.currentThread().getId();
    this.resolvedValue = null;
  }

  public Offer(Action<E,T> action, long timestamp, int index) {
    this.action = action;
    this.timestamp = timestamp;
    this.selectIndex = index;
    this.threadId = Thread.currentThread().getId();
    this.resolvedValue = null;
  }

  public Action<E,T> getAction() {
    return action;
  }

  public boolean isSendOffer() {
    return action.getBasicAction() instanceof SendAction<?>;
  }

  public boolean originatesFromSelect() {
    return selectIndex >= 0;
  }

  public void setTimestamp(long timestamp) {
    this.timestamp = timestamp;
  }

  public long getTimestamp() {
    return timestamp;
  }
  
  public int getSelectIndex() {
    return selectIndex;
  }

  public long getOriginatingId() {
    return threadId;
  }

  public void setResolvedValue(E value) {
    resolvedValue = value;
  }

  public E getResolvedValue() {
    return resolvedValue;
  }

  public T accept() {
    return action.accept(getResolvedValue());
  }

  public String toString() {
    if (originatesFromSelect())
      return getOriginatingId()+" -> selectoffer("+action.toString()+" @"+getTimestamp()+")";
    else
      return getOriginatingId()+" -> offer("+action.toString()+" @"+getTimestamp()+")";
  }
}
