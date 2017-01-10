package org.ladbury;

import java.util.concurrent.TimeUnit;

public class Main {

    public static void main(String[] args)
    {
        SocketControl s = new SocketControl();
        System.out.println("To clear the socket programming, press the green button");
        System.out.println("for 5 seconds or more until the red light flashes slowly");
        System.out.println("The socket is now in its learning mode and listening for");
        System.out.println("a control code to be sent. ");

        s.blinkSocket(SocketControl.SocketCode.SOCKET1,3);
        s.blinkSocket(SocketControl.SocketCode.SOCKET2,3);
        s.blinkSocket(SocketControl.SocketCode.SOCKET3,3);
        s.blinkSocket(SocketControl.SocketCode.SOCKET4,3);
        s.blinkSocket(SocketControl.SocketCode.ALL,3);

        System.exit(0);
    }
}
