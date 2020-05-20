package es.upm.babel.cclib.jmsg;

import java.util.ArrayList;


class ChannelOffers<E,T> {
  SynchronousChannel<E> channel;
  ArrayList<Offer<E,T>> offers;
  
  public ChannelOffers(SynchronousChannel<E> channel) {
    this.channel = channel;
    this.offers = new ArrayList<Offer<E,T>>();
  }
  
  public static <T,U> ChannelOffers<T,U> newChannelOffers(Offer<T,U> offer) {
    ChannelOffers<T,U> chOffers = new ChannelOffers<T,U>(offer.getAction().getBasicAction().getChannel());
    return chOffers;
  }
  
  public SynchronousChannel<E> getChannel() {
    return channel;
  }

  public ArrayList<Offer<E,T>> getOffers() {
    return offers;
  }

  public void add(Offer<?,?> offer) {
    @SuppressWarnings("unchecked")
      Offer<E,T> myOffer = (Offer<E,T>) offer;
    offers.add(myOffer);
  }

  public void cancelOffers(Offer<?,?> remain) {
    boolean found = false;

    for (int i=0; i<offers.size() && !found; i++)
      found = offers.get(i) != remain;

    if (found)
      channel.cancelOffers(offers,remain);
  }

  public void offers() {
    channel.offers(offers);
  }

  public void offers(long timestamp) {
    for (int i=0; i<offers.size(); i++)
      offers.get(i).setTimestamp(timestamp);    
    channel.offers(offers);
  }
}

