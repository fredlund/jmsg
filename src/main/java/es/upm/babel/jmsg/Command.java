package es.upm.babel.cclib.jmsg;

import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.function.Function;
import java.util.function.Consumer;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.Random;


/**
 * Provides methods that can be called to send or receive messages.
 * The class provides a number of convient methods (send, receive, ...)
 * for creating actions, and executing them using the execute method.
 * <p>
 * In addition the select command permits to specify a choice
 * between a list of actions, such that the command returns
 * when exactly one of the actions has been completed.
 * </p>
 * <p>
 * Examples:<br>
 * <ul>
 * <li>
 * Basic usage: execute an action:
 * <code>
 * Command.execute(Action.send(ch,2));
 * </code>
 * <li>
 * The previous command has the same result as invoking the convient
 * method send:
 * <code>
 * Command.send(ch,2);
 * </code>
 * <li>
 * A choice between sending the value 2 over ch2, or receiving a value over ch:<br>
 * <pre>
 * <code>
 * List&lt;Action&lt;?,Integer&gt;&gt; selectArgs =
 *   Arrays.asList(Action.receiveWithGuard(ch1, value -> value > 10),
 *                 Action.send(ch2,2));
 * Integer result = Command.select(selectArgs);
 * </code>
 * </pre>
 * After the select command finishes, result is either 2 (if the send action
 * succeeded), or an integer>10 which was returned by the receive statement.
 * </ul>
 * </p>
 */
public class Command {

  /**
   * Sends value over the channel parameter. Returns when the value has been received.
   * @return The sent value
   */
  public static <T> T send(SynchronousChannel<T> channel, T value) {
    return execute(Action.send(channel,value));
  }
  
  /**
   * Receives a value sent over the channel parameter. 
   * @return The received value
   */
  public static <T> T receive(SynchronousChannel<T> channel) {
    return execute(Action.receive(channel));
  }
  
  /**
   * Receives a value (which must satisfy the guard predicate) sent over the channel parameter.
   * @return The received value
   */
  public static <T> T receiveWithGuard(SynchronousChannel<T> channel, Predicate<T> guard) {
    return execute(Action.receiveWithGuard(channel,guard));
  }
  
  /**
   * Executes an action.
   * @return The value returned by executing the action.
   */
  public static <T,U> U execute(Action<T,U> action) {
    Offer<T,U> offer = new Offer<T,U>(action);
    offer(offer);
    while (true) {
      Message<?,?> msg = JMsgProcess.receive();
      if (msg instanceof CommitMessage<?,?>) {
        CommitMessage<?,?> cm = (CommitMessage<?,?>) msg;
        if (cm.getOffer() == offer) {
          return offer.accept();
        } else {
          if (JMsgProcess.getLogger().isLoggable(Level.SEVERE))
            JMsgProcess.getLogger().log(Level.SEVERE,Thread.currentThread().getName()+": got offer "+msg+" was waiting for "+offer);
          throw new RuntimeException();
        }
      }
    }
  }

