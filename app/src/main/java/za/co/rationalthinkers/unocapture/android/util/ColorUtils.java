package za.co.rationalthinkers.unocapture.android.util;

import android.hardware.camera2.params.RggbChannelVector;

public class ColorUtils {

    /**
     *  Converts a white balance temperature to red, green even, green odd and blue components.
     */
    public static RggbChannelVector convertTemperatureToRggb(int temperature_kelvin) {
        float temperature = temperature_kelvin / 100.0f;
        float red;
        float green;
        float blue;

        if( temperature <= 66 ) {
            red = 255;
        }
        else {
            red = temperature - 60;
            red = (float) (329.698727446 * (Math.pow((double) red, -0.1332047592)));
            if( red < 0 )
                red = 0;
            if( red > 255 )
                red = 255;
        }

        if( temperature <= 66 ) {
            green = temperature;
            green = (float) (99.4708025861 * Math.log(green) - 161.1195681661);
            if( green < 0 )
                green = 0;
            if( green > 255 )
                green = 255;
        }
        else {
            green = temperature - 60;
            green = (float) (288.1221695283 * (Math.pow((double) green, -0.0755148492)));
            if (green < 0)
                green = 0;
            if (green > 255)
                green = 255;
        }

        if( temperature >= 66 ) {
            blue = 255;
        }
        else if( temperature <= 19 ) {
            blue = 0;
        }
        else {
            blue = temperature - 10;
            blue = (float) (138.5177312231 * Math.log(blue) - 305.0447927307);
            if( blue < 0 )
                blue = 0;
            if( blue > 255 )
                blue = 255;
        }

        return new RggbChannelVector((red/255)*2,(green/255),(green/255),(blue/255)*2);
    }

    /** Converts a red, green even, green odd and blue components to a white balance temperature.
     *  Note that this is not necessarily an inverse of convertTemperatureToRggb, since many rggb
     *  values can map to the same temperature.
     */
    /*private int convertRggbToTemperature(RggbChannelVector rggbChannelVector) {
        float red = rggbChannelVector.getRed();
        float green_even = rggbChannelVector.getGreenEven();
        float green_odd = rggbChannelVector.getGreenOdd();
        float blue = rggbChannelVector.getBlue();
        float green = 0.5f*(green_even + green_odd);

        float max = Math.max(red, blue);
        if( green > max )
            green = max;

        float scale = 255.0f/max;
        red *= scale;
        green *= scale;
        blue *= scale;

        int red_i = (int)red;
        int green_i = (int)green;
        int blue_i = (int)blue;
        int temperature;
        if( red_i == blue_i ) {
            temperature = 6600;
        }
        else if( red_i > blue_i ) {
            // temperature <= 6600
            int t_g = (int)( 100 * Math.exp((green_i + 161.1195681661) / 99.4708025861) );
            if( blue_i == 0 ) {
                temperature = t_g;
            }
            else {
                int t_b = (int)( 100 * (Math.exp((blue_i + 305.0447927307) / 138.5177312231) + 10) );
                temperature = (t_g + t_b)/2;
            }
        }
        else {
            // temperature >= 6700
            if( red_i <= 1 || green_i <= 1 ) {
                temperature = max_white_balance_temperature_c;
            }
            else {
                int t_r = (int) (100 * (Math.pow(red_i / 329.698727446, 1.0 / -0.1332047592) + 60.0));
                int t_g = (int) (100 * (Math.pow(green_i / 288.1221695283, 1.0 / -0.0755148492) + 60.0));
                temperature = (t_r + t_g) / 2;
            }
        }
        temperature = Math.max(temperature, min_white_balance_temperature_c);
        temperature = Math.min(temperature, max_white_balance_temperature_c);

        return temperature;
    }
    */
}
