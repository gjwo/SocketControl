package org.ladbury;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;

@SuppressWarnings("CanBeFinal")
public class Main implements Runnable,IParameterValidator
{
    @Parameter(names = {"--demo", "-d"},description = "Demonstration")
    private
    boolean demo = false;
    @Parameter(names = {"--switch","-s"},description = "Switch a socket (1-4), 0 = all",arity = 1)
    private
    int switchNumber = -1;
    @Parameter(names = {"--train","-t"}, description = "Train socket (1-4)", arity = 1)
    private
    int trainSwitchNumber = -1;
    @Parameter(names = "-on", description = "if present the socket is turned on, else it is turned off")
    private
    boolean switchOn = false;
    @Parameter(names = {"--help", "-h"}, help = true)
    private boolean help=false;
    private JCommander jc;
    private int numberArgs =0;

private static SocketControl s;

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
    @Override
    public void run()
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

        s = new SocketControl();

        if (demo)demo();
        if (trainSwitchNumber>0) s.programSocket(trainSwitchNumber);
        if (switchNumber>=0) s.switchSocket(switchNumber,switchOn);

        System.exit(0);

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
