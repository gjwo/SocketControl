package org.ladbury.RCSwitch;
import com.pi4j.io.gpio.*;
import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;
import com.pi4j.wiringpi.*;

/**
 * RadioReceiver    -   class for receiving and decoding radio signals via a GPIO pin
 *
 * ported to java for Raspberry Pi by GJWood on 14/01/2017.
 *
 * RCSwitch - Arduino library for remote control outlet switches
 * Copyright (c) 2011 Suat Özgür.  All right reserved.
 *
 * Contributors:
 * - Andre Koehler / info(at)tomate-online(dot)de
 * - Gordeev Andrey Vladimirovich / gordeev(at)openpyro(dot)com
 * - Skineffect / http://forum.ardumote.com/viewtopic.php?f=2&t=46
 * - Dominik Fischer / dom_fischer(at)web(dot)de
 * - Frank Oltmanns / <first name>.<last name>(at)gmail(dot)com
 * - Andreas Steinel / A.<lastname>(at)gmail(dot)com
 * - Max Horn / max(at)quendi(dot)de
 * - Robert ter Vehn / <first name>.<last name>(at)gmail(dot)com
 * - Johann Richard / <first name>.<last name>(at)gmail(dot)com
 * - Vlad Gheorghe / <first name>.<last name>(at)gmail(dot)com https://github.com/vgheo

 * Project home: https://github.com/sui77/rc-switch/
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, In
 */

public class RadioReceiver implements GpioPinListenerDigital,GpioInterruptListener
{
    private int nReceiverInterrupt;
    // We can handle up to (unsigned long) => 32 bit * 2 H/L changes per bit + 2 for sync
    private final int RCSWITCH_MAX_CHANGES =67;

    private/*unsigned*/ long nReceivedValue = 0;
    private/*unsigned*/ int nReceivedBitLength = 0;
    private/*unsigned*/ int nReceivedDelay = 0;
    private Protocol protocol;
    private int nReceiveTolerance = 60;
    private final /*unsigned*/ int nSeparationLimit = 4300;
    private final int numProto = 6;

    private final int pinNumber;
    private final GpioPinDigitalInput receivePin;
    private volatile GpioPinDigitalStateChangeEvent event;
    private volatile long interruptCount;

    // variables used by interrupt handler code, which persist between interrupts
    private /*unsigned*/ int changeCount = 0;
    private /*unsigned*/ long lastTime = 0;
    private /*unsigned*/ int repeatCount = 0;


    // separationLimit: minimum microseconds between received codes, closer codes are ignored.
    // according to discussion on issue //#14 it might be more suitable to set the separation
    // limit to the same time as the 'low' part of the sync signal for the current protocol.
    private/*unsigned*/ final int[] timings = new int[RCSWITCH_MAX_CHANGES];

    public RadioReceiver(GpioPinDigitalInput receivePin)
    {
        System.out.println("RadioReceiver constructor " + receivePin.toString());
        this.interruptCount =0;
        this.nReceiverInterrupt = -1;
        this.setReceiveTolerance(60);
        this.nReceivedValue = 0;
        this.receivePin = receivePin;
        this.pinNumber = Integer.parseInt(receivePin.getPin().getName().substring(5));
        //this.receivePin.addListener(this);
    }

    /**
     * Enable receiving data
     */
    public void enableReceive(int interrupt)
    {
        System.out.println("enableReceive: "+ interrupt);
        this.nReceiverInterrupt = interrupt;
        enableReceive();
    }

    public void enableReceive()
    {
        if (this.nReceiverInterrupt != -1)
        {
            nReceivedValue = 0;
            nReceivedBitLength = 0;
            //#if defined(RaspberryPi) // Raspberry Pi
            this.receivePin.addListener(this);
            System.out.println("Listener added");
            //GpioInterrupt.addListener(this::pinStateChange); //GJW
            //GpioUtil.setEdgeDetection(pinNumber, GpioUtil.EDGE_BOTH); //GJW
            //wiringPiISR(this.nReceiverInterrupt, INT_EDGE_BOTH, &handleInterrupt);
            //#else // Arduino
            //attachInterrupt(this.nReceiverInterrupt, handleInterrupt, CHANGE);
            //#endif
        }
    }

