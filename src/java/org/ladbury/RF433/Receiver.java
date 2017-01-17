package org.ladbury.RF433;
import com.pi4j.io.gpio.*;
import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

/**
 * Receiver is a simple class to receive changing waveforms and store them for interpretation
 * Created by GJWood on 17/01/2017.
 */
public class Receiver implements GpioPinListenerDigital,Runnable
{
    private int ReceiveTolerance = 60;         //percentage variation in pulse width allowed
    private final int MIN_MESSAGE_SEPARATION_TIME = 4300; //minimum gap between messages in microseconds
    private final int MESSAGE_STORAGE_CAPACITY = 1000;
    private final int MIN_MESSAGE_SIZE = 7; //two bits sync four bits message
    private final int MAX_MESSAGE_SIZE = 512; //Arbitrary number
    private final int EVENT_STORAGE_CAPACITY = MAX_MESSAGE_SIZE;

    private final GpioPinDigitalInput receivePin;
    private ArrayList<RF433Event> events;
    private ArrayList<ArrayList<RF433Event>> messages;
    private volatile boolean newMessage;
    private int lastMessageRead;
    private boolean interrupted;
    private Thread decoder;


    private long lastTime;

    public Receiver(GpioPinDigitalInput receivePin, ArrayList<ArrayList<RF433Event>> messages)
    {
        this.receivePin = receivePin;
        this.messages = messages;
        this.lastTime = 0;
        this.events = new ArrayList<>(EVENT_STORAGE_CAPACITY);
        this.messages = new ArrayList<>(MESSAGE_STORAGE_CAPACITY);
        this.newMessage = false;
        this.lastMessageRead = -1;
        this.interrupted = false;
        this.decoder = new Thread(this);
        enableReceive();
        decoder.start();
    }

    public void shutdown()
    {
        interrupted = true;
        disableReceive();
    }

    /**
     * EnableReceive    -   plug in the interrupt handler
     *
     */
    public void enableReceive()
    {
        this.receivePin.addListener(this);
        System.out.println("Listener added");
    }

    /**
     * DisableReceive     -   unplug in the interrupt handler
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
            if (events.size()>= MIN_MESSAGE_SIZE) messages.add(events); // save onlt reasonable length messages
            events = new ArrayList<>(EVENT_STORAGE_CAPACITY);
        } else
        {
            if (events.size() >= MAX_MESSAGE_SIZE)
            {
                System.err.println("Event overflow, fragment stored");
                messages.add(events);
                events = new ArrayList<>(EVENT_STORAGE_CAPACITY);
            }
        }
        events.add( new RF433Event(pinEvent,duration)); // save the current event
        lastTime = time;
    }

    /**
     * diff     -   Helper function
     * @param A     nunber
     * @param B     number
     * @return      returns positive difference between two numbers
     */
    static /*inline*/ /*unsigned*/ int diff(int A, int B){return Math.abs(A - B);}

    /**
     * Run  -   The decoding loop
     */
    @Override
    public void run()
    {
        while(!interrupted)
        {
            try
            {
                TimeUnit.MILLISECONDS.sleep(100);
            } catch (InterruptedException e)
            {
                interrupted = true;
                break;
            }
            if (newMessage)
            {
                newMessage=false;
                for(int i=Math.max(0,lastMessageRead); i<messages.size();i++)
                {
                    processMessage(i);
                }
            }
        }
    }

    /**
     * processMessage       -   Attempt to decode the message
     * @param messageNumber -   index to the messages store
     */
    void processMessage(int messageNumber)
    {
        ArrayList<RF433Event> msg = messages.get(messageNumber);
    }
}

class RF433Event
{
    GpioPinDigitalStateChangeEvent gpioEvent;
    long duration;

    RF433Event(GpioPinDigitalStateChangeEvent e, long d)
    {gpioEvent=e; duration=d;}
}
