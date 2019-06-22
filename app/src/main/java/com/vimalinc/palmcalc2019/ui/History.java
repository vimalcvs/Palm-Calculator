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

package com.vimalinc.palmcalc2019.ui;

import android.content.SharedPreferences;
import android.widget.BaseAdapter;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Vector;
import java.util.regex.Pattern;

/**
 * Keeps track of the Calculation history
 *
 */
public class History {
	private static final int inVersion = 1;
	private static final int inMaxEntries = 100;
	Vector<HistoryEntry> mEntries = new Vector<HistoryEntry>();
	static String strPrefname = "myHistpref";
	private static SharedPreferences shPref;
	private int inPos;
	private BaseAdapter mObserver;

	History() {
		clear();
	}

	History(int inHVersion, DataInput in) throws IOException {
		if (inHVersion >= inVersion) {
			int inSize = in.readInt();
			for (int inI = 0; inI < inSize; ++inI) {
				mEntries.add(new HistoryEntry(inHVersion, in));
			}
			inPos = in.readInt();
		} else {
			throw new IOException("invalid inHVersion " + inHVersion);
		}
	}

	public void setObserver(BaseAdapter observer) {
		mObserver = observer;
	}

	private void notifyChanged() {
		if (mObserver != null) {
			mObserver.notifyDataSetChanged();
		}
	}

	void clear() {
		mEntries.clear();
		mEntries.add(new HistoryEntry(""));
		inPos = 0;
		notifyChanged();
	}

	void write(DataOutput out) throws IOException {
		out.writeInt(mEntries.size());
		for (HistoryEntry entry : mEntries) {
			entry.write(out);
		}
		out.writeInt(inPos);
	}

	public void update(String strText) {
		current().setEdited(strText);
	}

	public boolean moveToPrevious() {
		if (inPos > 0) {
			--inPos;
			return true;
		}
		return false;
	}

	public boolean moveToNext() {
		if (inPos < mEntries.size() - 1) {
			++inPos;
			return true;
		}
		return false;
	}

	public static void clearHistory() {
		SharedPreferences.Editor spEditor = shPref.edit();
		spEditor.clear();
		spEditor.commit();
	}

	public void enter(String strText) {
		current().clearEdited();
		SharedPreferences.Editor spEditor = shPref.edit();
		int inI = shPref.getInt("HistIndex", 0);
		if (mEntries.size() >= inMaxEntries) {
			mEntries.remove(0);
		}
		if (mEntries.size() < 2
				|| !strText.equals(mEntries.elementAt(mEntries.size() - 2)
						.getBase())) {
			if (isValidNumber(strText)) {
				spEditor.putString("hist" + inI, strText);
				spEditor.putInt("HistIndex", inI + 1);
				spEditor.commit();
			}
			mEntries.insertElementAt(new HistoryEntry(strText),
					mEntries.size() - 1);
		}
		inPos = mEntries.size() - 1;
		notifyChanged();
	}

	void list() {
		for (int inI = 0; inI < mEntries.size(); inI++) {
		}
	}

	public String getAns() {
		String strAns = "";
		for (int inI = 0; inI < mEntries.size(); inI++) {
			if (isValidNumber("" + mEntries.get(inI)))
				strAns = "" + mEntries.get(inI);
		}
		return strAns;
	}

	String[] getHistory() {
		String strAns[] = new String[mEntries.size()];
		int inTemp = 0;
		for (int inI = 0; inI < mEntries.size(); inI++) {
			if (isValidNumber("" + mEntries.get(inI))) {
				strAns[inTemp] = "" + mEntries.get(inI);
				inTemp++;
			}
		}
		return strAns;
	}

	HistoryEntry current() {
		return mEntries.elementAt(inPos);
	}

	public String getText() {
		return current().getEdited();
	}

	String getBase() {
		return current().getBase();
	}

	public static boolean isValidNumber(String strMystring) {
		if (strMystring.contains('\u2212' + "")) {
			strMystring = strMystring.replace('\u2212', '-');
		}
		final String Digits = "(\\p{Digit}+)";
		final String HexDigits = "(\\p{XDigit}+)";
		final String Exp = "[eE][+-]?" + Digits;
		final String fpRegex = ("[\\x00-\\x20]*" + "[+-]?(" + "NaN|"
				+ "Infinity|" + "((("
				+ Digits
				+ "(\\.)?("
				+ Digits
				+ "?)("
				+ Exp
				+ ")?)|"
				+ "(\\.("
				+ Digits
				+ ")("
				+ Exp
				+ ")?)|"
				+ "(("
				+ "(0[xX]"
				+ HexDigits
				+ "(\\.)?)|"
				+ "(0[xX]"
				+ HexDigits
				+ "?(\\.)"
				+ HexDigits
				+ ")" +

				")[pP][+-]?" + Digits + "))" + "[fFdD]?))" + "[\\x00-\\x20]*");
		if (Pattern.matches(fpRegex, strMystring)) {
			try {
				Double.valueOf(strMystring);
				return true;
			} catch (NumberFormatException e) {
				return false;
			}
		} else {
			return false;

		}
	}
}
