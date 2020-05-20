# A typed message passing library for Java
The JMsg library enables application to send synchronous messages between processes
  over synchronous communication channels.

 Synchronous Communication Channels (implemented by the class SynchronousChannel)
 are typed using Java Generics, i.e., it is possible
 to restrict the values that can sent over a channel, and the Java type system guarantees
 that only well-typed values can be sent and received over a channel.
 In a sense, this library implements a very simple CCS-like process algebra (due to Robin Milner),
 without  restriction and relabelling, and where internal actions cannot be included in the
 choice construct.
 
 Synchronous Communication Channels are binary, assymetric and synchronous, i.e., a communication happens
 only when there is a thread offering to send a value over a channel (ch), 
 and another thread offers to receive a value sent over the channel (ch).
 Moreover, a channel is independent of the thread that created it. That is, any thread may receive
 values over any channel, and any thread may send values over any channel.

 The library implements a select method, which returns when exactly one
 of the offers (to send or receive values) has resulted in a synchronous communication.
 Note that the argument to the select method (a list of communication offers) may concern
 offers over differently typed communication channels (see the method select in the Command class for
details).

## An example

Below we show a small self-contained example. When the main method is invoked, two additional threads
are created, which attempt to send messages (the Integers 1 and 10) 
to the thread executing the main method. However, the receiving thread accepts only values less than
10, thus ensuring that the thread sending the value 1 will synchronize with the main thread,
while the thread sending the value 10 will wait forever to synchronize:

```` java
import es.upm.babel.jmsg.*;

public class Example {
  static class Sender implements Runnable {
    private SynchronousChannel<Integer> ch;
    private int value;
    public Sender(SynchronousChannel<Integer> ch, int value) { this.ch = ch; this.value = value; }
    public void run() { Command.send(ch,value); }
  }

  public static void main(String args[]) {
    SynchronousChannel<Integer> ch = new SynchronousChannel<>();
 
    new Thread(new Sender(ch,1)).start();
    new Thread(new Sender(ch,10)).start();

    Integer result = Command.receiveWithGuard(ch,value -> value<10);
    System.out.println("The value received is "+result);
  }
}
  ````


