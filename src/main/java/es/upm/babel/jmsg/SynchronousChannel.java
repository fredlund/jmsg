package es.upm.babel.cclib.jmsg;

import java.util.ArrayList;
import java.util.function.Predicate;
import java.util.logging.Logger;
import java.util.logging.Level;


/**
 * A SynchronousChannel represents a communication channel for synchronizing between exactly two actions
 * (i.e., implementing binary synchronous message passing):
 * a sending action which transmits a value, and a receiveing action which receives the value.
 */
public class SynchronousChannel<E> implements Comparable<SynchronousChannel<E>> {

  private volatile ArrayList<Offer<E,?>> senders;
  private volatile ArrayList<Offer<E,?>> receivers;
  private volatile Offer<E,?> sendOffer;
  private volatile Offer<E,?> receiveOffer;
  private volatile boolean lockedDownSender;
  private volatile boolean lockedDownReceiver;
  private volatile boolean isIdle;
  private long id;
  private static volatile Long counter = 0L;
  private String nickName;
  
  /**
   * Creates a new synchronous channel.
   */
  public SynchronousChannel() {
    this(null);
  }

  /**
   * Creates a new synchronous channel with the associated nick name, which will be displayed
   * when printing the channel.
   */
  public SynchronousChannel(String nickName) {
    this.senders = new ArrayList<Offer<E,?>>();
    this.receivers = new ArrayList<Offer<E,?>>();
    this.lockedDownSender = false;
    this.lockedDownReceiver = false;
    this.isIdle = true;
    this.nickName = nickName;
    synchronized (counter) {
      id = counter++;
    }
  }

  public int compareTo(SynchronousChannel<E> other) {
    long otherId = other.getId();
    if (id < otherId) return -1;
    else if (id > otherId) return 1;
    else return 0;
  }

  long getId() {
    return id;
  }

  synchronized void offer(Offer<E,?> offer) {
    if (JMsgProcess.getLogger().isLoggable(Level.FINE))
      JMsgProcess.getLogger().log(Level.FINE,this+": offer "+offer);
    if (offer.isSendOffer()) 
      senders.add(offer);
    else
      receivers.add(offer);
    if (isIdle) checkMatchingOffers();
  }
  
  synchronized <T> void offers(ArrayList<Offer<E,T>> offers) {
    if (JMsgProcess.getLogger().isLoggable(Level.FINE))
      JMsgProcess.getLogger().log(Level.FINE,this+": offers "+offers+" isIdle="+isIdle+" senders="+senders+" receivers="+receivers);
    for (Offer<E,T> offer : offers) {
      if (offer.isSendOffer()) 
	senders.add(offer);
      else
	receivers.add(offer);
    }
    if (isIdle) checkMatchingOffers();
  }
  
  synchronized void acceptLockDown(Offer<E,?> offer) {
    if (JMsgProcess.getLogger().isLoggable(Level.FINE))
      JMsgProcess.getLogger().log(Level.FINE,this+": "+"acceptLockDown("+offer+")"); 

    if (sendOffer == offer)
      lockedDownSender = true;
    else if (receiveOffer == offer)
      lockedDownReceiver = true;
    
    if (lockedDownSender && lockedDownReceiver)
      commitToOffers(sendOffer, receiveOffer);
  }

  private synchronized boolean doCancelOffer(Offer<E,?> offer) {
    boolean reCheckMatch = false;
    boolean removeOffer = true;
    
    if (!isIdle) {
      // Check if the cancelled offer is part of a synchronization attempt
      if (offer == sendOffer) {
        isIdle = true;
        if (receiveOffer.originatesFromSelect()) {
          JMsgProcess.send(this,receiveOffer.getOriginatingId(), AbortMessage.newAbortMessage(receiveOffer));
        } else receivers.add(receiveOffer);
        removeOffer = false;
        reCheckMatch = true;
      } else if (offer == receiveOffer) {
        isIdle = true;
        if (sendOffer.originatesFromSelect()) {
          JMsgProcess.send(this,sendOffer.getOriginatingId(), AbortMessage.newAbortMessage(sendOffer));
        } else senders.add(sendOffer);
        removeOffer = false;
	reCheckMatch = true;
      } 
    }

    if (removeOffer) {
      if (JMsgProcess.getLogger().isLoggable(Level.FINE))
        JMsgProcess.getLogger().log(Level.FINE,this+": removing "+offer);
      if (offer.isSendOffer())
        senders.remove(offer);
      else
        receivers.remove(offer);
    }
    return reCheckMatch;
  }

