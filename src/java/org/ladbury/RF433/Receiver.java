package org.ladbury.RF433;
import com.pi4j.io.gpio.*;
import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;

import java.util.ArrayList;

/**
 * Receiver is a simple class to receive changing waveforms and store them for interpretation
 * Created by GJWood on 17/01/2017.
 */
public class Receiver implements GpioPinListenerDigital
{
    private int ReceiveTolerance = 60;         //percentage variation in pulse width allowed
    private final int MIN_MESSAGE_SEPARATION_TIME = 4300; //minimum gap between messages in microseconds
    private final int EVENT_STORAGE_CAPACITY = 5000;
    private final int MESSAGE_STORAGE_CAPACITY = 1000;


    private final GpioPinDigitalInput receivePin;
    private ArrayList<RF433Event> events;
    private ArrayList<ArrayList<RF433Event>> messages;
    private int eventCount;
    private int messageCount;

    private long lastTime;

    Receiver(GpioPinDigitalInput receivePin, ArrayList<ArrayList<RF433Event>> messages)
    {
        this.receivePin = receivePin;
        this.messages = messages;
        this.lastTime = 0;
        this.eventCount = 0;
        this.messageCount = 0;
        this.events = new ArrayList<>(EVENT_STORAGE_CAPACITY);
        this.messages = new ArrayList<>(MESSAGE_STORAGE_CAPACITY);
    }


    public void enableReceive()
    {
        this.receivePin.addListener(this);
        System.out.println("Listener added");
    }

    /**
     * Disable receiving data
     */
    public void disableReceive()
    {
        this.receivePin.removeListener(this);
        System.out.println("disableReceive: ");
    }

    /**
     * handleGpioPinDigitalStateChangeEvent -   interrupt handler
     *                                          records pin event & time since last event
     * @param pinEvent  the event that caused the interrupt
     */
    @Override
    public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent pinEvent)
    {
        final long time = System.nanoTime()/1000; //micros();
        final /*unsigned*/ int duration = (int)(time - lastTime);

        if (duration > MIN_MESSAGE_SEPARATION_TIME)
        {
            // A long stretch without signal level change occurred. This could
            // be the gap between two transmissions. store events as a message
            messages.add(events);
            events = new ArrayList<>(EVENT_STORAGE_CAPACITY);
        }
        // detect overflow
        if (events.size() >= EVENT_STORAGE_CAPACITY)
        {
            System.err.println("Event overflow");
            messages.add(events);
            events = new ArrayList<>(EVENT_STORAGE_CAPACITY);
        }
        events.add( new RF433Event(pinEvent,duration));
        lastTime = time;
    }

    /**
     * diff     -   Helper function
     * @param A     nunber
     * @param B     number
     * @return      returns positive difference between two numbers
     */
    static /*inline*/ /*unsigned*/ int diff(int A, int B){return Math.abs(A - B);}
}

class RF433Event
{
    GpioPinDigitalStateChangeEvent gpioEvent;
    long duration;

    RF433Event(GpioPinDigitalStateChangeEvent e, long d)
    {gpioEvent=e; duration=d;}
}
