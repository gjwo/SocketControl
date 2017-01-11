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

        final PinState pinD2;
        final PinState pinD1;
        final PinState pinD0;

        SocketCode(PinState pinD2,PinState pinD1,PinState pinD0)
        {
            this.pinD2 = pinD2;
            this.pinD1 = pinD1;
            this.pinD0 = pinD0;
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


    private final GpioPinDigitalOutput pinD0; //All = LOW, individual Socket = High
    private final GpioPinDigitalOutput pinD1; //All = LOW, individual Socket = High
    private final GpioPinDigitalOutput pinD2; // K2-K3: HIGH HIGH = Socket1, HIGH LOW = socket2
    private final GpioPinDigitalOutput pinD3; // K2-K3: LOW HIGH = Socket3, LOW LOW = socket4
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
        //was physical 22 now Pi4j GPIO 06
        this.modulatorEnablePin = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_06, "CE", PinState.LOW);
        this.modulatorEnablePin.setShutdownOptions(true, PinState.LOW);

        // Select the GPIO pin used to select Modulation ASK/FSK
        // Set the modulator to ASK for On Off Keying by setting MODSEL pin low
        //was physical 18 now Pi4j GPIO  05
        this.modulationSelectPin = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_05, "MODSEL", PinState.LOW);
        this.modulationSelectPin.setShutdownOptions(true, PinState.LOW);

        // Select the GPIO pins used for the encoder K0-K3 data inputs
        // and initialise K0-K3 inputs of the encoder to 0000
        //was physical 11 now Pi4j GPIO 00
        this.pinD0 = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_00, "Encoder signal D0", PinState.LOW);
        //was physical 15 now Pi4j GPIO 03
        this.pinD1 = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_03, "Encoder signal D1", PinState.LOW);
        //was physical 16 now Pi4j GPIO 04
        this.pinD2 = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_04, "Encoder signal D2", PinState.LOW);
        //was physical 13 now Pi4j GPIO 02
        this.pinD3 = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_02, "Encoder signal D3", PinState.LOW);
        this.pinD0.setShutdownOptions(true, PinState.LOW);
        this.pinD1.setShutdownOptions(true, PinState.LOW);
        this.pinD2.setShutdownOptions(true, PinState.LOW);
        this.pinD3.setShutdownOptions(true, PinState.LOW);
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
        this.pinD3.setState(state.state);

        this.pinD0.setState(socket.pinD0);
        this.pinD1.setState(socket.pinD1);
        this.pinD2.setState(socket.pinD2);
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