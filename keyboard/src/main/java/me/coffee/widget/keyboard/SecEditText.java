package me.coffee.widget.keyboard;

import android.content.Context;
import android.os.Build;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.ActionMode;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;

/**
 * Author: xuan
 * Created on 2021/12/13 11:14.
 * <p>
 * Describe:
 */
public class SecEditText extends EditText implements EditChangeListener {

    // 存储点击的内容
    private byte[] contentValue;
    // 密钥
    private byte[] enk;

    public SecEditText(Context context) {
        super(context);
        init();
    }

    public SecEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SecEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setLongClickable(false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            this.setCustomInsertionActionModeCallback(new ActionModeCallbackInterceptor());
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO);
        }
        addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() == 0) {
                    contentValue = null;
                }
            }
        });
    }

    public String getTextString() {
        try {
            return contentValue == null ? "" : new String(strDecrypt3Des(contentValue));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_DEL) {
            return super.dispatchKeyEvent(event);
        } else {
            return true;
        }
    }

    @Override
    public void addText(String str, int position) {
        if (contentValue == null) {
            enk = ThreeDes.hex();
            contentValue = strEncrypt3Des(str);
        } else {
            byte[] dts = strDecrypt3Des(contentValue);
            String value = new String(dts);

            if (position >= value.length()) {
                value += str;
            } else {
                StringBuffer stringBuffer = new StringBuffer(value);
                stringBuffer.insert(position, str);
                value = stringBuffer.toString();
            }
            contentValue = strEncrypt3Des(value);
        }
    }

    @Override
    public void delText(int position) {
        if (contentValue == null || position == 0) return;
        byte[] dts = strDecrypt3Des(contentValue);
        String value = new String(dts);

        if (value.length() == 1) {
            contentValue = null;
            return;
        }

        StringBuffer stringBuffer = new StringBuffer(value);
        stringBuffer.deleteCharAt(position - 1);
        value = stringBuffer.toString();
        contentValue = strEncrypt3Des(value);
    }

    /**
     * 对字符串进行3DES加密
     */
    private byte[] strEncrypt3Des(String str) {
        return ThreeDes.encryptMode(enk, str.getBytes());
    }

    /**
     * 对字符串进行3DES解密
     */
    private byte[] strDecrypt3Des(byte[] by) {
        return ThreeDes.decryptMode(enk, by);
    }

    class ActionModeCallbackInterceptor implements ActionMode.Callback {

        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            return false;
        }


        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }


        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            return false;
        }


        public void onDestroyActionMode(ActionMode mode) {
        }
    }
}
