package tw.jeff.smallcarremotecontroller;

/**
 * Created by Jeff on 2017/5/27.
 */

public class WheelCommand {
    public static final int defaultMax = 255;
    private int leftMax = defaultMax;
    private int rightMax = defaultMax;

    public int getLeftMax() {
        return leftMax;
    }

    public int getRightMax() {
        return rightMax;
    }

    public WheelCommand setMaxSpeed(int i) {
        if (Math.abs(i) < 256) {
            if (i > 0) {
                leftMax = defaultMax;
                rightMax = defaultMax - i;
            } else {
                leftMax = defaultMax + i;
                rightMax = defaultMax;
            }
        }
        return this;
    }


    public String getCommand(double leftRate, double rightRate) {
        if (leftRate > 1) leftRate = 1.0;
        if (leftRate < -1) leftRate = -1.0;
        if (rightRate > 1) rightRate = 1.0;
        if (rightRate < -1) rightRate = -1.0;
        return String.format("W%03d%03d",
                (Math.round(leftRate * leftMax)) + 255,
                (Math.round(rightRate * rightMax)) + 255);
    }


}
