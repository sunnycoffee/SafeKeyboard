package me.coffee.widget.keyboard;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.Editable;
import android.text.InputType;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

/**
 * Created by Mustang on 2019/1/9
 */

public class SafeKeyboard {

    private static final String TAG = "SafeKeyboard";

    private static final int KEYBOARD_TYPE_LETTER = 1;
    private static final int KEYBOARD_TYPE_SYMBOL = 2;
    private static final int KEYBOARD_TYPE_NUMBER = 3;

    private final Context mContext;               //上下文

    private View keyContainer;              //自定义键盘的容器View
    private SafeKeyboardView keyboardView;  //键盘的View
    private TextView titleView;             //标题
    private Keyboard keyboardNumber;        //数字键盘
    private Keyboard keyboardLetter;        //字母键盘
    private Keyboard keyboardSymbol;        //符号键盘
    private static boolean isCapes = false;
    private int keyboardType = KEYBOARD_TYPE_LETTER;
    private static final long HIDE_TIME = 300;
    private static final long SHOW_DELAY = 50;
    private static final long SHOW_TIME = 300;
    private static final long DELAY_TIME = 100;
    private final Handler showHandler = new Handler(Looper.getMainLooper());
    private final Handler hEndHandler = new Handler(Looper.getMainLooper());
    private final Handler sEndHandler = new Handler(Looper.getMainLooper());
    private final Handler checkHandler = new Handler(Looper.getMainLooper());
    private Drawable delDrawable;
    private Drawable lowDrawable;
    private Drawable upDrawable;
    private Drawable downDrawable;
    private final int keyboardContainerResId;
    private final int keyboardResId;

    private TranslateAnimation showAnimation;
    private TranslateAnimation hideAnimation;
    private long lastTouchTime;
    private EditText mEditText;
    private final List<EditText> editTextList;
    private final boolean isNumberRandom;
    private final boolean isLetterRandom;
    private final boolean isSymbolRandom;


    public SafeKeyboard(List<EditText> editTextList) {
        this(editTextList, false, false, false);
    }

    public SafeKeyboard(List<EditText> editTextList, boolean isNumberRandom, boolean isLetterRandom, boolean isSymbolRandom) {
        this.mContext = editTextList.get(0).getContext();
        this.editTextList = editTextList;
        this.keyboardContainerResId = R.layout.layout_keyboard_containor;
        this.keyboardResId = R.id.safeKeyboardLetter;
        this.isNumberRandom = isNumberRandom;
        this.isLetterRandom = isLetterRandom;
        this.isSymbolRandom = isSymbolRandom;
        delDrawable = mContext.getResources().getDrawable(R.drawable.icon_del);
        lowDrawable = mContext.getResources().getDrawable(R.drawable.icon_capital_default);
        upDrawable = mContext.getResources().getDrawable(R.drawable.icon_capital_selected);
        downDrawable = mContext.getResources().getDrawable(R.drawable.keyboard_done);
        initKeyboard();
        initAnimation();
        addListeners();
    }