  /**
   * Exactly one of the actions in the parameter list, which can be synchronized, 
   * is executed. Note that all actions in the parameter list must return a value
   * of the same type T, but they may attempt to synchronize over channels
   * of different types. 
   * @return the value returned by executing the action.
   */
  public static <T> T select(List<Action<?,T>> actions) {
    SortedMap<SynchronousChannel<?>,ChannelOffers<?,T>> selectOffers =
      new TreeMap<SynchronousChannel<?>,ChannelOffers<?,T>>();
    long timestamp = 0;
    
    // Inform all channels of the select offers
    for (int i=0; i<actions.size(); i++) {
      Action<?,T> action = actions.get(i);
      
      if (action != null) {
        // Construct a select offer
        Offer<?,T> offer = newOffer(action,timestamp,i);
	ChannelOffers<?,T> chanOffers = selectOffers.get(action.getBasicAction().getChannel());
	if (chanOffers == null) {
	  chanOffers = ChannelOffers.newChannelOffers(offer);
	  selectOffers.put(action.getBasicAction().getChannel(),chanOffers);
	}
	chanOffers.add(offer);
      }
    }
    
    for (ChannelOffers<?,?> chOffers : selectOffers.values()) {
      chOffers.offers();
    }
    
    boolean resolvedSelect = false;
    
    while (true) {
      if (JMsgProcess.getLogger().isLoggable(Level.FINE))
        JMsgProcess.getLogger().log(Level.FINE,JMsgProcess.threadName()+" WAITING for message while idle");
      
      Message<?,?> msg = JMsgProcess.receive();
      
      if (msg instanceof LockDownMessage<?,?>) {
        @SuppressWarnings("unchecked")
        LockDownMessage<?,T> ld = (LockDownMessage<?,T>) msg;
        Offer<?,T> offer = ld.getOffer();
        
        if (offer.getTimestamp() == timestamp) {
          
          // We are in LOCKDOWN mode!
          // First cancel all synchronization offers, excepting offer
	  for (ChannelOffers<?,?> chOffers : selectOffers.values()) {
	    chOffers.cancelOffers(offer);
	  }
          
          // Signal that we accept lockdown
	  ++timestamp;
          acceptLockDown(offer);
          boolean synchronization_aborted = false;
          
          do {
            if (JMsgProcess.getLogger().isLoggable(Level.FINE))
              JMsgProcess.getLogger().log(Level.FINE,JMsgProcess.threadName()+" WAITING for message while synchronizing on "+offer);
            // Next wait until channel commits, or we are aborted
            msg = JMsgProcess.receive();
            
            if (msg instanceof AbortMessage<?,?>) {
              // Other process cancelled some synchronization involving us
              AbortMessage<?,?> ad = (AbortMessage<?,?>) msg;
              if (ad.getOffer() == offer) synchronization_aborted=true;
            } else if (msg instanceof LockDownMessage<?,?>) {
              // Some other channel wants to lock us down; we do nothing
              // since presumably the channel has already got a cancelOffer
            } else if (msg instanceof CommitMessage<?,?>) {
              // Other process also wants to continue
              CommitMessage<?,?> cm = (CommitMessage<?,?>) msg;
              Offer<?,?> cmOffer = cm.getOffer();
              
              if (cmOffer == offer) {
                if (JMsgProcess.getLogger().isLoggable(Level.FINE))
                  JMsgProcess.getLogger().log(Level.FINE,JMsgProcess.threadName()+" got COMMIT on offer "+offer);
                return offer.accept();
              } else {
                if (JMsgProcess.getLogger().isLoggable(Level.SEVERE))
                  JMsgProcess.getLogger().log(Level.SEVERE,Thread.currentThread().getName()+": got offer "+cm+" was waiting for "+offer);
                throw new RuntimeException();
              }
            } 
          } while (!synchronization_aborted);
          
          if (JMsgProcess.getLogger().isLoggable(Level.FINE))
            JMsgProcess.getLogger().log(Level.FINE,JMsgProcess.threadName()+" REOFFERING");
          
          // Synchronization was aborted; we reoffer our cancelled offers
	  for (ChannelOffers<?,?> chOffers : selectOffers.values()) {
	    chOffers.offers(timestamp);
	  }
          
        } else {
	  JMsgProcess.getLogger().log(Level.FINE,Thread.currentThread().getName()+": got lockdown "+offer+" but timestamp is "+timestamp+"; skipping");
	}
      } else {
	if (JMsgProcess.getLogger().isLoggable(Level.SEVERE))
	  JMsgProcess.getLogger().log(Level.SEVERE,Thread.currentThread().getName()+": got message "+msg+" was waiting for lock_down message");
	throw new RuntimeException();
      }
    }
  }
  
  private static <T,U> Offer<T,U> newOffer(Action<T,U> action, long timestamp, int i) {
    return new Offer<T,U>(action,timestamp,i);
  }
  
  private static <T,U> void offer(Offer<T,U> offer) {
    offer.getAction().getBasicAction().getChannel().offer(offer);
  }
  
  private static <T,U> void acceptLockDown(Offer<T,U> offer) {
    offer.getAction().getBasicAction().getChannel().acceptLockDown(offer);
  }
  
  private static <T,U> void cancelOffer(Offer<T,U> offer) {
    offer.getAction().getBasicAction().getChannel().cancelOffer(offer);
  }
}
