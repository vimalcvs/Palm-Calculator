/*
 * <Palmcalc is a multipurpose application consisting of calculators, converters
 * and world clock> Copyright (C) <2013> <Cybrosys Technologies pvt. ltd.>
 * 
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 **/

package com.vimalinc.palmcalc2019.basic;

import com.vimalinc.palmcalc2019.ui.CalculatorDisplay;
import com.vimalinc.palmcalc2019.ui.History;
import com.vimalinc.palmcalc2019.ui.Logic;

import android.util.Log;
import android.widget.EditText;
import org.javia.arity.SyntaxException;

class BasicLogic extends Logic {

	BasicLogic(History history, CalculatorDisplay display) {
        super(history, display);
		mErrorString = "Error";
		mDisplay.setLogic(this);
	}

	boolean eatHorizontalMove(boolean toLeft) {
		EditText editText = mDisplay.getEditText();
		int cursorPos = editText.getSelectionStart();
		return toLeft ? cursorPos == 0 : cursorPos >= editText.length();
	}

	public void evaluateAndShowResult(String text, CalculatorDisplay.Scroll scroll) {
		try {
			String result = evaluate(text);
			Log.d("RESULT", result);
			if (!text.equals(result)) {
				mHistory.enter(text);
				mResult = result;
				mDisplay.setText(mResult, scroll);
			}
		} catch (SyntaxException e) {
			mIsError = true;
			mResult = mErrorString;
			mDisplay.setText(mResult, scroll);
		}
	}

	@Override
	public String evaluate(String input) throws SyntaxException {
		if (input.trim().equals("")) {
			return "";
		}
		int size = input.length();
		while (size > 0 && isOperator(input.charAt(size - 1))) {
			input = input.substring(0, size - 1);
			--size;
		}

		double value = mSymbols.eval(input);

		String result = "";
		for (int precision = mLineLength; precision > 6; precision--) {
			result = tryFormattingWithPrecision(value, precision);
			if (result.length() <= mLineLength) {
				break;
			}
		}
		return result.replace(INFINITY, INFINITY_UNICODE);
	}
}
