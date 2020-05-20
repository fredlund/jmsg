import es.upm.babel.cclib.jmsg.*;

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

