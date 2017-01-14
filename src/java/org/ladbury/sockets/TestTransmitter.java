package org.ladbury.sockets;


import com.pi4j.io.gpio.Pin;
import com.pi4j.io.gpio.RaspiPin;
import de.pi3g.pi.rcswitch.*;
import java.util.BitSet;
import java.util.concurrent.TimeUnit;


/**
 * TestTransmitter - tranmit codes using RF 433
 * Created by GJWood on 13/01/2017.
 */
class TestTransmitter
{
    private BitSet address;
    private RCSwitch transmitter;
    private Pin transmitterPin;

    TestTransmitter()
    {
        //our switching group address is 01011 (marked with 1 to 5 on the DIP switch
        //on the switching unit itself)
        this.address = RCSwitch.getSwitchGroupAddress("01011");
        this.transmitterPin = RaspiPin.GPIO_00;
        this.transmitter = new RCSwitch(RaspiPin.GPIO_00);
    }
  void test1()
    {
        System.out.println("switch on");
        transmitter.switchOn(address, 1); //switches the switch unit A (A = 1, B = 2, ...) on
        try
        {
            TimeUnit.SECONDS.sleep(5); //wait 5 sec.
        } catch (InterruptedException ignore) {}
        System.out.println("switch off");
        transmitter.switchOff(address, 1); //switches the switch unit A off
    }
}
