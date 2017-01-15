package org.ladbury.sockets;
import com.pi4j.io.gpio.*;
/**
 * ported to java by GJWood on 14/01/2017.
 *
 *   RCSwitch - Arduino libary for remote control outlet switches
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

    class HighLow {
        byte high;
        byte low;
        
        HighLow( byte h, byte l){high = h;low = l;}
    };

    /* Format for protocol definitions:
     * {pulselength, Sync bit, "0" bit, "1" bit}
     *
     * pulselength: pulse length in microseconds, e.g. 350
     * Sync bit: {1, 31} means 1 high pulse and 31 low pulses
     *     (perceived as a 31*pulselength long pulse, total length of sync bit is
     *     32*pulselength microseconds), i.e:
     *      _
     *     | |_______________________________ (don't count the vertical bars)
     * "0" bit: waveform for a data bit of value "0", {1, 3} means 1 high pulse
     *     and 3 low pulses, total length (1+3)*pulselength, i.e:
     *      _
     *     | |___
     * "1" bit: waveform for a data bit of value "1", e.g. {3,1}:
     *      ___
     *     |   |_
     *
     * These are combined to form Tri-State bits when sending or receiving codes.
     *    { 350, {  1, 31 }, {  1,  3 }, {  3,  1 }, false },    // protocol 1
     *    { 650, {  1, 10 }, {  1,  2 }, {  2,  1 }, false },    // protocol 2
     *    { 100, { 30, 71 }, {  4, 11 }, {  9,  6 }, false },    // protocol 3
     *    { 380, {  1,  6 }, {  1,  3 }, {  3,  1 }, false },    // protocol 4
     *    { 500, {  6, 14 }, {  1,  2 }, {  2,  1 }, false },    // protocol 5
     *    { 450, { 23,  1 }, {  1,  2 }, {  2,  1 }, true }      // protocol 6 (HT6P20B)
     */
    enum Protocol
    {      
        protocol1 (1,350,1,31,1,3,3,1,false ),
        protocol2 (2,650,1,10,1,2,2,1,false),
        protocol3 (3,100,30,71,4,11,9,6,false ),
        protocol4 (4,380,1,6,1,3,3,1,false ),
        protocol5 (5,500,6,14,1,2,2,1,false ),
        protocol6 (6,450,23,1,1,2,2,1,true ); // (HT6P20B)

        final int protocolNumber;
        final int pulseLength;
        final HighLow syncFactor;
        final HighLow zero;
        final HighLow one;
        final boolean invertedSignal; //if true inverts the high and low logic levels in the HighLow structs
         
        Protocol(int n,int l, int sfh, int sfl, int zh, int zl, int oh, int ol, boolean inv)
        {
            this.protocolNumber = n;
            this.pulseLength = l;
            this.syncFactor = new HighLow((byte)sfh, (byte)sfl);
            this.zero = new HighLow((byte)zh, (byte) zl);
            this.one = new HighLow((byte)oh, (byte) ol);
            this.invertedSignal = inv;
        }
    }



class RCSwitch
{


    private Protocol proto;
    private Protocol protocol;
    private int numProto = 6; //sizeof(proto) / sizeof(proto[0])


    private int nRepeatTransmit;
    private int nTransmitterPin;
//#endif

    RCSwitch()
    {
        this.nTransmitterPin = -1;
        this.setRepeatTransmit(10);
        this.setProtocol(1);
      //#if not defined( RCSwitchDisableReceiving )
      //#endif
    }

    /**
     * Sets the protocol to send.
     */
    void setProtocol(Protocol protocol)
    {
        this.protocol = protocol;
    }

    /**
     * Sets the protocol to send, from a list of predefined protocols
     */
    void setProtocol(int nProtocol) 
    {
        for(Protocol p: Protocol.values())
        {
            if (p.protocolNumber == nProtocol) this.protocol = p;
            return;
        }
        // TODO: trigger an error, e.g. "bad protocol" ???
        this.protocol = Protocol.protocol1;
    }

