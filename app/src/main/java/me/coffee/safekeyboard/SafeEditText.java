package me.coffee.safekeyboard;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.EditText;

/**
 * @author kongfei
 */
public class SafeEditText extends EditText {

    public SafeEditText(Context context) {
        super(context);
    }

    public SafeEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SafeEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public SafeEditText(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    public boolean dispatchKeyEventPreIme(KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
            Log.d("test", "====>dispatchKeyEventPreIme");
        }
        return super.dispatchKeyEventPreIme(event);
    }
}
