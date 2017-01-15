package org.ladbury.sockets;

/**
* Created by GJWood on 15/01/2017.
*/ /* Format for protocol definitions:
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
