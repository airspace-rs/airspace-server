package AiRSpace.AiRSpaceServer.Utility;

public class Helper
{
    public static int hexToInt(byte data[], int offset, int len) {
        int temp = 0;
        int i = 1000;
        for (int cntr = 0; cntr < len; cntr++) {
            int num = (data[offset + cntr] & 0xFF) * i;
            temp += (int) num;
            if (i > 1) {
                i = i / 1000;
            }
        }
        return temp;
    }
}
