package com.devicehive.messages.bus;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.devicehive.model.Message;

/**
 * DeferredResponse represents the result of {@link MessageBus#subscribe(com.devicehive.messages.MessageType, com.devicehive.messages.MessageDetails)} command.
 * It used in cases when poll requests in non-permanent connection are performed.
 * How to use: after messageBus.poll() invoked you have deferred instance.
 * 
 * Check for deferred.messages().size() - if size > 0 just return list of messages and that is all.
 * If there are no messages, but you want to wait for messages while timeout, use deferred.pollLock() and deferred.hasMessages() objects to wait:
 * 
 *  <pre>
 * {@code}
 *  DeferredResponse deferred = messageBus.poll(..., long timeout, ...);
 *  if (!deferred.messages().isEmpty()) {
 *      return deferred.messages();
 *  } else {
 *      Lock lock = deferred.pollLock();
 *      Condition hasMessages = deferred.hasMessages();
 *      
 *      lock.lock();
 *      
 *      try {
 *          while (deferred.messages().isEmpty()) {
 *              try {
 *                  hasMessages.await(timeout, TimeUnit.SECONDS);
 *              } catch (InterruptedException e) {...}
 *          }
 *           
 *          //Here you have messages to return. Please don't use this DeferredResponse anymore. 
 *          hasMessages.signal();
 *      } finally {
 *          lock.unlock();
 *          return deferred.messages();
 *      }
 *  }
 * </pre>
 *  
 * 
 * @author rroschin
 *
 */
public final class DeferredResponse {

    private final CopyOnWriteArrayList<Message> messages;
    private final Lock pollLock;
    private final Condition hasMessages;

    protected DeferredResponse() {
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