    private void initAnimation() {
        showAnimation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0.0f,
                Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF
                , 1.0f, Animation.RELATIVE_TO_SELF, 0.0f);
        hideAnimation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0.0f,
                Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF
                , 0.0f, Animation.RELATIVE_TO_SELF, 1.0f);
        showAnimation.setDuration(SHOW_TIME);
        hideAnimation.setDuration(HIDE_TIME);

        showAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                // 在这里设置可见, 会出现第一次显示键盘时直接闪现出来, 没有动画效果, 后面正常
                // keyContainer.setVisibility(View.VISIBLE);
                // 动画持续时间 SHOW_TIME 结束后, 不管什么操作, 都需要执行, 把 isShowStart 值设为 false; 否则
                // 如果 onAnimationEnd 因为某些原因没有执行, 会影响下一次使用
                sEndHandler.removeCallbacks(showEnd);
                sEndHandler.postDelayed(showEnd, SHOW_TIME);
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                keyContainer.clearAnimation();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });

        hideAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                // 动画持续时间 HIDE_TIME 结束后, 不管什么操作, 都需要执行, 把 isHideStart 值设为 false; 否则
                // 如果 onAnimationEnd 因为某些原因没有执行, 会影响下一次使用
                hEndHandler.removeCallbacks(hideEnd);
                hEndHandler.postDelayed(hideEnd, HIDE_TIME);
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                if (keyContainer.getVisibility() != View.GONE) {
                    keyContainer.setVisibility(View.GONE);
                }
                keyContainer.clearAnimation();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
    }

    @SuppressLint({"ClickableViewAccessibility", "SetTextI18n"})
    private void initKeyboard() {
        if (mContext instanceof Activity) {
            ViewGroup rootView = ((Activity) mContext).getWindow().getDecorView().findViewById(android.R.id.content);
            keyContainer = LayoutInflater.from(mContext).inflate(keyboardContainerResId, rootView, false);
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT);
            params.gravity = Gravity.BOTTOM;
            rootView.addView(keyContainer, params);

            titleView = keyContainer.findViewById(R.id.title);
            titleView.setText(getAppName() + "安全键盘");
            keyContainer.findViewById(R.id.keyboardDone).setOnClickListener(v -> {
                if (isKeyboardShown()) {
                    hideKeyboard();
                }
            });
        }
        keyContainer.setVisibility(View.GONE);
        keyboardNumber = new Keyboard(mContext, R.xml.keyboard_num);            //实例化数字键盘
        keyboardLetter = new Keyboard(mContext, R.xml.keyboard_letter);         //实例化字母键盘
        keyboardSymbol = new Keyboard(mContext, R.xml.keyboard_symbol);         //实例化符号键盘
        // 由于符号键盘与字母键盘共用一个KeyBoardView, 所以不需要再为符号键盘单独实例化一个KeyBoardView
        keyboardView = keyContainer.findViewById(keyboardResId);
        keyboardView.setDelDrawable(delDrawable);
        keyboardView.setLowDrawable(lowDrawable);
        keyboardView.setUpDrawable(upDrawable);
        keyboardView.setHideDrawable(downDrawable);
        keyboardView.setEnabled(true);
        keyboardView.setPreviewEnabled(false);
        keyboardView.setOnKeyboardActionListener(listener);

        keyboardView.setOnTouchListener((v, event) -> event.getAction() == MotionEvent.ACTION_MOVE);
    }

    private static String getAppName() {
        try {
            final Context context = getApplicationByReflect();
            PackageManager pm = context.getPackageManager();
            PackageInfo pi = pm.getPackageInfo(context.getPackageName(), 0);
            return pi == null ? null : pi.applicationInfo.loadLabel(pm).toString();
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return "";
        }
    }

    private static Application getApplicationByReflect() {
        try {
            @SuppressLint("PrivateApi")
            Class<?> activityThread = Class.forName("android.app.ActivityThread");
            Object thread = activityThread.getMethod("currentActivityThread").invoke(null);
            Object app = activityThread.getMethod("getApplication").invoke(thread);
            return (Application) app;
        } catch (Exception ignore) {
        }
        return null;
    }

    /**
     * 键盘数字随机切换
     */
    private void randomNumbers() {
        List<Keyboard.Key> keyList = keyboardNumber.getKeys();
        // 查找出0-9的数字键
        List<Keyboard.Key> newkeyList = new ArrayList<Keyboard.Key>();
        for (int i = 0; i < keyList.size(); i++) {
            if (keyList.get(i).label != null
                    && isNumber(keyList.get(i).label.toString())) {
                newkeyList.add(keyList.get(i));
            }
        }
        // 数组长度
        int count = newkeyList.size();
        // 结果集
        List<KeyModel> resultList = new ArrayList<KeyModel>();
        // 用一个LinkedList作为中介
        LinkedList<KeyModel> temp = new LinkedList<KeyModel>();
        // 初始化temp
        for (int i = 0; i < count; i++) {
            temp.add(new KeyModel(48 + i, i + ""));
        }
       /* temp.add(new KeyModel(44 , "" + (char)44));
        temp.add(new KeyModel(46 , "" + (char)46));*/
        // 取数
        Random rand = new Random();
        for (int i = 0; i < count; i++) {
            int num = rand.nextInt(count - i);
            resultList.add(new KeyModel(temp.get(num).getCode(), temp.get(num)
                    .getLable()));
            temp.remove(num);
        }
        for (int i = 0; i < newkeyList.size(); i++) {
            newkeyList.get(i).label = resultList.get(i).getLable();
            newkeyList.get(i).codes[0] = resultList.get(i).getCode();
        }

        //keyboardView.setKeyboard(keyDig);
    }

    private boolean isNumber(String str) {
        String numStr = mContext.getString(R.string.zeroTonine);
        return numStr.contains(str.toLowerCase());
    }

    // 设置键盘点击监听
    private final KeyboardView.OnKeyboardActionListener listener = new KeyboardView.OnKeyboardActionListener() {

        @Override
        public void onPress(int primaryCode) {
        }

        @Override
        public void onRelease(int primaryCode) {
        }

        @Override
        public void onKey(int primaryCode, int[] keyCodes) {
            try {
                final Editable editable = mEditText.getText();
                final int start = mEditText.getSelectionStart();

                if (primaryCode == Keyboard.KEYCODE_CANCEL) {
                    // 隐藏键盘
                    hideKeyboard();
                } else if (primaryCode == Keyboard.KEYCODE_DELETE || primaryCode == -35) {
                    // 回退键,删除字符
                    if (editable != null && editable.length() > 0) {
                        if (start > 0) {
                            secEditDel(mEditText, start);
                            editable.delete(start - 1, start);
                        }
                    }
                } else if (primaryCode == Keyboard.KEYCODE_SHIFT) {
                    // 大小写切换
                    changeKeyboardLetterCase();
                    // 重新setKeyboard, 进而系统重新加载, 键盘内容才会变化(切换大小写)
                    keyboardType = KEYBOARD_TYPE_LETTER;
                    switchKeyboard();
                } else if (primaryCode == Keyboard.KEYCODE_MODE_CHANGE) {
                    // 数字与字母键盘互换
                    if (keyboardType == KEYBOARD_TYPE_NUMBER) { //当前为数字键盘
                        if (mEditText.getTag() != null) {
                            return;
                        }
                        keyboardType = KEYBOARD_TYPE_LETTER;
                    } else {        //当前不是数字键盘
                        keyboardType = KEYBOARD_TYPE_NUMBER;
                    }
                    switchKeyboard();
                } else if (primaryCode == -7) {
                    if (secEditAdd(mEditText, " ", start)) {
                        // 输入键盘值
                        editable.insert(start, " ");
                    }
                } else if (primaryCode == -8) {
                    hideKeyboard();
                } else if (primaryCode == 100860) {
                    // 字母与符号切换
                    if (keyboardType == KEYBOARD_TYPE_SYMBOL) { //当前是符号键盘
                        keyboardType = KEYBOARD_TYPE_LETTER;
                    } else {        //当前不是符号键盘, 那么切换到符号键盘
                        keyboardType = KEYBOARD_TYPE_SYMBOL;
                    }
                    switchKeyboard();
                } else {
                    if (secEditAdd(mEditText, Character.toString((char) primaryCode), start)) {
                        // 输入键盘值
                        editable.insert(start, Character.toString((char) primaryCode));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onText(CharSequence text) {
            final Editable editable = mEditText.getText();
            final boolean isCap = keyboardView.isCap();
            final int start = mEditText.getSelectionStart();
            final String value = isCap ? text.toString().toUpperCase() : text.toString().toLowerCase();
            if (secEditAdd(mEditText, value, start)) {
                // 输入键盘值
                editable.insert(start, value);
            }
        }

        @Override
        public void swipeLeft() {
        }

        @Override
        public void swipeRight() {
        }

        @Override
        public void swipeDown() {
        }

        @Override
        public void swipeUp() {
        }
    };


    private void switchKeyboard() {
        switch (keyboardType) {

            case KEYBOARD_TYPE_LETTER:
                if (isLetterRandom && !isCapes) {
                    randomLetter();
                }
                keyboardView.setKeyboard(keyboardLetter);
                break;
            case KEYBOARD_TYPE_SYMBOL:
                if (isSymbolRandom) {
                    randomSymbolkey();
                }

                keyboardView.setKeyboard(keyboardSymbol);
                break;
            case KEYBOARD_TYPE_NUMBER:
                if (isNumberRandom) {
                    randomNumbers();
                }
                keyboardView.setKeyboard(keyboardNumber);
                break;
            default:
                Log.e(TAG, "ERROR keyboard type");
                break;
        }
    }

    private void changeKeyboardLetterCase() {
        List<Keyboard.Key> keyList = keyboardLetter.getKeys();
        if (isCapes) {
            for (Keyboard.Key key : keyList) {
                if (key.label != null && isUpCaseLetter(key.label.toString())) {
                    key.label = key.label.toString().toLowerCase();
                    key.codes[0] += 32;
                }
            }

        } else {
            for (Keyboard.Key key : keyList) {
                if (key.label != null && isLowCaseLetter(key.label.toString())) {
                    key.label = key.label.toString().toUpperCase();
                    key.codes[0] -= 32;
                }
            }

        }
        isCapes = !isCapes;
        keyboardView.setCap(isCapes);


    }

    public void hideKeyboard() {
        keyContainer.clearAnimation();
        keyContainer.startAnimation(hideAnimation);
    }

    /**
     * 延迟状态判定
     */
    private final Runnable checkFocus = new Runnable() {
        @Override
        public void run() {
            boolean isFocus = true;
            for (int j = 0; j < editTextList.size(); j++) {
                boolean flag = !editTextList.get(j).isFocused();
                isFocus = isFocus && flag;
            }
            if (isFocus) {
                if (isKeyboardShown()) {
                    hideKeyboard();
                }
            }
        }
    };

    /**
     * 只起到延时开始显示的作用
     */
    private final Runnable showRun = this::showKeyboard;

    private final Runnable hideEnd = () -> {
        if (keyContainer.getVisibility() != View.GONE) {
            keyContainer.setVisibility(View.GONE);
        }
    };

    private final Runnable showEnd = () -> {
        // 在迅速点击不同输入框时, 造成自定义软键盘和系统软件盘不停的切换, 偶尔会出现停在使用系统键盘的输入框时, 没有隐藏
        // 自定义软键盘的情况, 为了杜绝这个现象, 加上下面这段代码
        if (mEditText != null && !mEditText.isFocused()) {
            hideKeyboard();
        }
    };

    private void showKeyboard() {
        keyContainer.setVisibility(View.VISIBLE);
        keyContainer.clearAnimation();
        keyContainer.startAnimation(showAnimation);
    }

    private boolean isLowCaseLetter(String str) {
        String letters = "abcdefghijklmnopqrstuvwxyz";
        return letters.contains(str);
    }

    private boolean isUpCaseLetter(String str) {
        String letters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        return letters.contains(str);
    }


    @SuppressLint("ClickableViewAccessibility")
    private void addListeners() {
        for (int i = 0; i < editTextList.size(); i++) {
            editTextList.get(i).setOnTouchListener((v, event) -> {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    hideSystemKeyBoard((EditText) v);
                    if (!isKeyboardShown()) {
                        showHandler.removeCallbacks(showRun);
                        showHandler.postDelayed(showRun, SHOW_DELAY);
                    }
                }
                return false;
            });

            editTextList.get(i).setOnFocusChangeListener((v, hasFocus) -> {
                // boolean result = isValidTouch();
                if (v instanceof EditText) {
                    if (hasFocus) {
                        updateKeyBoard(v);
                        hideSystemKeyBoard((EditText) v);
                        if (!isKeyboardShown()) {
                            showHandler.removeCallbacks(showRun);
                            showHandler.postDelayed(showRun, SHOW_DELAY);
                        }
                    } else {
                        checkHandler.removeCallbacks(checkFocus);
                        checkHandler.postDelayed(checkFocus, 20);
                    }
                }
            });
        }

    }

    private void updateKeyBoard(View v) {
        if (v.getTag() == null) {
            for (int j = 0; j < keyboardNumber.getKeys().size(); j++) {
                if (" ".equals(keyboardNumber.getKeys().get(j).label)) {
                    keyboardNumber.getKeys().get(j).label = "ABC";
                }

            }
            if (isLetterRandom) {
                randomLetter();
            }
            keyboardView.setKeyboard(keyboardLetter);

        } else {
            for (int j = 0; j < keyboardNumber.getKeys().size(); j++) {
                if ("ABC".equals(keyboardNumber.getKeys().get(j).label)) {
                    keyboardNumber.getKeys().get(j).label = " ";
                }

            }
            if (isNumberRandom) {
                randomNumbers();
            }
            keyboardView.setKeyboard(keyboardNumber);
        }
    }

    public boolean isShow() {
        return isKeyboardShown();
    }

    //隐藏系统键盘关键代码
    private void hideSystemKeyBoard(EditText edit) {
        this.mEditText = edit;
        InputMethodManager imm = (InputMethodManager) this.mContext.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm == null) {
            return;
        }
        boolean isOpen = imm.isActive();
        if (isOpen) {
            imm.hideSoftInputFromWindow(edit.getWindowToken(), 0);
        }

        int currentVersion = Build.VERSION.SDK_INT;
        String methodName = null;
        if (currentVersion >= 16) {
            methodName = "setShowSoftInputOnFocus";
        } else if (currentVersion >= 14) {
            methodName = "setSoftInputShownOnFocus";
        }

        if (methodName == null) {
            edit.setInputType(0);
        } else {
            try {
                Method setShowSoftInputOnFocus = EditText.class.getMethod(methodName, Boolean.TYPE);
                setShowSoftInputOnFocus.setAccessible(true);
                setShowSoftInputOnFocus.invoke(edit, Boolean.FALSE);
            } catch (NoSuchMethodException e) {
                edit.setInputType(0);
                e.printStackTrace();
            } catch (IllegalAccessException | InvocationTargetException |
                     IllegalArgumentException e) {
                e.printStackTrace();
            }
        }
    }

    private boolean isKeyboardShown() {
        return keyContainer.getVisibility() == View.VISIBLE;
    }

    private boolean isValidTouch() {
        long thisTouchTime = SystemClock.elapsedRealtime();
        if (thisTouchTime - lastTouchTime > 500) {
            lastTouchTime = thisTouchTime;
            return true;
        }
        lastTouchTime = thisTouchTime;
        return false;
    }

    public void setDelDrawable(Drawable delDrawable) {
        this.delDrawable = delDrawable;
        keyboardView.setDelDrawable(delDrawable);
    }

    public void setLowDrawable(Drawable lowDrawable) {
        this.lowDrawable = lowDrawable;
        keyboardView.setLowDrawable(lowDrawable);
    }

    public void setUpDrawable(Drawable upDrawable) {
        this.upDrawable = upDrawable;
        keyboardView.setUpDrawable(upDrawable);
    }


    private void randomLetter() {
        List<Keyboard.Key> keyList = keyboardLetter.getKeys();
        // 查找出a-z的数字键
        List<Keyboard.Key> newkeyList = new ArrayList<Keyboard.Key>();
        for (int i = 0; i < keyList.size(); i++) {
            if (keyList.get(i).label != null
                    && isword(keyList.get(i).label.toString())) {
                newkeyList.add(keyList.get(i));
            }
        }
        // 数组长度
        int count = newkeyList.size();
        // 结果集
        List<KeyModel> resultList = new ArrayList<KeyModel>();
        // 用一个LinkedList作为中介
        LinkedList<KeyModel> temp = new LinkedList<KeyModel>();
        // 初始化temp
        for (int i = 0; i < count; i++) {
            temp.add(new KeyModel(97 + i, "" + (char) (97 + i)));
        }
        //temp.add(new KeyModel(64, "" + (char) 64));//.
        // 取数
        Random rand = new Random();
        for (int i = 0; i < count; i++) {
            int num = rand.nextInt(count - i);
            resultList.add(new KeyModel(temp.get(num).getCode(), temp.get(num)
                    .getLable()));
            temp.remove(num);
        }
        for (int i = 0; i < newkeyList.size(); i++) {
            newkeyList.get(i).label = resultList.get(i).getLable();
            newkeyList.get(i).codes[0] = resultList.get(i).getCode();
        }

        //keyboardView.setKeyboard(keyAlp);
    }

    private boolean isword(String str) {
        String wordstr = "abcdefghijklmnopqrstuvwxyz";
        return wordstr.indexOf(str.toLowerCase()) > -1;
    }

    /**
     * 标点符号键盘-随机
     */

    private void randomSymbolkey() {
        List<Keyboard.Key> keyList = keyboardSymbol.getKeys();

        // 查找出标点符号的数字键
        List<Keyboard.Key> newkeyList = new ArrayList<Keyboard.Key>();
        for (int i = 0; i < keyList.size(); i++) {
            if (keyList.get(i).label != null
                    && isInterpunction(keyList.get(i).label.toString())) {
                newkeyList.add(keyList.get(i));
            }
        }
        // 数组长度
        int count = newkeyList.size();
        // 结果集
        List<KeyModel> resultList = new ArrayList<KeyModel>();
        // 用一个LinkedList作为中介
        LinkedList<KeyModel> temp = new LinkedList<KeyModel>();

        // 初始化temp
        temp.add(new KeyModel(33, "" + (char) 33));
        temp.add(new KeyModel(34, "" + (char) 34));
        temp.add(new KeyModel(35, "" + (char) 35));
        temp.add(new KeyModel(36, "" + (char) 36));
        temp.add(new KeyModel(37, "" + (char) 37));
        temp.add(new KeyModel(38, "" + (char) 38));
        temp.add(new KeyModel(39, "" + (char) 39));
        temp.add(new KeyModel(40, "" + (char) 40));
        temp.add(new KeyModel(41, "" + (char) 41));
        temp.add(new KeyModel(42, "" + (char) 42));
        temp.add(new KeyModel(43, "" + (char) 43));
        temp.add(new KeyModel(45, "" + (char) 45));
        temp.add(new KeyModel(47, "" + (char) 47));
        temp.add(new KeyModel(58, "" + (char) 58));
        temp.add(new KeyModel(59, "" + (char) 59));
        temp.add(new KeyModel(60, "" + (char) 60));
        temp.add(new KeyModel(61, "" + (char) 61));
        temp.add(new KeyModel(62, "" + (char) 62));
        temp.add(new KeyModel(63, "" + (char) 63));
        temp.add(new KeyModel(91, "" + (char) 91));
        temp.add(new KeyModel(92, "" + (char) 92));
        temp.add(new KeyModel(93, "" + (char) 93));
        temp.add(new KeyModel(94, "" + (char) 94));
        temp.add(new KeyModel(95, "" + (char) 95));
        temp.add(new KeyModel(96, "" + (char) 96));
        temp.add(new KeyModel(123, "" + (char) 123));
        temp.add(new KeyModel(124, "" + (char) 124));
        temp.add(new KeyModel(125, "" + (char) 125));
        temp.add(new KeyModel(126, "" + (char) 126));

        // 取数
        Random rand = new Random();
        for (int i = 0; i < count; i++) {
            int num = rand.nextInt(count - i);
            resultList.add(new KeyModel(temp.get(num).getCode(), temp.get(num)
                    .getLable()));
            temp.remove(num);
        }
        for (int i = 0; i < newkeyList.size(); i++) {
            newkeyList.get(i).label = resultList.get(i).getLable();
            newkeyList.get(i).codes[0] = resultList.get(i).getCode();
        }

        //keyboardView.setKeyboard(keyPun);
    }

    private boolean isInterpunction(String str) {
        String wordstr = "!\"#$%&()*+-\\/:;<=>?`'^_[]{|}~";
        return wordstr.indexOf(str) > -1;
    }

    public void setHideDrawable(Drawable downDrawable) {
        this.downDrawable = downDrawable;
        keyboardView.setHideDrawable(downDrawable);
    }

    public void setTitle(String title) {
        if (title != null) titleView.setText(title);
    }

    public boolean doBackKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
            if (isShow()) {
                hideKeyboard();
                return true;
            }
        }
        return false;
    }

    private boolean secEditAdd(EditText editText, String value, int position) {
        try {
            if (!checkIsSec(editText)) return true;
            //避免设置inputType之后影响输入内容
            if (editText.getInputType() == InputType.TYPE_CLASS_NUMBER) {
                editText.setInputType(InputType.TYPE_CLASS_TEXT);
            }
            ((EditChangeListener) mEditText).addText(value, position);
            editText.getText().insert(position, "•");
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return true;
        }
    }

    private void secEditDel(EditText editText, int position) {
        if (checkIsSec(editText)) {
            ((EditChangeListener) mEditText).delText(position);
        }
    }

    private boolean checkIsSec(EditText editText) {
        return editText instanceof EditChangeListener;
    }

}
