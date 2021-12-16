package me.coffee.widget.keyboard;

/**
 * Author: xuan
 * Created on 2021/12/13 11:15.
 * <p>
 * Describe:
 */
public interface EditChangeListener {

    void addText(String value, int position);

    void delText(int position);
}
