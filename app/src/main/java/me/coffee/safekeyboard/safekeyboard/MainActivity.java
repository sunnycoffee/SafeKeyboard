package me.coffee.safekeyboard.safekeyboard;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.HapticFeedbackConstants;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import me.coffee.safekeyboard.R;
import me.coffee.widget.keyboard.SafeKeyboard;

public class MainActivity extends AppCompatActivity {

    private SafeKeyboard safeKeyboard;

    private EditText safeEdit;
    private EditText safeEdit2;
    private EditText safeEdit3;
    private final List<EditText> editList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //设置当前activity不被录制以及截屏
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        setContentView(R.layout.activity_main);

        safeEdit = findViewById(R.id.safeEditText);
        safeEdit2 = findViewById(R.id.safeEditText2);
        safeEdit3 = findViewById(R.id.safeEditText3);

        safeEdit.setTag("number");
        editList.add(safeEdit);
        editList.add(safeEdit2);
        editList.add(safeEdit3);

        final Button clck = findViewById(R.id.feed_back);
        clck.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                clck.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS, HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
                Toast.makeText(MainActivity.this, safeEdit3.getText(), Toast.LENGTH_SHORT).show();
            }
        });
        safeKeyboard = new SafeKeyboard(MainActivity.this, editList, false, false, false);
    }


    // 当点击返回键时, 如果软键盘正在显示, 则隐藏软键盘并是此次返回无效
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
            if (safeKeyboard.isShow()) {
                safeKeyboard.hideKeyboard();
                StringBuilder builder = new StringBuilder();
                builder.append(safeEdit2.getText());
                ((TextView) findViewById(R.id.tv)).setText(builder.toString());
                return false;
            }

            return super.onKeyDown(keyCode, event);
        }
        return super.onKeyDown(keyCode, event);
    }


}
