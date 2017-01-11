package org.ladbury.sockets;

import com.pi4j.io.gpio.*;

import java.util.concurrent.TimeUnit;
import java.util.HashMap;

/**
 * Created by GJWood on 09/01/2017.
 *
 */
public class SocketControl
{

    // 4 bits select and control the sockets b3,b2,b1,b0 , setting 0 is pin low, 1 is pin high
    // b3 controls on or off: b3 = 0 is off, b3 = 1 is on
    // b2 controls all sockets / individual sockets b2 = 0 is all, b2 = 0 is individual
    // b1 and b2 select an individual socket the numbering is socket number = inverse b1b0 +1
    // i.e 11(3) = socket 1, 10(2) = socket 2, 01(1) = socket 1 00(0) = socket 4

    enum SocketCode
    {
        ALL(PinState.LOW,PinState.HIGH,PinState.HIGH),
        SOCKET1(PinState.HIGH,PinState.HIGH,PinState.HIGH),
        SOCKET2(PinState.HIGH,PinState.HIGH,PinState.LOW),
        SOCKET3(PinState.HIGH,PinState.LOW,PinState.HIGH),
        SOCKET4(PinState.HIGH,PinState.LOW,PinState.LOW);

        final PinState pinK1;
        final PinState pinK2;
        final PinState pinK3;

        SocketCode(PinState pinK1,PinState pinK2,PinState pinK3)
        {
            this.pinK1 = pinK1;
            this.pinK2 = pinK2;
            this.pinK3 = pinK3;
        }
    }

    enum SocketState
    {
        ON (PinState.HIGH),
        OFF (PinState.LOW);

        final PinState state;

        SocketState(PinState state)
        {
            this.state = state;
        }
    }


    private final GpioPinDigitalOutput pinK0; //All = LOW, individual Socket = High
    private final GpioPinDigitalOutput pinK1; //All = LOW, individual Socket = High
    private final GpioPinDigitalOutput pinK2; // K2-K3: HIGH HIGH = Socket1, HIGH LOW = socket2
    private final GpioPinDigitalOutput pinK3; // K2-K3: LOW HIGH = Socket3, LOW LOW = socket4
    @SuppressWarnings("FieldCanBeLocal")
    private final GpioPinDigitalOutput modulationSelectPin;
    private final GpioPinDigitalOutput modulatorEnablePin;
    private final HashMap <Integer,SocketCode>  socketMap;

    /**
     * SocketControl    -   Constructor, sets up the GPIO pins required to control the sockets
     */
    public SocketControl()
    {
        final GpioController gpio = GpioFactory.getInstance();
        socketMap = new HashMap<>();
        socketMap.put(0,SocketCode.ALL);
        socketMap.put(1,SocketCode.SOCKET1);
        socketMap.put(2,SocketCode.SOCKET2);
        socketMap.put(3,SocketCode.SOCKET3);
        socketMap.put(4,SocketCode.SOCKET4);

        // the pins numbering scheme is different using pi4j
        // GPIO pinout uses the Pi4J/WiringPi GPIO numbering scheme.

        //Select the signal used to enable/disable the modulator
        //was 22 now 06
        this.modulatorEnablePin = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_06, "CE", PinState.LOW);
        this.modulatorEnablePin.setShutdownOptions(true, PinState.LOW);

        // Select the GPIO pin used to select Modulation ASK/FSK
        // Set the modulator to ASK for On Off Keying by setting MODSEL pin low
        //was 18 now 05
        this.modulationSelectPin = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_05, "ASK/FSK", PinState.LOW);
        this.modulationSelectPin.setShutdownOptions(true, PinState.LOW);

        // Select the GPIO pins used for the encoder K0-K3 data inputs
        // and initialise K0-K3 inputs of the encoder to 0000
        //was 11 now 00
        this.pinK0 = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_00, "K0", PinState.LOW);
        //was 15 now 03
        this.pinK1 = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_03, "K1", PinState.LOW);
        //was 16 now 04
        this.pinK2 = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_04, "K2", PinState.LOW);
        //was 13 now 02
        this.pinK3 = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_02, "K3", PinState.LOW);
        this.pinK0.setShutdownOptions(true, PinState.LOW);
        this.pinK1.setShutdownOptions(true, PinState.LOW);
        this.pinK2.setShutdownOptions(true, PinState.LOW);
        this.pinK3.setShutdownOptions(true, PinState.LOW);
        System.out.println("Socket Control initialised");
    }

    /**
     * switchSocket     -   Switches the specified socket to the required state
     * @param s         -   Socket 1-4 or 0 for ALL
     * @param on         -  true for on false for off
     */
    public void switchSocket(int s, boolean on)
    {
        SocketCode socket = socketMap.get(s);
        SocketState state = SocketState.OFF;
        if (on) state = SocketState.ON;
        this.pinK0.setState(state.state);
        this.pinK1.setState(socket.pinK1);
        this.pinK2.setState(socket.pinK2);
        this.pinK3.setState(socket.pinK3);
        if (socket == SocketCode.ALL)
        {
            this.pinK1.setState(PinState.LOW);
            if(state == SocketState.ON) this.pinK0.setState(PinState.LOW); else this.pinK0.setState(PinState.HIGH);
        }
        try
        {
            TimeUnit.MILLISECONDS.sleep(100);// delay to allow encoder to settle
        } catch (InterruptedException ignored){}
        modulatorEnablePin.setState(PinState.HIGH);
        try
        {
            TimeUnit.MILLISECONDS.sleep(250);// delay to socket to action command
        } catch (InterruptedException ignored){}
        modulatorEnablePin.setState(PinState.LOW);
        System.out.println(socket.name()+" switched "+ state.name());
    }

    /**
     * blinkSocket      - blinks a socket on for the required time
     * @param socket    - socket number (1-4) or 0 for all
     * @param seconds   - on time in seconds
     */
    public void blinkSocket(int socket, int seconds)
    {
        switchSocket(socket, true);
        try
        {
            TimeUnit.SECONDS.sleep(seconds);
        } catch (InterruptedException ignored){}
        switchSocket(socket, false);
        try
        {
            TimeUnit.SECONDS.sleep(seconds);
        } catch (InterruptedException ignored){}
    }

    /**
     * programSocket    -   sends signals to a socket to allow it to learn it's code
     * @param socket    -   socket number 1-4 only
     */
    public void programSocket(int socket)
    {
        System.out.println("To clear the socket programming, press the green button");
        System.out.println("for 5 seconds or more until the red light flashes slowly");
        System.out.println("The socket is now in its learning mode and listening for");
        System.out.println("a control code to be sent to switch the socket on and off (3 attempts)");
        try
        {
            TimeUnit.SECONDS.sleep(7); //give time for the socket to be put in learning mode
        } catch (InterruptedException ignored){}
        blinkSocket(socket,2);
        blinkSocket(socket,2);
        blinkSocket(socket,2);
     }
 }