    /**
     * Disable receiving data
     */
    public void disableReceive()
    {
        System.out.println("disableReceive: ");
        //#if not defined(RaspberryPi) // Arduino
        //detachInterrupt(this.nReceiverInterrupt);
        //GpioInterrupt.disablePinStateChangeCallback(pinNumber);
        //#endif // For Raspberry Pi (wiringPi) you can't unregister the ISR
        //this.nReceiverInterrupt = -1;
    }

    // getters
    public boolean available(){return nReceivedValue != 0;}
    public /*unsigned*/ long getReceivedValue(){return nReceivedValue;}
    public /*unsigned*/ int getReceivedBitLength(){return nReceivedBitLength;}
    public /*unsigned*/ int getReceivedDelay(){return nReceivedDelay;}
    public /*unsigned*/ Protocol getReceivedProtocol(){return protocol;}
    public /*unsigned*/ int[] getReceivedRawData(){return timings;}
    public long getInterruptCount(){return interruptCount;}
    public int getnReceiverInterrupt()
    {
        return nReceiverInterrupt;
    }

    // setters
    public void resetAvailable(){nReceivedValue = 0;}

    /**
     * Set Receiving Tolerance
     */
    public void setReceiveTolerance(int nPercent)
    {
        //#if not defined( RCSwitchDisableReceiving )
        nReceiveTolerance = nPercent;
        //#endif
    }

    /* helper function for the receiveProtocol method */
    static /*inline*/ /*unsigned*/ int diff(int A, int B){return Math.abs(A - B);}

    /**
     * receiveProtocol
     * @param protocol      -   Protocol to try
     * @param changeCount   -   Number of state changes in the message
     * @return              -   true if using this protocol decoded a message
     */
    boolean /*RECEIVE_ATTR*/ receiveProtocol(Protocol protocol, /*unsigned*/ int changeCount)
    {
        /*unsigned*/ long code = 0;
        //Assuming the longer pulse length is the pulse captured in timings[0]
        final /*unsigned*/ int syncLengthInPulses =  ((protocol.syncFactor.low) > (protocol.syncFactor.high)) ? (protocol.syncFactor.low) : (protocol.syncFactor.high);
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
        final /*unsigned*/ int firstDataTiming = (protocol.invertedSignal) ? (2) : (1);

        for (/*unsigned*/ int i = firstDataTiming; i < changeCount - 1; i += 2) {
            code <<= 1;
            if (diff(timings[i], delay * protocol.zero.high) < delayTolerance &&
                    diff(timings[i + 1], delay * protocol.zero.low) < delayTolerance) {
                // zero
            } else if (diff(timings[i], delay * protocol.one.high) < delayTolerance &&
                    diff(timings[i + 1], delay * protocol.one.low) < delayTolerance) {
                // one
                code |= 1;
            } else {
                // Failed
                return false;
            }
        }

        if (changeCount > 7) {    // ignore very short transmissions: no device sends them, so this must be noise
            this.nReceivedValue = code;
            this.nReceivedBitLength = (changeCount - 1) / 2;
            this.nReceivedDelay = delay;
            this.protocol = protocol;
            return true;
        }
        return false;
    }

    @Override
    public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent event) {
        this.event = event;
        interruptCount++;
        //System.out.println(event.toString());
        handleInterrupt();
    }

    @Override
    public void pinStateChange(GpioInterruptEvent event)
    {
        //this.event = event;
        //System.out.println(event.toString());
        handleInterrupt();
    }

    private void /*RECEIVE_ATTR*/ handleInterrupt()
    {
        final long time = System.nanoTime()/1000; //micros();
        final /*unsigned*/ int duration = (int)(time - lastTime);

        if (duration > nSeparationLimit)
        {
            // A long stretch without signal level change occurred. This could
            // be the gap between two transmissions.
            if (diff(duration, timings[0]) < 200)
            {
                // This long signal is close in length to the long signal which
                // started the previously recorded timings; this suggests that
                // it may indeed by a a gap between two transmissions (we assume
                // here that a sender will send the signal multiple times,
                // with roughly the same gap between them).
                repeatCount++;
                if (repeatCount == 2)
                {
                    for(Protocol pr: Protocol.values())
                    {
                        if (receiveProtocol(pr, changeCount)){break;}
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