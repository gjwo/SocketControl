package org.ladbury;

import com.pi4j.io.gpio.*;

import java.util.concurrent.TimeUnit;

/**
 * Created by GJWood on 09/01/2017.
 *
 */
public class SocketControl
{

    //To clear the socket programming, press the green button for 5 seconds or more until the red light flashes slowly
    //The socket is now in its learning mode and listening for a control code to be sent.
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

    private final GpioPinDigitalOutput pinK0;
    private final GpioPinDigitalOutput pinK1;
    private final GpioPinDigitalOutput pinK2;
    private final GpioPinDigitalOutput pinK3;
    private final GpioPinDigitalOutput modulationSelect;
    private final GpioPinDigitalOutput modulatorEnable;


    public SocketControl()
    {
        final GpioController gpio = GpioFactory.getInstance();

        // set the pins numbering mode
        // GPIO.setmode(GPIO.BOARD)

        //Select the signal used to enable/disable the modulator
        this.modulatorEnable = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_22, "CE", PinState.LOW);
        this.modulatorEnable.setShutdownOptions(true, PinState.LOW);

        // Select the GPIO pin used to select Modulation ASK/FSK
        // Set the modulator to ASK for On Off Keying by setting MODSEL pin low
        this.modulationSelect = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_18, "ASK/FSK", PinState.LOW);
        this.modulationSelect.setShutdownOptions(true, PinState.LOW);

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
    }

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
        modulatorEnable.high();
        try
        {
            TimeUnit.MILLISECONDS.sleep(250);// delay to socket to action command
        } catch (InterruptedException ignored)
        {
        }
        modulatorEnable.low();
    }
 }