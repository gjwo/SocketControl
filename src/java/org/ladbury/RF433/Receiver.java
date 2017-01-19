package org.ladbury.RF433;
import com.pi4j.io.gpio.*;
import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;

import java.time.Instant;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.ladbury.RCSwitch.Protocol;


/**
 * Receiver is a simple class to receive changing waveforms and store them for interpretation
 * Created by GJWood on 17/01/2017.
 */
public class Receiver implements GpioPinListenerDigital,Runnable
{
    private int PULSE_WIDTH_TOLERANCE = 30; //percentage variation in pulse width allowed
    private final int MIN_MESSAGE_SEPARATION_TIME = 4300; //minimum gap between rawMessages in microseconds
    private final int MESSAGE_STORAGE_CAPACITY = 1000;
    public static final int MIN_MESSAGE_SIZE = 6; //two bits sync + four bits message
    public static final int MAX_MESSAGE_SIZE = 66; // limit on long (64bits) => 32 bit * 2 H/L changes per bit + 2 for sync

    private final GpioPinDigitalInput receivePin;
    private RawMessage rawMessage;
    private volatile boolean newMessage;
    private int lastMessageRead;
    private boolean interrupted;
    private Thread decoder;
    private volatile CircularFifoQueue <RawMessage> rawMessages;
    private long rawMessageCount;
    private CircularFifoQueue<DecodedMessage> decodedMessages;

    private long lastTime;

    public Receiver(GpioPinDigitalInput receivePin)
    {
        this.receivePin = receivePin;
        this.lastTime = 0;
        this.rawMessage = new RawMessage(MAX_MESSAGE_SIZE);
        this.rawMessages = new CircularFifoQueue<>(MESSAGE_STORAGE_CAPACITY);
        this.rawMessageCount = 0;
        this.decodedMessages = new CircularFifoQueue<>(MESSAGE_STORAGE_CAPACITY);
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
        System.out.println("Listener removed");
        interrupted = true;
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
        final int duration = (int)(time - lastTime);

        if (duration > MIN_MESSAGE_SEPARATION_TIME)
        {
            // A long stretch without signal level change occurred. This could
            // be the gap between two transmissions. store events as a message
            if (rawMessage.events.size()>= MIN_MESSAGE_SIZE)
            {
                rawMessages.add(rawMessage); // save only reasonable length rawMessages
                rawMessageCount++;
                newMessage =true;
            }
            rawMessage= new RawMessage(MAX_MESSAGE_SIZE);
        } else
        {
            if (rawMessage.events.size() >= MAX_MESSAGE_SIZE)
            {
                rawMessage.events.clear(); //throw fragment away
            }
        }
        rawMessage.events.add( new RF433Event(pinEvent,duration)); // save the current event
        lastTime = time;
    }

    /**
     * diff     -   Helper function
     * @param A     nunber
     * @param B     number
     * @return      returns positive difference between two numbers
     */
    static int diff(int A, int B){return Math.abs(A - B);}

    /**
     * processMessage   -   Attempt to decode the message
     * @param msg       -   a raw message
     */
    boolean processMessage(RawMessage msg)
    {
        boolean matched = false;
        for(Protocol pr: Protocol.values())
        {
            matched = processsMsgWithProtocol(pr, msg);
            if (matched){break;}
        }
        return matched;
    }

