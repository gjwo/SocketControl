package org.ladbury.sockets;

class CodeWord
{
    TriState word[];
    boolean valid;

    CodeWord() {word = new TriState[13]; valid=false; }
    TriState getTriStateBit(int i){return word[i];}
    void setTriStateBit(int i,TriState t){word[i] = t;}
    void setValid(boolean v){valid = v;}
    boolean isValid(){return valid;}
}

enum TriState {zero,one,floating}


/**
 * Created by GJWood on 14/01/2017.
 */
public class CodeWords
{

    /**
     * @param sCodeWord   a tristate code word consisting of the letter 0, 1, F
     */
    static void sendTriState(final byte[] sCodeWord)
    {
        // turn the tristate code word into the corresponding bit pattern, then send it
        /*unsigned*/ long code = 0;
        /*unsigned*/ int length = 0;
        for (final char* p = sCodeWord; *p; p++)
        {
            code <<= 2L;
            switch (*p) {
                case '0':
                    // bit pattern 00
                    break;
                case 'F':
                    // bit pattern 01
                    code |= 1L;
                    break;
                case '1':
                    // bit pattern 11
                    code |= 3L;
                    break;
            }
            length += 2;
        }
        //zzzz.send(code, length);
    }


    /**
     * Returns a char[13], representing the code word to be send.
     *
     */
    static CodeWord getCodeWordA(final char[] sGroup, final char[] sDevice, boolean bStatus)
    {
        /*static*/ CodeWord cw = new CodeWord();
        int index = 0;

        for (int i = 0; i < 5; i++) {
            cw.setTriStateBit(index++,  (sGroup[i] == '0') ? TriState.floating : TriState.zero);
        }
        for (int i = 0; i < 5; i++) {
            cw.setTriStateBit(index++,(sDevice[i] == '0') ? TriState.floating : TriState.zero);
        }
        cw.setTriStateBit(index++,bStatus ? TriState.zero : TriState.floating);
        cw.setTriStateBit(index++,bStatus ? TriState.floating : TriState.zero);
        //cw[index] = '\0';
        return cw;
    }

    /**
     * Encoding for type B switches with two rotary/sliding switches.
     *
     * The code word is a tristate word and with following bit pattern:
     *
     * +-----------------------------+-----------------------------+----------+------------+
     * | 4 bits address              | 4 bits address              | 3 bits   | 1 bit      |
     * | switch group                | switch number               | not used | on / off   |
     * | 1=0FFF 2=F0FF 3=FF0F 4=FFF0 | 1=0FFF 2=F0FF 3=FF0F 4=FFF0 | FFF      | on=F off=0 |
     * +-----------------------------+-----------------------------+----------+------------+
     *
     * @param nAddressCode  Number of the switch group (1..4)
     * @param nChannelCode  Number of the switch itself (1..4)
     * @param bStatus       Whether to switch on (true) or off (false)
     *
     * @return char[13], representing a tristate code word of length 12
     */
    static /* char* */ byte[] getCodeWordB(int nAddressCode, int nChannelCode, boolean bStatus)
    {
        /*static*/ char[] sReturn = new char[13];
        int nReturnPos = 0;

        if (nAddressCode < 1 || nAddressCode > 4 || nChannelCode < 1 || nChannelCode > 4) {
            return 0;
        }

        for (int i = 1; i <= 4; i++) {
            sReturn[nReturnPos++] = (nAddressCode == i) ? '0' : 'F';
        }

        for (int i = 1; i <= 4; i++) {
            sReturn[nReturnPos++] = (nChannelCode == i) ? '0' : 'F';
        }

        sReturn[nReturnPos++] = 'F';
        sReturn[nReturnPos++] = 'F';
        sReturn[nReturnPos++] = 'F';

        sReturn[nReturnPos++] = bStatus ? 'F' : '0';

        sReturn[nReturnPos] = '\0';
        return sReturn;
    }

    /**
     * Like getCodeWord (Type C = Intertechno)
     */
    static /* char* */ byte[] getCodeWordC(char sFamily, int nGroup, int nDevice, boolean bStatus)
    {
        static char sReturn[13];
        int nReturnPos = 0;

        int nFamily = (int)sFamily - 'a';
        if ( nFamily < 0 || nFamily > 15 || nGroup < 1 || nGroup > 4 || nDevice < 1 || nDevice > 4) {
            return null ;//0
        }

        // encode the family into four bits
        sReturn[nReturnPos++] = (nFamily & 1) ? 'F' : '0';
        sReturn[nReturnPos++] = (nFamily & 2) ? 'F' : '0';
        sReturn[nReturnPos++] = (nFamily & 4) ? 'F' : '0';
        sReturn[nReturnPos++] = (nFamily & 8) ? 'F' : '0';

        // encode the device and group
        sReturn[nReturnPos++] = ((nDevice-1) & 1) ? 'F' : '0';
        sReturn[nReturnPos++] = ((nDevice-1) & 2) ? 'F' : '0';
        sReturn[nReturnPos++] = ((nGroup-1) & 1) ? 'F' : '0';
        sReturn[nReturnPos++] = ((nGroup-1) & 2) ? 'F' : '0';

        // encode the status code
        sReturn[nReturnPos++] = '0';
        sReturn[nReturnPos++] = 'F';
        sReturn[nReturnPos++] = 'F';
        sReturn[nReturnPos++] = bStatus ? 'F' : '0';

        sReturn[nReturnPos] = '\0';
        return sReturn;
    }

    /**
     * Encoding for the REV Switch Type
     *
     * The code word is a tristate word and with following bit pattern:
     *
     * +-----------------------------+-------------------+----------+--------------+
     * | 4 bits address              | 3 bits address    | 3 bits   | 2 bits       |
     * | switch group                | device number     | not used | on / off     |
     * | A=1FFF B=F1FF C=FF1F D=FFF1 | 1=0FF 2=F0F 3=FF0 | 000      | on=10 off=01 |
     * +-----------------------------+-------------------+----------+--------------+
     *
     * Source: http://www.the-intruder.net/funksteckdosen-von-rev-uber-arduino-ansteuern/
     *
     * @param sGroup        Name of the switch group (A..D, resp. a..d)
     * @param nDevice       Number of the switch itself (1..3)
     * @param bStatus       Whether to switch on (true) or off (false)
     *
     * @return char[13], representing a tristate code word of length 12
     */
    static /*char* */ byte[] getCodeWordD(char sGroup, int nDevice, boolean bStatus)
    {
        static char sReturn[13];
        int nReturnPos = 0;

        // sGroup must be one of the letters in "abcdABCD"
        int nGroup = (sGroup >= 'a') ? (int)sGroup - 'a' : (int)sGroup - 'A';
        if ( nGroup < 0 || nGroup > 3 || nDevice < 1 || nDevice > 3) {
            return 0;
        }

        for (int i = 0; i < 4; i++) {
            sReturn[nReturnPos++] = (nGroup == i) ? '1' : 'F';
        }

        for (int i = 1; i <= 3; i++) {
            sReturn[nReturnPos++] = (nDevice == i) ? '1' : 'F';
        }

        sReturn[nReturnPos++] = '0';
        sReturn[nReturnPos++] = '0';
        sReturn[nReturnPos++] = '0';

        sReturn[nReturnPos++] = bStatus ? '1' : '0';
        sReturn[nReturnPos++] = bStatus ? '0' : '1';

        sReturn[nReturnPos] = '\0';
        return sReturn;
    }
}