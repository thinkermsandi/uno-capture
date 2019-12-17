package za.co.rationalthinkers.unocapture.android.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.TextureView;

public class CustomTextureView extends TextureView {

    private int mRatioWidth = 0;
    private int mRatioHeight = 0;

    public CustomTextureView(Context context) {
        this(context, null);
    }

    public CustomTextureView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CustomTextureView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    /**
     * Sets the aspect ratio for this view. The size of the view will be measured based on the ratio
     * calculated from the parameters. Note that the actual sizes of parameters don't matter, that
     * is, calling setAspectRatio(2, 3) and setAspectRatio(4, 6) make the same result.
     *
     * @param width  Relative horizontal size
     * @param height Relative vertical size
     */
    public void setAspectRatio(int width, int height) {
        if (width < 0 || height < 0) {
            throw new IllegalArgumentException("Size cannot be negative.");
        }

        mRatioWidth = width;
        mRatioHeight = height;

        requestLayout();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);

        /*
         * Fixing the issue of displaying the textureview preview at
         * approximately 70% of the screen height by
         * switching setMeasuredDimension();
         *
         * https://stackoverflow.com/questions/38535355/android-textureview-full-screen-preview-with-correct-aspect-ratio
         */
        if (0 == mRatioWidth || 0 == mRatioHeight) {
            setMeasuredDimension(width, height);
        }
        else {
            if (width < height * mRatioWidth / mRatioHeight) {
                setMeasuredDimension(height * mRatioWidth / mRatioHeight, height);
            }
            else {
                setMeasuredDimension(width, width * mRatioHeight / mRatioWidth);
            }
        }
    }

}
