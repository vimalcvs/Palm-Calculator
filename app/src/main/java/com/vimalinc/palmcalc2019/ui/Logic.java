package com.vimalinc.palmcalc2019.ui;

import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;

import org.javia.arity.Symbols;
import org.javia.arity.SyntaxException;

import java.util.Locale;

/**
 * @author Adam Howard
 * @since 2019-06-07
 */
public abstract class Logic {
    protected static final char chMinus = '-';
    protected static final String INFINITY_UNICODE = "\u221e";
    protected static final String INFINITY = "Infinity";
    public final static int DELETE_MODE_BACKSPACE = 0;
    protected Symbols mSymbols = new Symbols();
    protected String mResult = "";
    protected int mLineLength = 0;
    protected CalculatorDisplay mDisplay;
    protected History mHistory;
    protected String mErrorString;
    protected boolean mIsError = false;

    private Listener mListener;
    private int mDeleteMode = DELETE_MODE_BACKSPACE;
    private final static int DELETE_MODE_CLEAR = 1;
    private static final String NaN = "NaN";
    private static final String MARKER_EVALUATE_ON_RESUME = "?";

    public Logic(History history, CalculatorDisplay display) {
        mHistory = history;
        mDisplay = display;
    }

    public void insert(String delta) {
        Log.d("Display", delta);
        mDisplay.insert(delta);
        setDeleteMode(DELETE_MODE_BACKSPACE);
    }

    public abstract String evaluate(String expr) throws SyntaxException;

    private boolean isOperator(String strText) {
        return strText.length() == 1 && isOperator(strText.charAt(0));
    }

    public void resumeWithHistory() {
        clearWithHistory(false);
    }

    public void onEnter() {
        if (mDeleteMode == DELETE_MODE_CLEAR) {
            clearWithHistory(false); // clear after an Enter on result
        } else {
            evaluateAndShowResult(getText(), CalculatorDisplay.Scroll.UP);
        }
    }

    public abstract void evaluateAndShowResult(String text, CalculatorDisplay.Scroll scroll);

    private void clearWithHistory(boolean scroll) {
        String text = mHistory.getText();
        if (MARKER_EVALUATE_ON_RESUME.equals(text)) {
            if (!mHistory.moveToPrevious()) {
                text = "";
            }
            text = mHistory.getText();
            evaluateAndShowResult(text, CalculatorDisplay.Scroll.NONE);
        } else {
            mResult = "";
            mDisplay.setText(text, scroll ? CalculatorDisplay.Scroll.UP
                    : CalculatorDisplay.Scroll.NONE);
            mIsError = false;
        }
    }

    public int getDeleteMode() {
        return mDeleteMode;
    }

    boolean acceptInsert(String delta) {
        String text = getText();
        return !mIsError
                && (!mResult.equals(text) || isOperator(delta) || mDisplay
                .getSelectionStart() != text.length());
    }

    protected boolean isOperator(char c) {
        // plus minus times div
        return "+-\u00d7\u00f7/*".indexOf(c) != -1;
    }

    public void onDown() {
        String text = getText();
        if (!text.equals(mResult)) {
            mHistory.update(text);
        }
        if (mHistory.moveToNext()) {
            mDisplay.setText(mHistory.getText(), CalculatorDisplay.Scroll.UP);
        }
    }

    protected String getText() {
        return mDisplay.getText().toString();
    }

    public void onUp() {
        String strText = getText();
        if (!strText.equals(mResult)) {
            mHistory.update(strText);
        }
        if (mHistory.moveToPrevious()) {
            mDisplay.setText(mHistory.getText(), CalculatorDisplay.Scroll.DOWN);
        }
    }

    public interface Listener {
        void onDeleteModeChange();
    }

    public void setDeleteMode(int mode) {
        if (mDeleteMode != mode) {
            mDeleteMode = mode;
            mListener.onDeleteModeChange();
        }
    }

    void cleared() {
        mResult = "";
        mIsError = false;
        updateHistory();
        setDeleteMode(DELETE_MODE_BACKSPACE);
    }

    void updateHistory() {
        String text = getText();
        // Don't set the ? marker for empty text or the error string.
        // There is no need to evaluate those later.
        if (!TextUtils.isEmpty(text)
                && !TextUtils.equals(text, mErrorString)
                && text.equals(mResult)) {
            mHistory.update(MARKER_EVALUATE_ON_RESUME);
        } else {
            mHistory.update(getText());
        }
    }

    public void setListener(Listener listener) {
        this.mListener = listener;
    }

    public void onDelete() {
        if (getText().equals(mResult) || mIsError) {
            clear(false);
        } else {
            mDisplay.dispatchKeyEvent(new KeyEvent(0, KeyEvent.KEYCODE_DEL));
            mResult = "";
        }
    }

    protected void clear(boolean isScroll) {
        mHistory.enter("");
        mDisplay.setText("", isScroll ? CalculatorDisplay.Scroll.UP
                : CalculatorDisplay.Scroll.NONE);
        cleared();
    }

    public void onClear() {
        clear(mDeleteMode == DELETE_MODE_CLEAR);
    }

    public void setLineLength(int inDigits) {
        mLineLength = inDigits;
    }

    // To format the orginal result with predefined precision
    protected String tryFormattingWithPrecision(double value, int precision) {
        // The standard scientific formatter is basically what we need. We will
        // start with what it produces and then massage it a bit.
        String result = String.format(Locale.US, "%" + mLineLength + "."
                + precision + "g", value);
        if (result.equals(NaN)) { // treat NaN as Error
            mIsError = true;
            return mErrorString;
        }
        String mantissa = result;
        String exponent = null;
        int e = result.indexOf('e');
        if (e != -1) {
            mantissa = result.substring(0, e);

            // Strip "+" and unnecessary 0's from the exponent
            exponent = result.substring(e + 1);
            if (exponent.startsWith("+")) {
                exponent = exponent.substring(1);
            }
            exponent = String.valueOf(Integer.parseInt(exponent));
        } else {
            mantissa = result;
        }

        int period = mantissa.indexOf('.');
        if (period == -1) {
            period = mantissa.indexOf(',');
        }
        if (period != -1) {
            // Strip trailing 0's
            while (mantissa.length() > 0 && mantissa.endsWith("0")) {
                mantissa = mantissa.substring(0, mantissa.length() - 1);
            }
            if (mantissa.length() == period + 1) {
                mantissa = mantissa
                        .substring(0, mantissa.length() - 1);
            }
        }

        if (exponent != null) {
            result = mantissa + 'e' + exponent;
        } else {
            result = mantissa;
        }
        return result;
    }
}
