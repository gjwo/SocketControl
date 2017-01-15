package org.ladbury.sockets;
import com.pi4j.io.gpio.*;
import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;
import com.pi4j.wiringpi.GpioInterrupt;


/**
 * RadioReceiver    -   class for receiving and decoding radio signals via a GPIO pin
 * Created by GJWood on 15/01/2017.
 */
public class RadioReceiver implements GpioPinListenerDigital
{
    private int nReceiverInterrupt;
    // We can handle up to (unsigned long) => 32 bit * 2 H/L changes per bit + 2 for sync
    private final int RCSWITCH_MAX_CHANGES =67;
    private/*unsigned*/ long nReceivedValue = 0;
    private/*unsigned*/ int nReceivedBitlength = 0;
    private/*unsigned*/ int nReceivedDelay = 0;
    private/*unsigned*/ int nReceivedProtocol = 0;
    private Protocol protocol;
    private Protocol pro;
    private int nReceiveTolerance = 60;
    private final /*unsigned*/ int nSeparationLimit = 4300;
    private int numProto;

    private final int pinNumber;
    public int getnReceiverInterrupt()
    {
        return nReceiverInterrupt;
    }
    private final GpioPinDigitalInput receivePin;
    private volatile GpioPinDigitalStateChangeEvent event;

    // separationLimit: minimum microseconds between received codes, closer codes are ignored.
    // according to discussion on issue //#14 it might be more suitable to set the separation
    // limit to the same time as the 'low' part of the sync signal for the current protocol.
    private/*unsigned*/ volatile int[] timings = new int[RCSWITCH_MAX_CHANGES];

    RadioReceiver(GpioPinDigitalInput receivePin)
    {
        this.nReceiverInterrupt = -1;
        this.setReceiveTolerance(60);
        this.nReceivedValue = 0;
        this.receivePin = receivePin;
        this.pinNumber = Integer.parseInt(receivePin.getPin().getName());
        //this.receivePin.addListener(this);
    }


    //#if not defined( RCSwitchDisableReceiving )
    /**
     * Enable receiving data
     */
    void enableReceive(int interrupt)
    {
        this.nReceiverInterrupt = interrupt;
        this.enableReceive();
    }

    void enableReceive()
    {
        if (this.nReceiverInterrupt != -1) {
            nReceivedValue = 0;
            nReceivedBitlength = 0;
            //#if defined(RaspberryPi) // Raspberry Pi
            this.receivePin.addListener(this);
            //wiringPiISR(this.nReceiverInterrupt, INT_EDGE_BOTH, &handleInterrupt);
            //#else // Arduino
            //attachInterrupt(this.nReceiverInterrupt, handleInterrupt, CHANGE);
            //#endif
        }
    }

    /**
     * Disable receiving data
     */
    void disableReceive()
    {
        //#if not defined(RaspberryPi) // Arduino
        //detachInterrupt(this.nReceiverInterrupt);
        GpioInterrupt.disablePinStateChangeCallback(pinNumber);
        //#endif // For Raspberry Pi (wiringPi) you can't unregister the ISR
        this.nReceiverInterrupt = -1;
    }

    // getters
    boolean available(){return nReceivedValue != 0;}
    /*unsigned*/ long getReceivedValue(){return nReceivedValue;}
    /*unsigned*/ int getReceivedBitlength(){return nReceivedBitlength;}
    /*unsigned*/ int getReceivedDelay(){return nReceivedDelay;}
    /*unsigned*/ int getReceivedProtocol(){return nReceivedProtocol;}
    /*unsigned*/ int[] getReceivedRawdata(){return timings;}

    // setters
    void resetAvailable(){nReceivedValue = 0;}

    /**
     * Set Receiving Tolerance
     */
    void setReceiveTolerance(int nPercent)
    {
        //#if not defined( RCSwitchDisableReceiving )
        nReceiveTolerance = nPercent;
        //#endif
    }

    /* helper function for the receiveProtocol method */
    static /*inline*/ /*unsigned*/ int diff(int A, int B){return Math.abs(A - B);}

    /**
     * receiveProtocol
     * @param pn            -   Protocol number
     * @param changeCount   - ?
     * @return              - true if ...
     */
    boolean /*RECEIVE_ATTR*/ receiveProtocol(final int pn, /*unsigned*/ int changeCount)
    {
        this.pro = Protocol.protocol1;
        for(Protocol pr: Protocol.values())
        {
            if (pr.protocolNumber == pn){ this.pro = pr; break;}
        }


        /*unsigned*/ long code = 0;
        //Assuming the longer pulse length is the pulse captured in timings[0]
        final /*unsigned*/ int syncLengthInPulses =  ((pro.syncFactor.low) > (pro.syncFactor.high)) ? (pro.syncFactor.low) : (pro.syncFactor.high);
        final /*unsigned*/ int delay = timings[0] / syncLengthInPulses;
        final /*unsigned*/ int delayTolerance = delay * nReceiveTolerance / 100;

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
        final /*unsigned*/ int firstDataTiming = (pro.invertedSignal) ? (2) : (1);

        for (/*unsigned*/ int i = firstDataTiming; i < changeCount - 1; i += 2) {
            code <<= 1;
            if (diff(timings[i], delay * pro.zero.high) < delayTolerance &&
                    diff(timings[i + 1], delay * pro.zero.low) < delayTolerance) {
                // zero
            } else if (diff(timings[i], delay * pro.one.high) < delayTolerance &&
                    diff(timings[i + 1], delay * pro.one.low) < delayTolerance) {
                // one
                code |= 1;
            } else {
                // Failed
                return false;
            }
        }

        if (changeCount > 7) {    // ignore very short transmissions: no device sends them, so this must be noise
            nReceivedValue = code;
            nReceivedBitlength = (changeCount - 1) / 2;
            nReceivedDelay = delay;
            nReceivedProtocol = pn;
            return true;
        }

        return false;
    }

    @Override
    public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent event) {
        this.event = event;
        handleInterrupt();
    }


    void /*RECEIVE_ATTR*/ handleInterrupt()
    {

        /*static*/ /*unsigned*/ int changeCount = 0;
        /*static*/ /*unsigned*/ long lastTime = 0;
        /*static*/ /*unsigned*/ int repeatCount = 0;

        final long time = System.nanoTime()/1000; //micros();
        final /*unsigned*/ int duration = (int)(time - lastTime);

        if (duration > nSeparationLimit) {
            // A long stretch without signal level change occurred. This could
            // be the gap between two transmission.
            if (diff(duration, timings[0]) < 200) {
                // This long signal is close in length to the long signal which
                // started the previously recorded timings; this suggests that
                // it may indeed by a a gap between two transmissions (we assume
                // here that a sender will send the signal multiple times,
                // with roughly the same gap between them).
                repeatCount++;
                if (repeatCount == 2) {
                    for(/*unsigned*/ int i = 1; i <= numProto; i++) {
                        if (receiveProtocol(i, changeCount)) {
                            // receive succeeded for protocol i
                            break;
                        }
                    }
                    repeatCount = 0;
                }
            }
            changeCount = 0;
        }

        // detect overflow
        if (changeCount >= RCSWITCH_MAX_CHANGES) {
            changeCount = 0;
            repeatCount = 0;
        }

        timings[changeCount++] = duration;
        lastTime = time;
    }
}