package org.ophion.snitch.util;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * EventSinks are an easy way to provide multicast delegates for event handling.
 * <p/>
 * In a nutshell, EventSinks allow you to quickly create event notification
 * points to which end users can subscribe by implementing the IEventHandler
 * interface
 * <p/>
 * Also, please note that most events are dispatch serially. The async dispatch
 * options is only provided for convenience. <br>
 * <p/>
 * How to use the event sink:
 * <p/>
 * <code>
 * public void testDispatch() throws Exception { <br>
 * EventSink es = new EventSink(); <br>
 * int a = es.appendHandler(new CountHandler()); <br>
 * es.dispatch(this,null); <br>
 * }<br>
 * </code>
 *
 * @author Rafael Ferreira <raf@uvasoftware.com>
 */
public class EventSink {
    private ArrayList<IEventHandler> listeners = new ArrayList<IEventHandler>();
    private static Log log = new Log();
    private static ExecutorService es = Executors.newCachedThreadPool();
    private final Object lock = new Object();

    /**
     * Returns the number of handlers currently registered with this event
     *
     * @return
     */
    public int size() {
        return (listeners.size());
    }

    /**
     * Simple worker runnable to assist with dispatching events async
     *
     * @author Rafael Ferreira <raf@uvasoftware.com>
     */
    private class EventSinkWorker implements Runnable {
        EventSink target = null;
        Object eventSource, eventArgs;
        IEventHandler callback = null;

        public EventSinkWorker(EventSink ev, Object eventSource,
                               Object eventArgs, IEventHandler callback) {
            target = ev;
            this.eventArgs = eventArgs;
            this.eventSource = eventSource;
            this.callback = callback;
        }

        public void run() {
            try {
                target.dispatch(eventSource, eventArgs);

                log.fine("calling callback");

                if (callback != null)
                    callback.handle(this, null);

            } catch (RuntimeException e) {
                log.severe("dispatch error:", e);
            }
        }
    }

    /**
     * Subscribe to this event by passing a delegate that implements the
     * IEventHandler interface
     *
     * @param handler the delegate
     * @return the id of the delegate in the delegate queue (use this id to
     *         remove the delegate later)
     */
    public int appendHandler(IEventHandler handler) {
        synchronized (lock) {
            listeners.add(handler);
        }
        return listeners.indexOf(handler);
    }

    /**
     * Dispatches the event queue by calling all the delegates serially
     *
     * @param eventSource the object that triggered the event
     * @param eventArgs   the arguments needed to service the event (note that this is
     *                    in a per event basis)
     * @throws Exception the exception thrown by the event handler - note that if one
     *                   of the events throws an exception, the whole pipeline gets
     *                   killed
     */
    public void dispatch(Object eventSource, Object eventArgs) {

        synchronized (lock) {
            for (IEventHandler i : listeners) {
                try {
                    i.handle(eventSource, eventArgs);
                } catch (StopDispatchException e) {
                    log.info("early chain termination caught", e);
                    return;
                }
            }
        }

    }


    /**
     * Dispatches the event queue asynchronously and calls the callback when
     * done
     *
     * @param eventSource the object that triggered the event
     * @param eventArgs   the arguments needed to service the event (note that this is
     *                    in a per event basis)
     * @throws Exception the exception thrown by the event handler - note that if one
     *                   of the events throws an exception, the whole pipeline gets
     *                   killed
     */
    public void dispatchAsync(final Object eventSource, final Object eventArgs) {

        for (final IEventHandler i : listeners) {
            try {

                es.execute(new Runnable() {
                    @Override
                    public void run() {
                        i.handle(eventSource, eventArgs);
                    }
                });
            } catch (StopDispatchException e) {
                log.info("early chain termination caught", e);
                return;
            }
        }
    }


    /**
     * Removes a handler/delegate identified by this id
     *
     * @param id the handler id
     * @return true if successful
     */
    public synchronized boolean removerHandler(int id) {
        boolean r;
        synchronized (lock) {
            r = listeners.remove(id) != null;
        }

        return r;

    }

}