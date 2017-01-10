package org.ladbury;

import java.util.concurrent.TimeUnit;

public class Main {

    public static void main(String[] args)
    {
        SocketControl s = new SocketControl();
        System.out.println("To clear the socket programming, press the green button");
        System.out.println("To clear the socket programming, press the green button");
        System.out.println("for 5 seconds or more until the red light flashes slowly");
        System.out.println("The socket is now in its learning mode and listening for");
        System.out.println("a control code to be sent. ");
        s.switchSocket(SocketControl.SocketCode.SOCKET1, SocketControl.SocketState.ON);
        try
        {
            TimeUnit.SECONDS.sleep(3);
        } catch (InterruptedException ignored){}
        s.switchSocket(SocketControl.SocketCode.SOCKET2, SocketControl.SocketState.ON);
         try
        {
            TimeUnit.SECONDS.sleep(3);
        } catch (InterruptedException ignored){}
        s.switchSocket(SocketControl.SocketCode.SOCKET1, SocketControl.SocketState.OFF);
        try
        {
            TimeUnit.SECONDS.sleep(3);
        } catch (InterruptedException ignored){}
        s.switchSocket(SocketControl.SocketCode.SOCKET2, SocketControl.SocketState.OFF);
        try
        {
            TimeUnit.SECONDS.sleep(3);
        } catch (InterruptedException ignored){}
        s.switchSocket(SocketControl.SocketCode.ALL, SocketControl.SocketState.ON);
        try
        {
            TimeUnit.SECONDS.sleep(3);
        } catch (InterruptedException ignored){}
        s.switchSocket(SocketControl.SocketCode.ALL, SocketControl.SocketState.OFF);
        try
        {
            TimeUnit.SECONDS.sleep(3);
        } catch (InterruptedException ignored) {}
        System.exit(0);
    }
}
