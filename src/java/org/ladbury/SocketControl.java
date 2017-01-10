package org.ladbury;

import com.pi4j.io.gpio.*;

import java.util.concurrent.TimeUnit;

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
        @SuppressWarnings("unused")SOCKET3(PinState.HIGH,PinState.LOW,PinState.HIGH),
        @SuppressWarnings("unused")SOCKET4(PinState.HIGH,PinState.LOW,PinState.LOW);

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

    private final GpioPinDigitalOutput pinK0;
    private final GpioPinDigitalOutput pinK1;
    private final GpioPinDigitalOutput pinK2;
    private final GpioPinDigitalOutput pinK3;
    @SuppressWarnings("FieldCanBeLocal")
    private final GpioPinDigitalOutput modulationSelectPin;
    private final GpioPinDigitalOutput modulatorEnablePin;

    /**
     * SocketControl    -   Constructor, sets up the GPIO pins required to control the sockets
     */
    public SocketControl()
    {
        final GpioController gpio = GpioFactory.getInstance();

        // set the pins numbering mode
        // GPIO.setMode(GPIO.BOARD)

        //Select the signal used to enable/disable the modulator
        this.modulatorEnablePin = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_22, "CE", PinState.LOW);
        this.modulatorEnablePin.setShutdownOptions(true, PinState.LOW);

        // Select the GPIO pin used to select Modulation ASK/FSK
        // Set the modulator to ASK for On Off Keying by setting MODSEL pin low
        this.modulationSelectPin = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_18, "ASK/FSK", PinState.LOW);
        this.modulationSelectPin.setShutdownOptions(true, PinState.LOW);

        // Select the GPIO pins used for the encoder K0-K3 data inputs
        // and initialise K0-K3 inputs of the encoder to 0000
        this.pinK0 = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_11, "K0", PinState.LOW);
        this.pinK1 = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_15, "K1", PinState.LOW);
        this.pinK2 = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_16, "K2", PinState.LOW);
        this.pinK3 = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_13, "K3", PinState.LOW);
        this.pinK0.setShutdownOptions(true, PinState.LOW);
        this.pinK1.setShutdownOptions(true, PinState.LOW);
        this.pinK2.setShutdownOptions(true, PinState.LOW);
        this.pinK3.setShutdownOptions(true, PinState.LOW);
        System.out.println("Socket Control initialised");
    }
    /**
     * switchSocket     -   Switches the specified socket to the required state
     * @param socket    -   Socket 1-4 or ALL
     * @param state     -   ON or OFF
     */
    public void switchSocket(SocketCode socket, SocketState state)
    {
        this.pinK0.setState(state.state);
        this.pinK1.setState(socket.pinK1);
        this.pinK2.setState(socket.pinK2);
        this.pinK3.setState(socket.pinK3);

        try
        {
            TimeUnit.MILLISECONDS.sleep(100);// delay to allow encoder to settle
        } catch (InterruptedException ignored)
        {
        }
        modulatorEnablePin.high();
        try
        {
            TimeUnit.MILLISECONDS.sleep(250);// delay to socket to action command
        } catch (InterruptedException ignored)
        {
        }
        modulatorEnablePin.low();
        System.out.println(socket.name()+" switched "+ state.name());
    }
 }