    /**
     * Sets the protocol to send with pulse length in microseconds.
     */
    void setProtocol(int nProtocol, int nPulseLength)
    {
        setProtocol(nProtocol);
        this.setPulseLength(nPulseLength);
    }


    /**
     * Sets pulse length in microseconds
     */
    void setPulseLength(int nPulseLength)
    {
        // TODO this.protocol.pulseLength = nPulseLength;
    }

    /**
     * Sets Repeat Transmits
     */
    void setRepeatTransmit(int nRepeatTransmit)
    {
        this.nRepeatTransmit = nRepeatTransmit;
    }




    /**
     * Enable transmissions
     *
     * @param nTransmitterPin    Arduino Pin to which the sender is connected to
     */
    void enableTransmit(int nTransmitterPin)
    {
        this.nTransmitterPin = nTransmitterPin;
        pinMode(this.nTransmitterPin, OUTPUT);
    }

    /**
     * Disable transmissions
     */
    void disableTransmit()
    {
        this.nTransmitterPin = -1;
    }

    /**
     * @param sCodeWord   a binary code word consisting of the letter 0, 1
     */
    void send(final String sCodeWord)
    {
        // turn the tristate code word into the corresponding bit pattern, then send it
        /*unsigned*/ long code = 0;
        /*unsigned*/ int length = 0;
        for (int i = 0; i< sCodeWord.length(); i++) {
            code <<= 1L;
            if (sCodeWord.charAt(i) != '0')
            code |= 1L;
            length++;
        }
        this.send(code, length);
    }

    /**
     * Transmit the first 'length' bits of the integer 'code'. The
     * bits are sent from MSB to LSB, i.e., first the bit at position length-1,
     * then the bit at position length-2, and so on, till finally the bit at position 0.
     */
    void send(/*unsigned*/ long code, /*unsigned*/ int length)
    {
        if (this.nTransmitterPin == -1)
        return;

    //#if not defined( RCSwitchDisableReceiving )
        // make sure the receiver is disabled while we transmit
        int nReceiverInterrupt_backup = nReceiverInterrupt;
        if (nReceiverInterrupt_backup != -1) {
            this.disableReceive();
        }
    //#endif

        for (int nRepeat = 0; nRepeat < nRepeatTransmit; nRepeat++) {
            for (int i = length-1; i >= 0; i--) {
                if ((code & (1L << i))>0)
                    this.transmit(protocol.one);
          else
                this.transmit(protocol.zero);
            }
            this.transmit(protocol.syncFactor);
        }

    //#if not defined( RCSwitchDisableReceiving )
        // enable receiver again if we just disabled it
        if (nReceiverInterrupt_backup != -1) {
            this.enableReceive(nReceiverInterrupt_backup);
        }
    //#endif
    }

    /**
     * Transmit a single high-low pulse.
     */
    void transmit(HighLow pulses)
    {
        byte /*uint8_t*/ firstLogicLevel = (this.protocol.invertedSignal) ? LOW : HIGH;
        byte /*uint8_t*/ secondLogicLevel = (this.protocol.invertedSignal) ? HIGH : LOW;

        digitalWrite(this.nTransmitterPin, firstLogicLevel);
        delayMicroseconds( this.protocol.pulseLength * pulses.high);
        digitalWrite(this.nTransmitterPin, secondLogicLevel);
        delayMicroseconds( this.protocol.pulseLength * pulses.low);
    }

//#endif
}
/*

//#include "RCSwitch.h"

//#ifdef RaspberryPi
    // PROGMEM and _P functions are for AVR based microprocessors,
    // so we must normalize these for the ARM processor:
    //#define PROGMEM
    //#define memcpy_P(dest, src, num) memcpy((dest), (src), (num))
//#endif

//#ifdef ESP8266
    // interrupt handler and related code must be in RAM on ESP8266,
    // according to issue //#46.
    //#define /*RECEIVE_ATTR*/ ICACHE_RAM_ATTR
//#else
    //#define /*RECEIVE_ATTR*/
//#endif


 */