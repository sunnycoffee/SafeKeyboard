package me.coffee.safekeyboard;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import me.coffee.widget.keyboard.SafeKeyboard;
import me.coffee.widget.keyboard.SecEditText;

public class MainActivity extends AppCompatActivity {

    private SafeKeyboard safeKeyboard;

    private EditText safeEdit;
    private EditText safeEdit2;
    private EditText safeEdit3;
    private SecEditText safeEdit4;
    private SecEditText safeEdit5;
    private Button btn4;
    private Button btn5;
    private TextView tv;
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
        safeEdit4 = findViewById(R.id.safeEditText4);
        safeEdit5 = findViewById(R.id.safeEditText5);
        btn4 = findViewById(R.id.btn_4);
        btn5 = findViewById(R.id.btn_5);
        tv = findViewById(R.id.tv);

        btn4.setOnClickListener(v -> {
            tv.setText(safeEdit4.getText().toString());
            showToast(safeEdit4.getTextString());
        });

        btn5.setOnClickListener(v -> {
            tv.setText(safeEdit5.getText().toString());
            showToast(safeEdit5.getTextString());
        });


        editList.add(safeEdit);
        editList.add(safeEdit2);
        editList.add(safeEdit3);
        editList.add(safeEdit4);
        editList.add(safeEdit5);
        safeKeyboard = new SafeKeyboard(editList);
    }


    // 当点击返回键时, 如果软键盘正在显示, 则隐藏软键盘并是此次返回无效
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (safeKeyboard.doBackKeyDown(keyCode, event)) return true;
        else return super.onKeyDown(keyCode, event);
    }

    private void showToast(String msg){
        Toast.makeText(this,msg,Toast.LENGTH_LONG).show();
    }


}
