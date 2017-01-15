package org.ladbury.sockets;

/**
 * Created by GJWood on 14/01/2017.
 */
public class SwitchControl
{

    /**
     * Switch a remote switch on (Type D REV)
     *
     * @param sGroup        Code of the switch group (A,B,C,D)
     * @param nDevice       Number of the switch itself (1..3)
     */
    void switchOn(char sGroup, int nDevice)
    {
        CodeWords.sendTriState( CodeWords.getCodeWordD(sGroup, nDevice, true) );
    }

    /**
     * Switch a remote switch off (Type D REV)
     *
     * @param sGroup        Code of the switch group (A,B,C,D)
     * @param nDevice       Number of the switch itself (1..3)
     */
    void switchOff(char sGroup, int nDevice)
    {
        CodeWords.sendTriState( CodeWords.getCodeWordD(sGroup, nDevice, false) );
    }

    /**
     * Switch a remote switch on (Type C Intertechno)
     *
     * @param sFamily  Familycode (a..f)
     * @param nGroup   Number of group (1..4)
     * @param nDevice  Number of device (1..4)
     */
    void switchOn(char sFamily, int nGroup, int nDevice)
    {
        CodeWords.sendTriState( CodeWords.getCodeWordC(sFamily, nGroup, nDevice, true) );
    }

    /**
     * Switch a remote switch off (Type C Intertechno)
     *
     * @param sFamily  Familycode (a..f)
     * @param nGroup   Number of group (1..4)
     * @param nDevice  Number of device (1..4)
     */
    void switchOff(char sFamily, int nGroup, int nDevice)
    {
        CodeWords.sendTriState( CodeWords.getCodeWordC(sFamily, nGroup, nDevice, false) );
    }

    /**
     * Switch a remote switch on (Type B with two rotary/sliding switches)
     *
     * @param nAddressCode  Number of the switch group (1..4)
     * @param nChannelCode  Number of the switch itself (1..4)
     */
    void switchOn(int nAddressCode, int nChannelCode)
    {
        CodeWords.sendTriState( CodeWords.getCodeWordB(nAddressCode, nChannelCode, true) );
    }

    /**
     * Switch a remote switch off (Type B with two rotary/sliding switches)
     *
     * @param nAddressCode  Number of the switch group (1..4)
     * @param nChannelCode  Number of the switch itself (1..4)
     */
    void switchOff(int nAddressCode, int nChannelCode)
    {
        CodeWords.sendTriState( CodeWords.getCodeWordB(nAddressCode, nChannelCode, false) );
    }

    /**
     * Deprecated, use switchOn(final char* sGroup, final char* sDevice) instead!
     * Switch a remote switch on (Type A with 10 pole DIP switches)
     *
     * @param sGroup        Code of the switch group (refers to DIP switches 1..5 where "1" = on and "0" = off, if all DIP switches are on it's "11111")
     * @param nChannel      Number of the switch itself (1..5)
     */
    void switchOn(final String sGroup, int nChannel)
    {
        final /*char* */ String[] code = { "00000", "10000", "01000", "00100", "00010", "00001" };
        this.switchOn(sGroup, code[nChannel]);
    }

    /**
     * Deprecated, use switchOff(final char* sGroup, final char* sDevice) instead!
     * Switch a remote switch off (Type A with 10 pole DIP switches)
     *
     * @param sGroup        Code of the switch group (refers to DIP switches 1..5 where "1" = on and "0" = off, if all DIP switches are on it's "11111")
     * @param nChannel      Number of the switch itself (1..5)
     */
    void switchOff(final String sGroup, int nChannel)
    {
        final /*char* */ String[]code = { "00000", "10000", "01000", "00100", "00010", "00001" };
        this.switchOff(sGroup, code[nChannel]);
    }

    /**
     * Switch a remote switch on (Type A with 10 pole DIP switches)
     *
     * @param sGroup        Code of the switch group (refers to DIP switches 1..5 where "1" = on and "0" = off, if all DIP switches are on it's "11111")
     * @param sDevice       Code of the switch device (refers to DIP switches 6..10 (A..E) where "1" = on and "0" = off, if all DIP switches are on it's "11111")
     */
    void switchOn(final String sGroup, final String sDevice)
    {
        CodeWords.sendTriState( CodeWords.getCodeWordA(sGroup, sDevice, true) );
    }

    /**
     * Switch a remote switch off (Type A with 10 pole DIP switches)
     *
     * @param sGroup        Code of the switch group (refers to DIP switches 1..5 where "1" = on and "0" = off, if all DIP switches are on it's "11111")
     * @param sDevice       Code of the switch device (refers to DIP switches 6..10 (A..E) where "1" = on and "0" = off, if all DIP switches are on it's "11111")
     */
    void switchOff(final String sGroup, final String sDevice)
    {
        CodeWords.sendTriState( CodeWords.getCodeWordA(sGroup, sDevice, false) );
    }


}