    /**
     * processsMsgWithProtocol  -   Attempt to decode a raw messages using the specified protocol
     * @param protocol          -   A specification of sync and bit encoding timings
     * @param rawMessage               -   the raw message
     * @return                  -   true if decoded successfully, decoded message added to store
     */
    boolean processsMsgWithProtocol(Protocol protocol, RawMessage rawMessage)
    {
        if (rawMessage == null) return false;
        if (rawMessage.events.size() < MIN_MESSAGE_SIZE) return false; // ignore very short transmissions: no device sends them, so this must be noise
        long code = 0;
        //Assuming the longer pulse length is the pulse captured in timings[0]
        final int syncLengthInPulses =  ((protocol.syncFactor.low) > (protocol.syncFactor.high)) ? (protocol.syncFactor.low) : (protocol.syncFactor.high);
        final int pulseWidth = rawMessage.events.get(0).duration / syncLengthInPulses;
        final int pulseWidthTolerance = pulseWidth * PULSE_WIDTH_TOLERANCE / 100;

        /* For protocols that start low, the sync period looks like
         *               _________
         * _____________|         |XXXXXXXXXXXX|
         *
         * |--1st dur--|-2nd dur-|-Start data-|
         *
         * The 3rd saved duration starts the data.
         *
         * For protocols that start high, the sync period looks like
         *
         *  ______________
         * |              |____________|XXXXXXXXXXXXX|
         *
         * |-filtered out-|--1st dur--|--Start data--|
         *
         * The 2nd saved duration starts the data
         */
        final int firstDataTiming = (protocol.invertedSignal) ? (2) : (1);
        DecodedMessage dMsg = new DecodedMessage(protocol.name(),pulseWidth);
        for (int i = firstDataTiming; i < rawMessage.events.size() - 1; i += 2)
        {
            int bit1Dur = rawMessage.events.get(i).duration;
            int bit2Dur = rawMessage.events.get(i+1).duration;
            //check each pair of bits matches the protocol definition for
            if (Math.abs( bit1Dur - pulseWidth * protocol.zero.high) < pulseWidthTolerance &&
                    Math.abs(bit2Dur - pulseWidth * protocol.zero.low) < pulseWidthTolerance)
            {
                // matched bit 0
                dMsg.addBit(false);
            } else if (Math.abs(bit1Dur - pulseWidth * protocol.one.high) < pulseWidthTolerance &&
                    Math.abs(bit2Dur - pulseWidth * protocol.one.low) < pulseWidthTolerance)
            {
                // matched bit 1
                dMsg.addBit(true);
            } else
            {
                return false; // Failed, out of spec bit pair cannot be translated
            }
        }
        if (dMsg.size() >= MIN_MESSAGE_SIZE) {
            decodedMessages.add(dMsg);
            return true;
        }
        return false;
    }

    /**
     * Run  -   The decoding loop
     */
    @Override
    public void run()
    {
        boolean decoded;
        DecodedMessage dMsg;
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
                for(RawMessage message: rawMessages)
                {
                    decoded = processMessage(message);
                    if (decoded)
                    {
                        dMsg = decodedMessages.get(decodedMessages.size()-1);
                        rawMessages.remove(message);
                        System.out.printf(dMsg.toString());
                    }
                }
            }
        }
        System.out.println("Decoding stopped");
        System.out.printf("Summary - Decoded Messages: %d, Raw rawMessages: %d%n", rawMessageCount,decodedMessages.size());
        for(DecodedMessage d:decodedMessages) System.out.println(d.toString());
        System.out.println("Messages not decoded");
        for(RawMessage r:rawMessages)
        {
            System.out.println(r.toString());
        }
    }
}

class RF433Event
{
    GpioPinDigitalStateChangeEvent gpioEvent;
    int duration;

    RF433Event(GpioPinDigitalStateChangeEvent e, int d)
    {gpioEvent=e; duration=d;}

    @Override
    public String toString()
    {
        return gpioEvent.getEdge().toString()+" "+ duration;

    }
}

class RawMessage
{
    final ArrayList<RF433Event> events;
    final Instant receivedTime;

    RawMessage(int size)
    {
        events = new ArrayList<RF433Event>(size);
        receivedTime = Instant.now();
    }
    @Override
    public String toString()
    {
        String s = new String();
        for (RF433Event e: events)
        {
            s = s+e.duration+" "+e.gpioEvent.getState().toString().substring(0,2)+" ";
        }
        return s;
    }
}
class DecodedMessage
{
    final String protocolName;
    final int pulseWidth;
    int numberOfBits;
    long code;
    String codeString;

    DecodedMessage(String protocolName,int pulseWidth)
    {
        this.protocolName = protocolName;
        this.pulseWidth = pulseWidth;
        this.numberOfBits = 0;
        this.code = 0;
        this.codeString = new String();
    }

    @Override
    public String toString()
    {
        return String.format("%s (PulseWidth %d) %d bits code %d 0x%h %s %n",
                protocolName,pulseWidth,numberOfBits,code,code,codeString);
    }

    public void addBit(boolean bit)
    {
        codeString = codeString + ((bit)?"1":"0");
        code = ((code<<1) | ((bit)?1:0));
        numberOfBits++;
    }

    public int size() {return codeString.length();}
}
