package es.upm.babel.cclib.jmsg;

import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import org.junit.*;
import org.junit.Assert;
import org.junit.runners.*;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import org.junit.rules.Timeout;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.logging.Level;

public class Tests {
  static ThreadGroup tg;
  static volatile int raisedException;

  public Tests() { }
  
  @Rule
  public Timeout timeout = new Timeout(20000, TimeUnit.MILLISECONDS);

  
  @Test
  public void test_2rcv() {
    //JMsgProcess.getLogger().setLevel(Level.FINEST);
    SynchronousChannel<Integer> ch = new SynchronousChannel<Integer>();

    class Sender implements Runnable {
      private int value;
      public Sender(int value) { this.value = value; }
      public void run() { Command.send(ch,value); }
    }

    new Thread(tg,new Sender(1),"sender1").start();
    new Thread(tg,new Sender(2),"sender2").start();

    int r1 = Command.receive(ch);
    int r2 = Command.receive(ch);

    assertThat(r1, either(is(1)).or(is(2)));
    assertThat(r2, either(is(1)).or(is(2)));
    assertThat(r1 - r2, not(is(0)));
    sleep(100);
    assertThat(raisedException,is(0));
  }

  
  @Test
  public void test_rcv_cond1() {
    SynchronousChannel<Integer> ch = new SynchronousChannel<Integer>();

    class Sender implements Runnable {
      private int value;
      public Sender(int value) { this.value = value; }
      public void run() { Command.send(ch,value); }
    }

    new Thread(tg,new Sender(1),"sender1").start();
    new Thread(tg,new Sender(2),"sender2").start();
    new Thread(tg,new Sender(3),"sender3").start();
    int r1 = Command.receiveWithGuard(ch,value -> value==2);
    assertThat(r1, is(2));
    sleep(100);
    assertThat(raisedException,is(0));
  }

  
  @Test
  public void test_rcv_cond2() {
    SynchronousChannel<Integer> ch = new SynchronousChannel<Integer>();

    class Sender implements Runnable {
      private int value;
      public Sender(int value) { this.value = value; }
      public void run() { Command.send(ch,value); }
    }

    new Thread(tg,new Sender(3),"sender3").start();
    new Thread(tg,new Sender(1),"sender1").start();
    new Thread(tg,new Sender(2),"sender2").start();

    int r1 = Command.receiveWithGuard(ch,value -> value != 2);
    assertThat(r1, either(is(1)).or(is(3)));
    sleep(100);
    assertThat(raisedException,is(0));
  }

  
  @Test
  public void test_synch_conflicting() {
    SynchronousChannel<Integer> ch1 = new SynchronousChannel<Integer>("ch1");
    SynchronousChannel<Integer> ch2 = new SynchronousChannel<Integer>("ch2");
    SynchronousChannel<Integer> chRep = new SynchronousChannel<Integer>("ch2");

    class Agent implements Runnable {
      private SynchronousChannel<Integer> ch1;
      private SynchronousChannel<Integer> ch2;
      private SynchronousChannel<Integer> reportCh;
      private int value;
      private int result;
      
      public Agent(SynchronousChannel<Integer> ch1,
                   SynchronousChannel<Integer> ch2,
                   SynchronousChannel<Integer> reportCh, int value) {
        this.value = value;
        this.ch1 = ch1;
        this.ch2 = ch2;
        this.reportCh = reportCh;
      }

      public void run() {
        List<Action<?,Integer>> selectArgs =
          Arrays.asList(Action.send(ch1, value),Action.receive(ch2));
        Command.send(reportCh,Command.select(selectArgs));
      }
    }

    Thread tg1 = new Thread(tg,new Agent(ch1,ch2,chRep,1),"ag1");
    Thread tg2 = new Thread(tg,new Agent(ch2,ch1,chRep,2),"ag2");
    tg1.start(); tg2.start();

    Integer r1r = Command.receive(chRep);
    Integer r2r = Command.receive(chRep);

    assertThat(r1r, either(is(1)).or(is(2)));
    assertThat(r2r, either(is(1)).or(is(2)));
    assertThat(Math.abs(r1r-r2r),is(0));
    sleep(100); 
    assertThat(raisedException,is(0));
    assertThat(JMsgProcess.getQueue(tg1.getId()).isEmpty(),is(true));
    assertThat(JMsgProcess.getQueue(tg2.getId()).isEmpty(),is(true));
  }

  
  @Test
  public void test_select_types1() {
    SynchronousChannel<Integer> ch1 = new SynchronousChannel<Integer>("ch1");
    SynchronousChannel<String> ch2 = new SynchronousChannel<String>("ch2");

    class Sender1 implements Runnable {
      private SynchronousChannel<Integer> ch;
      private int value;
      public Sender1(SynchronousChannel<Integer> ch, int value) { this.ch = ch; this.value = value; }
      public void run() { Command.send(ch,value); }
    }

    class Sender2 implements Runnable {
      private SynchronousChannel<String> ch;
      private String value;
      public Sender2(SynchronousChannel<String> ch, String value) { this.ch = ch; this.value = value; }
      public void run() { Command.send(ch,value); }
    }

    new Thread(tg,new Sender2(ch2,"2"),"Sender2").start();
    new Thread(tg,new Sender1(ch1,1),"Sender1").start();

    List<Action<?,Integer>> selectArgs =
      Arrays.asList(Action.receive(ch1),Action.receive(ch2, value -> Integer.parseInt(value)));
    Integer resultInt = Command.select(selectArgs);

    Assert.assertTrue(resultInt != null);
    assertThat(resultInt, either(is(1)).or(is(2)));
    sleep(100); 
    assertThat(raisedException,is(0));
  }

  
  @Test
  public void test_select_types2() {
    SynchronousChannel<Integer> ch1 = new SynchronousChannel<Integer>("ch1");
    SynchronousChannel<String> ch2 = new SynchronousChannel<String>("ch2");

    class Sender1 implements Runnable {
      private SynchronousChannel<Integer> ch;
      private int value;
      public Sender1(SynchronousChannel<Integer> ch, int value) { this.ch = ch; this.value = value; }
      public void run() { Command.send(ch,value); }
    }

    class Sender2 implements Runnable {
      private SynchronousChannel<String> ch;
      private String value;
      public Sender2(SynchronousChannel<String> ch, String value) { this.ch = ch; this.value = value; }
      public void run() { Command.send(ch,value); }
    }

    //new Thread(tg,new Sender2(ch2,"2"),"Sender2").start();
    new Thread(tg,new Sender1(ch1,1),"Sender1").start();

    List<Action<?,Integer>> selectArgs =
      Arrays.asList(Action.receive(ch1),Action.receive(ch2, value -> Integer.parseInt(value)));
    Integer resultInt = Command.select(selectArgs);

    Assert.assertTrue(resultInt != null);
    assertThat(resultInt, is(1));
    sleep(100); 
    assertThat(raisedException,is(0));
  }

  
  @Test
  public void test_select_types3() {
    SynchronousChannel<Integer> ch1 = new SynchronousChannel<Integer>("ch1");
    SynchronousChannel<String> ch2 = new SynchronousChannel<String>("ch2");

    class Sender1 implements Runnable {
      private SynchronousChannel<Integer> ch;
      private int value;
      public Sender1(SynchronousChannel<Integer> ch, int value) { this.ch = ch; this.value = value; }
      public void run() { Command.send(ch,value); }
    }

    class Sender2 implements Runnable {
      private SynchronousChannel<String> ch;
      private String value;
      public Sender2(SynchronousChannel<String> ch, String value) { this.ch = ch; this.value = value; }
      public void run() { Command.send(ch,value); }
    }

    new Thread(tg,new Sender2(ch2,"2"),"Sender2").start();

    List<Action<?,Integer>> selectArgs =
      Arrays.asList(Action.receive(ch1),Action.receive(ch2, value -> Integer.parseInt(value)));
    Integer resultInt = Command.select(selectArgs);

    Assert.assertTrue(resultInt != null);
    assertThat(resultInt, is(2));
    sleep(100); 
    assertThat(raisedException,is(0));
  }


