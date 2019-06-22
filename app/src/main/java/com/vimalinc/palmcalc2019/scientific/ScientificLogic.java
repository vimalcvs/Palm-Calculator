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

package com.vimalinc.palmcalc2019.scientific;

import com.vimalinc.palmcalc2019.ui.CalculatorDisplay;
import com.vimalinc.palmcalc2019.ui.CalculatorDisplay.Scroll;
import com.vimalinc.palmcalc2019.palmcalc.R;
import com.vimalinc.palmcalc2019.ui.History;
import com.vimalinc.palmcalc2019.ui.HistoryAdapter;
import com.vimalinc.palmcalc2019.ui.Logic;

import android.widget.EditText;
import android.content.Context;

import java.text.DecimalFormat;
import java.text.NumberFormat;

import org.javia.arity.SyntaxException;

class ScientificLogic extends Logic {

	private static int inDeg = 1;
	private HistoryAdapter hst;
	private String strCurrentMode = "[DEG]";
	private int inModeFlag = 0;

	ScientificLogic(Context context, History history, CalculatorDisplay display) {
	    super(history, display);
		mErrorString = context.getResources().getString(R.string.error);
		mDisplay.setLogic(this);
		hst = new HistoryAdapter(context, mHistory, this);
	}

	boolean eatHorizontalMove(boolean isToLeft) {
		EditText etxtEditText = mDisplay.getEditText();
		ScientificCalcFragment.imm.hideSoftInputFromWindow(
				etxtEditText.getWindowToken(), 0);
		int cursorPos = etxtEditText.getSelectionStart();
		return isToLeft ? cursorPos == 0 : cursorPos >= etxtEditText.length();
	}

	void onShow() {
		insert(mHistory.getAns());
	}

	// returns history
	String[] HistorySize() {
		String strAns[] = new String[hst.getCount()];
		int inJ = hst.getCount() - 1;
		if (hst.getCount() > 0)
			for (int inI = 0; inI < hst.getCount(); inI++) {
				strAns[inI] = "" + hst.getItem(inJ);
				inJ--;
			}
		return strAns;
	}

	// handle DEG change
	public String checkMode(String strText) {

		String strDisplay = strText;
		if (inDeg == 1) {
			if (strDisplay.contains("sin(")) {
				strDisplay = strDisplay.replace("sin(", "sind(");
			}
			if (strDisplay.contains("cos(")) {
				strDisplay = strDisplay.replace("cos(", "cosd(");
			}
			if (strDisplay.contains("tan(")) {
				strDisplay = strDisplay.replace("tan(", "tand(");
			}
			return strDisplay;
		} else
			return strDisplay;

	}

	// to check number of open brackets and closed brackets are same in the
	// input
	public static Boolean getBracketCount(String strText) {
		int inBracketOCount = 0;
		int inBracketCCount = 0;
		if (strText.contains("(")) {
			for (final char ch : strText.toCharArray()) {
				switch (ch) {
				case '(':
					++inBracketOCount;
					break;

				case ')':
					++inBracketCCount;
					break;
				}
			}
			if (inBracketOCount == inBracketCCount)
				return true;
			else
				return false;
		} else
			return true;
	}

	// function shows the result by reading input
	public void evaluateAndShowResult(String strText, Scroll scroll) {
		if (getBracketCount(strText)) {
			try {
				String strReslt = evaluate(checkMode(strText));
				if (!strText.equals(strReslt) && !strText.equalsIgnoreCase("")) {
					mHistory.enter(strReslt);
					mResult = strReslt;
					mDisplay.setText(mResult, scroll);
				}
			} catch (SyntaxException inExp) {
				mIsError = true;
				mResult = mErrorString;
				mDisplay.setText(mResult, scroll);
			}
		} else {
			mIsError = true;
			mResult = mErrorString;
			mDisplay.setText(mResult, scroll);
		}
	}

	/**to evaluate the result of calculation*/
	@Override
	public String evaluate(String strInput) throws SyntaxException {
		if (strInput.trim().equals("")) {
			return "";
		}
		double dblValue = mSymbols.eval(strInput);
		String strReslt = "";
		if (ScientificCalcFragment.txtvFSE.getText().toString()
				.equalsIgnoreCase("Floatpt")) {
			for (int inPrecision = mLineLength; inPrecision > 6; inPrecision--) {
				strReslt = tryFormattingWithPrecision(dblValue, inPrecision);
				if (strReslt.length() <= mLineLength) {
					break;
				}
			}
		} else if (ScientificCalcFragment.txtvFSE.getText().toString()
				.contains("FIX")) {
			strReslt = EventListener.fix(dblValue,
					PreferenceClass.getMyIntPref(EventListener.ctx));

		} else if (ScientificCalcFragment.txtvFSE.getText().toString()
				.contains("SCI")) {
			strReslt = EventListener.sci(dblValue,
					PreferenceClass.getMyIntPref(EventListener.ctx));
		}
		return strReslt.replace('-', chMinus).replace(INFINITY,
                INFINITY_UNICODE);
	}

	private String formater(String strReslt) {
		String strMantissa = strReslt;
		int inLen = 0;
		int inPeriod = strMantissa.indexOf('.');
		if (inPeriod != -1) {
			String strSub = strMantissa.substring(inPeriod);
			inLen = strSub.length();

		}
		String strHash = ".";
		if (inLen != 0) {
			for (int i = 0; i < inLen - 1; i++) {
				strHash = strHash.concat("#");
			}
		}
		System.out.println(strHash);
		String strFormat = "#,##0" + strHash;

		NumberFormat formatter = new DecimalFormat(strFormat);

		return formatter.format(Double.parseDouble(strReslt));
	}

	// To handle DEG change event
	public void onDegChange() {
		if (strCurrentMode.equalsIgnoreCase(ScientificCalcFragment.txtvDeg
				.getText().toString())) {
			ScientificCalcFragment.txtvDeg.setText("[RAD]");
			inDeg = 0;
		} else {
			ScientificCalcFragment.txtvDeg.setText("[DEG]");
			inDeg = 1;
		}
	}

	// To handle ALT press event
	public void onShiftPress() {
		ScientificCalcFragment.txtvShift.setText("[ALT]");

	}

	// To return display text
	public String getDisplayText() {
		return mDisplay.getText().toString();
	}

	// To handle hyp press event
	public void onHypPress() {
		ScientificCalcFragment.txtvHyp.setText("[HYP]");
	}

	// To handle FSE press event
	public void onFSEChange() {
		inModeFlag = 1;
	}
}
