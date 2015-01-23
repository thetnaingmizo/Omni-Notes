/*
 * Copyright (C) 2015 Federico Iosue (federico.iosue@gmail.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package it.feio.android.omninotes.utils;

import android.content.Context;
import android.text.Html;
import android.text.Spanned;
import android.text.SpannedString;

import java.util.Locale;

import it.feio.android.omninotes.models.Note;


public class TextHelper {
	/**
	 * @param note
	 * @return
	 */
	public static Spanned[] parseTitleAndContent(Context mContext, Note note) {

		final int CONTENT_SUBSTRING_LENGTH = 300;
		final int TITLE_SUBSTRING_OF_CONTENT_LIMIT = 50;

		// Defining title and content texts
		String titleText, contentText;

		String content = note.getContent().trim();

		if (note.getTitle().length() > 0) {
			titleText = note.getTitle();
			contentText = limit(note.getContent().trim(), 0, CONTENT_SUBSTRING_LENGTH, false, true);
		} else {
			titleText = limit(content, 0, TITLE_SUBSTRING_OF_CONTENT_LIMIT, true, false);
			contentText = limit(content.replace(titleText, "").trim(), 0, CONTENT_SUBSTRING_LENGTH, false, false);
		}
		content = null;

		// Masking title and content string if note is locked
		if (note.isLocked()
				&& !mContext.getSharedPreferences(Constants.PREFS_NAME, mContext.MODE_MULTI_PROCESS).getBoolean(
				"settings_password_access", false)) {
			// This checks if a part of content is used as title and should be partially masked
			if (!note.getTitle().equals(titleText) && titleText.length() > 3) {
				titleText = limit(titleText, 0, 4, false, false);
			}
			contentText = "";
		}

		// Replacing checkmarks symbols with html entities
		Spanned titleSpanned, contentSpanned;
		if (note.isChecklist()) {
			titleText = titleText.replace(it.feio.android.checklistview.interfaces.Constants.CHECKED_SYM,
					it.feio.android.checklistview.interfaces.Constants.CHECKED_ENTITY).replace(
					it.feio.android.checklistview.interfaces.Constants.UNCHECKED_SYM,
					it.feio.android.checklistview.interfaces.Constants.UNCHECKED_ENTITY);
			titleSpanned = Html.fromHtml(titleText);
			contentText = contentText
					.replace(it.feio.android.checklistview.interfaces.Constants.CHECKED_SYM,
							it.feio.android.checklistview.interfaces.Constants.CHECKED_ENTITY)
					.replace(it.feio.android.checklistview.interfaces.Constants.UNCHECKED_SYM,
							it.feio.android.checklistview.interfaces.Constants.UNCHECKED_ENTITY)
					.replace(System.getProperty("line.separator"), "<br/>");
			contentSpanned = Html.fromHtml(contentText);
		} else {
			titleSpanned = new SpannedString(titleText);
			contentSpanned = new SpannedString(contentText);
		}

		return new Spanned[]{titleSpanned, contentSpanned};
	}


	public static String limit(String value, int start, int length, boolean singleLine, boolean elipsize) {
		if (start > value.length()) {
			return null;
		}
		StringBuilder buf = new StringBuilder(value.substring(start));
		int indexNewLine = buf.indexOf(System.getProperty("line.separator"));
		int endIndex = singleLine && indexNewLine < length ? indexNewLine : length < buf.length() ? length : -1;
		if (endIndex != -1) {
			buf.setLength(endIndex);
			if (elipsize) {
				buf.append("...");
			}
		}
		return buf.toString();
	}


	public static String capitalize(String string) {
		return string.substring(0, 1).toUpperCase(Locale.getDefault()) + string.substring(1, string.length()).toLowerCase(Locale.getDefault());
	}

}
