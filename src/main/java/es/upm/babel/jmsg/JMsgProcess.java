package es.upm.babel.cclib.jmsg;

import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.SimpleFormatter;
import java.util.logging.LogRecord;


class JMsgProcess {

  private final static Map<Long,BlockingQueue<Message<?,?>>> queues =
    Collections.synchronizedMap(new HashMap<Long,BlockingQueue<Message<?,?>>>());
  private static volatile Logger logger = Logger.getLogger("es.upm.babel.cclib.JMsgLogger");
  private static ConsoleHandler consoleHandler = null;

  public static BlockingQueue<Message<?,?>> getQueue() {
    return getQueue(Thread.currentThread().getId());
  }

  public static BlockingQueue<Message<?,?>> getQueue(long threadId) {
    synchronized (queues) {
      BlockingQueue<Message<?,?>> queue = queues.get(threadId);
      if (queue == null) {
        queue = new LinkedBlockingQueue<Message<?,?>>();
        queues.put(threadId,queue);
      }
      return queue;
    }
  }

  public static String threadName() {
    return "thread "+Thread.currentThread().getName()+"("+Thread.currentThread().getId()+")";
  }

  public static Message<?,?> receive() {
    BlockingQueue<Message<?,?>> myQueue = getQueue();
    try {
      Message<?,?> msg = myQueue.take();
      if (logger.isLoggable(Level.FINE))
        logger.log(Level.FINE,threadName()+" got message "+msg);
      return msg;
    } catch (InterruptedException sexc) {
      Thread.currentThread().interrupt();
      throw new RuntimeException(sexc);
    }
  }

  public static void send(Object sender, long threadId, Message<?,?> msg) {
    try {
      if (logger.isLoggable(Level.FINE))
        logger.log(Level.FINE,sender+": "+msg+" => "+threadId);
      getQueue(threadId).put(msg);
    } catch (InterruptedException rexc) {
      Thread.currentThread().interrupt();
      throw new RuntimeException(rexc);
    }
  }

  public static Logger getLogger() {
    Level logLevel = logger.getLevel();
    if (logLevel != null) {
      synchronized (logger) {
        if (consoleHandler == null) {
          for (Handler handler : logger.getHandlers()) {  logger.removeHandler(handler);}
          ConsoleHandler consoleHandler = new ConsoleHandler();
          consoleHandler.setFormatter(new SimpleFormatter() {
              private static final String format = "[%1$-7s] %2$s %n";

              @Override
              public synchronized String format(LogRecord lr) {
                return String.format(format,
                                     lr.getLevel().getLocalizedName(),
                                     lr.getMessage()
                                     );
              }
            });
          consoleHandler.setLevel(logLevel);
          logger.addHandler(consoleHandler);
        }
      }
    }
    return logger;
  }
}