  @Test
  public void test_select_types_with_nulls() {
    SynchronousChannel<Integer> ch1 = new SynchronousChannel<Integer>("ch1");
    SynchronousChannel<String> ch2 = new SynchronousChannel<String>("ch2");

    class Sender1 implements Runnable {
      private SynchronousChannel<Integer> ch;
      private int value;
      public Sender1(SynchronousChannel<Integer> ch, int value) { this.ch = ch; this.value = value; }
      public void run() { Command.send(ch,value); }
    }

    class Sender2 implements Runnable {
      private SynchronousChannel<String> ch;
      private String value;
      public Sender2(SynchronousChannel<String> ch, String value) { this.ch = ch; this.value = value; }
      public void run() { Command.send(ch,value); }
    }

    new Thread(tg,new Sender2(ch2,"2"),"Sender2").start();

    List<Action<?,Integer>> selectArgs =
      Arrays.asList
      (null,
       Action.receive(ch1),
       null,
       Action.receive(ch2, value -> Integer.parseInt(value)),
       null);
    Integer resultInt = Command.select(selectArgs);

    Assert.assertTrue(resultInt != null);
    assertThat(resultInt, is(2));
    sleep(100); 
    assertThat(raisedException,is(0));
  }

  
  @Test
  public void test_select_types4() {
    SynchronousChannel<Integer> ch1 = new SynchronousChannel<Integer>("ch1");
    SynchronousChannel<String> ch2 = new SynchronousChannel<String>("ch2");

    class Sender1 implements Runnable {
      private SynchronousChannel<Integer> ch;
      private int value;
      public Sender1(SynchronousChannel<Integer> ch, int value) { this.ch = ch; this.value = value; }
      public void run() { Command.send(ch,value); }
    }

    class Sender2 implements Runnable {
      private SynchronousChannel<String> ch;
      private String value;
      public Sender2(SynchronousChannel<String> ch, String value) { this.ch = ch; this.value = value; }
      public void run() { Command.send(ch,value); }
    }

    new Thread(tg,new Sender2(ch2,"2"),"Sender2").start();
    new Thread(tg,new Sender1(ch1,1),"Sender1").start();

    List<Action<?,Void>> selectArgs =
      Arrays.asList(Action.receive(ch1, value -> { return null; }),
                    Action.receive(ch2, value -> { return null; }));
    Command.select(selectArgs);

    sleep(100); 
    assertThat(raisedException,is(0));
  }


  
  @Test
  public void test_consumer_producer_using_1buf() {
    SynchronousChannel<Integer> toBufCh = new SynchronousChannel<Integer>("toBuf");
    SynchronousChannel<Integer> fromBufCh = new SynchronousChannel<Integer>("fromBuf");
    SynchronousChannel<Integer> reportCh = new SynchronousChannel<Integer>("report");
    ArrayList<Integer> input = new ArrayList<Integer>(Arrays.asList(22,3,5,22,8,9,-1));
    ArrayList<Integer> output = new ArrayList<Integer>();

    class Producer implements Runnable {
      public void run() {
        for (int i=0; i<input.size(); i++) {
          Command.send(toBufCh,input.get(i));
        }
      }
    }

    class Consumer implements Runnable {
      int value;
      public void run() {
        do {
          value = Command.receive(fromBufCh);
          output.add(value);
        } while (value != -1);
        Command.send(reportCh,-1);
      }
    }

    class Buffer implements Runnable {
      boolean hasValue;
      Integer value;

      public Buffer() {
        hasValue = false;
      }

      public void run() {
        while (true) {
          if (hasValue) {
            Command.send(fromBufCh, value);
          } else {
            value = Command.receive(toBufCh);
          }
          hasValue = !hasValue;
        }
      }
    }
    
    new Thread(tg,new Buffer(),"buffer").start();
    new Thread(tg,new Producer(),"producer").start();
    new Thread(tg,new Consumer(),"consumer").start();
    Command.receive(reportCh);

    assertThat(output, equalTo(input));
    sleep(100); 
    assertThat(raisedException,is(0));
  }


  @Before
  public void setup() throws Exception {
    raisedException = 0;
    tg = new ExceptionHandlingThreadGroup("testing-tg");
    JMsgProcess.getLogger().setLevel(Level.SEVERE);
  }

    private void sleep(long milliseconds) {
	try { Thread.sleep(milliseconds); }
	catch (InterruptedException exc) { };
    }
}

class ExceptionHandlingThreadGroup extends ThreadGroup {
  public ExceptionHandlingThreadGroup(String name) {
        super(name);
    }
  
  @Override public void uncaughtException(Thread t, Throwable e) {
    Tests.raisedException++;
    System.out.println(t.getName()+": "+e);
    e.printStackTrace();
  }
}
  
