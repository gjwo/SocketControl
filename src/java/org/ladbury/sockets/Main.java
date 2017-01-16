package org.ladbury.sockets;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.RaspiPin;
import org.ladbury.RCSwitch.RadioReceiver;
import org.ladbury.RCSwitch.RadioTransmitter;

import java.util.concurrent.TimeUnit;

@SuppressWarnings("CanBeFinal")
public class Main implements Runnable,IParameterValidator
{
    private static Main main;
    @Parameter(names = {"--help", "-h",},description = "Display help information", help = true)
    private boolean help=false;
    @Parameter(names = {"--demo", "-d"},description = "Demonstration")
    private boolean demo = false;
    @Parameter(names = {"--testRCSwitch", "-trc"},description = "Test RC Switch")
    private boolean testRC = false;
    @Parameter(names = {"--testRadioTransmitter", "-trt"},description = "Test Radio Transmitter")
    private boolean testRT = false;
    @Parameter(names = {"--switch","-s"},description = "Switch a socket (1-4), 0 = all add -on if required",arity = 1)
    private int switchNumber = -1;
    @Parameter(names = {"--train","-t"}, description = "Train socket (1-4)", arity = 1)
    private int trainSwitchNumber = -1;
    @Parameter(names = "-on", description = "if present the socket is turned on, else it is turned off")
    private boolean switchOn = false;
    @Parameter(names = {"--TestRadioReceiver", "-trr"},description = "Test Radio Receiver")
    private boolean testRR = false;


    private JCommander jc;
    private int numberArgs =0;
    private final GpioController gpio;
    private RadioReceiver radioReceiver;
    private RadioTransmitter radioTransmitter;

private static SocketControl s;

    private Main()
    {
        gpio= GpioFactory.getInstance();
    }

    public static RadioTransmitter getTransmitter(){return main.radioTransmitter;}
    public static RadioReceiver getReceiver(){return main.radioReceiver;}

    public static void main(String[] args)
    {
        Main main = new Main();
        main.jc = new JCommander(main,args);
        main.numberArgs = args.length;
        main.run();
    }

    private static void demo()
    {
        s.blinkSocket(1,3);
        s.blinkSocket(2,3);
        s.blinkSocket(3,3);
        s.blinkSocket(4,3);
        s.blinkSocket(0,3);

    }
    private void actionArguments()
    {
        if ((numberArgs == 0) | help)
        {
            jc.usage();
            /*
            System.out.println("Usage: java SocketControl -d -s 1 on -s 2 off -a on");
            System.out.println("-d demonstration cycle, all plugs on and off individually then together");
            System.out.println("-s <n> <state>  Switch socket n 'on' or 'off', n is 1-4");
            System.out.println("-a <state>  Switch all sockets'on' or 'off'");
            */
            System.exit(1);
        }

        s = new SocketControl(this.gpio);

        if (demo)demo();
        if (trainSwitchNumber>0) s.programSocket(trainSwitchNumber);
        if (switchNumber>=0) s.switchSocket(switchNumber,switchOn);
        if (testRC)
        {
            TestRCSwitch tt = new TestRCSwitch();
            tt.test1();
        }
        if (testRR)
        {
            radioReceiver = new RadioReceiver(gpio.provisionDigitalInputPin(RaspiPin.GPIO_25,"Receiver Pin"));
            Thread mainThread = new Thread(this);
            mainThread.run();
            radioReceiver.enableReceive(1);
            System.out.println("Receiver started");
            try
            {
                TimeUnit.SECONDS.sleep(120);
            } catch (InterruptedException e)
            {
                e.printStackTrace();
            }
            radioReceiver.disableReceive();
            System.out.println("Receiver stopped");
        }
        if (testRT)
        {
            this.radioTransmitter = new RadioTransmitter(gpio.provisionDigitalOutputPin(RaspiPin.GPIO_27,"Transmitter Pin"));
        }
        gpio.shutdown();
        System.exit(0);
    }
    @Override
    public void run()
    {
        while(radioReceiver.getnReceiverInterrupt()>=0)
        {
            if(radioReceiver.available())
            {
                System.out.println( "Received: "+ radioReceiver.getReceivedValue()+
                                    " nBits: "+radioReceiver.getReceivedBitLength()+
                                    " Delay: " + radioReceiver.getReceivedDelay()+
                                    " Protocol: "+radioReceiver.getReceivedProtocol().name());
                radioReceiver.resetAvailable();
            }
            try
            {
                TimeUnit.MILLISECONDS.sleep(500);
            } catch (InterruptedException e)
            {
                e.printStackTrace();
            }
        }

    }
    @Override
    public void validate(String name, String value) throws ParameterException
    {
        int n = Integer.parseInt(value);
        if (name.contains("s"))
        {
            if ((n < 0) | (n > 4))
            {
                throw new ParameterException("Parameter " + name + "should be  0,1,2,3 or 4" + "it was " + value + ")");
            }
        }
        if (name.contains("t"))
        {
            if ((n < 1) | (n > 4))
            {
                throw new ParameterException("Parameter " + name + "should be  1,2,3 or 4" + "it was " + value + ")");
            }
        }
    }
}
