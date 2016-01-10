package arduinotoandroid.arduinotoandroidcode;

/**
 * Created by nickdelnano on 12/17/15.
 */
public class SpeedHolder
{
    private String leftSpeed;
    private String rightSpeed;

    public SpeedHolder()
    {
        leftSpeed = "0";
        rightSpeed = "0";
    }

    public String getLeftSpeed() {
        return leftSpeed;
    }

    public void setLeftSpeed(String leftSpeed) {
        this.leftSpeed = leftSpeed;
    }

    public String getRightSpeed() {
        return rightSpeed;
    }

    public void setRightSpeed(String rightSpeed) {
        this.rightSpeed = rightSpeed;
    }
}
