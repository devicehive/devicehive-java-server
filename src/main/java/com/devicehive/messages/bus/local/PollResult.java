package com.devicehive.messages.bus.local;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.devicehive.model.Message;

/**
 * PollResult represents the result of {@link MessageBus#poll(com.devicehive.model.MessageType, java.util.Date, Long, Long)} command.
 * It used in cases when poll requests in non-permanent connection are performed.
 * How to use: after messageBus.poll() invoked you have pollResult instance.
 * 
 * Check for pollResult.messages().size() - if size > 0 just return list of messages and that is all.
 * If there are no messages, but you want to wait for messages while timeout, use pollResult.pollLock() and pollResult.hasMessages() objects to wait:
 * 
 *  <pre>
 * {@code}
 *  PollResult pollResult = messageBus.poll(..., long timeout, ...);
 *  if (!pollResult.messages().isEmpty()) {
 *      return pollResult.messages();
 *  } else {
 *      Lock lock = pollResult.pollLock();
 *      Condition hasMessages = pollResult.hasMessages();
 *      
 *      lock.lock();
 *      
 *      try {
 *          while (pollResult.messages().isEmpty()) {
 *              try {
 *                  hasMessages.await(timeout, TimeUnit.SECONDS);
 *              } catch (InterruptedException e) {...}
 *          }
 *           
 *          //Here you have messages to return. Please don't use this PollResult anymore. 
 *          hasMessages.signal();
 *      } finally {
 *          lock.unlock();
 *          return pollResult.messages();
 *      }
 *  }
 * </pre>
 *  
 * 
 * @author rroschin
 *
 */
public final class PollResult {

    private final CopyOnWriteArrayList<Message> messages;
    private final Lock pollLock;
    private final Condition hasMessages;

    protected PollResult() {
        this.messages = new CopyOnWriteArrayList<>();
        this.pollLock = new ReentrantLock();
        this.hasMessages = this.pollLock.newCondition();
    }

    public CopyOnWriteArrayList<Message> messages() {
        return messages;
    }

    public Lock pollLock() {
        return pollLock;
    }

    public Condition hasMessages() {
        return hasMessages;
    }

}