  synchronized <T> void cancelOffer(Offer<E,T> offer) {
    if (JMsgProcess.getLogger().isLoggable(Level.FINE))
      JMsgProcess.getLogger().log(Level.FINE,this+": "+"cancelOffer("+offer+")");
    if (doCancelOffer(offer))
      checkMatchingOffers();
  }

  synchronized <T> void cancelOffers(ArrayList<Offer<E,T>> offers, Offer<?,?> remain) {
    if (JMsgProcess.getLogger().isLoggable(Level.FINE))
      JMsgProcess.getLogger().log(Level.FINE,this+": "+"cancelOffers("+offers+") remain="+remain+" senders="+senders+" receivers="+receivers);

    boolean checkMatchingOffers = false;
    for (Offer<E,T> offer : offers) {
      if (offer != remain)
	checkMatchingOffers = checkMatchingOffers || doCancelOffer(offer);
    }
    
    if (JMsgProcess.getLogger().isLoggable(Level.FINE))
      JMsgProcess.getLogger().log(Level.FINE,this+": "+"post cancelOffers("+offers+") remain="+remain+" senders="+senders+" receivers="+receivers);

    if (checkMatchingOffers)
      checkMatchingOffers();
  }
   
  private void checkMatchingOffers() {
    int sendersSize = senders.size();
    int receiversSize = receivers.size();
    boolean found = false;
    
    if (sendersSize > 0 && receiversSize > 0) {
      for (int senderIndex = 0; senderIndex < sendersSize && !found; senderIndex++) {
        for (int receiverIndex = 0; receiverIndex < receiversSize && !found; receiverIndex++) {
          found = checkMatchingOffer(senderIndex,receiverIndex);
        }
      }
    }
  }

  private boolean checkMatchingOffer(int sendIndex, int receiveIndex) {
    Offer<E,?> sendOffer = senders.get(sendIndex);
    Offer<E,?> receiveOffer = receivers.get(receiveIndex);
    
    if (sendOffer.getOriginatingId() != receiveOffer.getOriginatingId()) {
      
      @SuppressWarnings("unchecked")
        ReceiveAction<E> ra = (ReceiveAction) receiveOffer.getAction().getBasicAction();
      @SuppressWarnings("unchecked")
        SendAction<E> sa = (SendAction) sendOffer.getAction().getBasicAction();

      Predicate<E> guard = ra.getGuard();
      E value = sa.getValue();

      if (guard == null || guard.test(value)) {
        senders.remove(sendIndex);
        receivers.remove(receiveIndex);

        // This could work; lets see if we have to lockdown the involved processes
        if (!receiveOffer.originatesFromSelect() && !sendOffer.originatesFromSelect()) {
          commitToOffers(sendOffer, receiveOffer);
          return true;
        } else {

	  // We have to the protocol dance
	  isIdle = false;
	  this.sendOffer = sendOffer;
	  this.receiveOffer = receiveOffer;

	  if (JMsgProcess.getLogger().isLoggable(Level.FINE))
	    JMsgProcess.getLogger().log(Level.FINE,this+": will try to synch "+sendOffer+" and "+receiveOffer);
                
	  if (sendOffer.originatesFromSelect()) {
	    lockedDownSender = false;
	    JMsgProcess.send(this,sendOffer.getOriginatingId(), LockDownMessage.newLockDownMessage(sendOffer));
	  } else lockedDownSender = true;

	  if (receiveOffer.originatesFromSelect()) {
	    JMsgProcess.send(this,receiveOffer.getOriginatingId(), LockDownMessage.newLockDownMessage(receiveOffer));
	    lockedDownReceiver = false;
	  } else lockedDownReceiver = true;
	}
        return true;
      } else return false;
    } else return false;
  }

  private void commitToOffers(Offer<E,?> sendOffer, Offer<E,?> receiveOffer) {
    E value = null;
    BasicAction<E> action = sendOffer.getAction().getBasicAction();
    if (action instanceof SendAction<?>) {
      value = ((SendAction<E>) action).getValue();
    }
    sendOffer.setResolvedValue(value);
    receiveOffer.setResolvedValue(value);

    JMsgProcess.send(this,sendOffer.getOriginatingId(), CommitMessage.newCommitMessage(sendOffer));
    JMsgProcess.send(this,receiveOffer.getOriginatingId(), CommitMessage.newCommitMessage(receiveOffer));
    isIdle = true;
  }

  public String toString() {
    if (nickName != null)
      return nickName;
    else
      return "channel "+Long.toString(getId());
  }
}
