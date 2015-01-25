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
package it.feio.android.omninotes.models.adapters;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.text.Spanned;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.neopixl.pixlui.components.textview.TextView;
import com.nhaarman.listviewanimations.util.Insertable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.RejectedExecutionException;

import it.feio.android.omninotes.R;
import it.feio.android.omninotes.async.TextWorkerTask;
import it.feio.android.omninotes.db.DbHelper;
import it.feio.android.omninotes.models.Attachment;
import it.feio.android.omninotes.models.Note;
import it.feio.android.omninotes.models.holders.NoteViewHolder;
import it.feio.android.omninotes.models.views.SquareImageView;
import it.feio.android.omninotes.utils.BitmapHelper;
import it.feio.android.omninotes.utils.Constants;
import it.feio.android.omninotes.utils.Navigation;
import it.feio.android.omninotes.utils.TextHelper;


public class NoteAdapter extends ArrayAdapter<Note> implements Insertable {

	private final Activity mActivity;


	public List<Note> getNotes() {
		return notes;
	}


	private List<Note> notes = new ArrayList<Note>();
	private SparseBooleanArray selectedItems = new SparseBooleanArray();
	private boolean expandedView;
	private int layout;
	private LayoutInflater inflater;


	public NoteAdapter(Activity activity, int layout, List<Note> notes) {
		super(activity, R.layout.note_layout_expanded, notes);
		this.mActivity = activity;
		this.notes = notes;
		this.layout = layout;

		expandedView = layout == R.layout.note_layout_expanded;
		inflater = (LayoutInflater) mActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	@SuppressLint("NewApi")
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {

		Note note = notes.get(position);

		NoteViewHolder holder;
		if (convertView == null) {
			convertView = inflater.inflate(layout, parent, false);

			holder = new NoteViewHolder();

			holder.root = convertView.findViewById(R.id.root);
			holder.cardLayout = convertView.findViewById(R.id.card_layout);
			holder.categoryMarker = convertView.findViewById(R.id.category_marker);

			holder.title = (TextView) convertView.findViewById(R.id.note_title);
			holder.content = (TextView) convertView.findViewById(R.id.note_content);
			holder.date = (TextView) convertView.findViewById(R.id.note_date);

			holder.locationIcon = (ImageView) convertView.findViewById(R.id.locationIcon);
			holder.alarmIcon = (ImageView) convertView.findViewById(R.id.alarmIcon);
			if (!expandedView)
				holder.attachmentIcon = (ImageView) convertView.findViewById(R.id.attachmentIcon);

			holder.attachmentThumbnail = (SquareImageView) convertView.findViewById(R.id.attachmentThumbnail);

			convertView.setTag(holder);

		} else {
			holder = (NoteViewHolder) convertView.getTag();
		}

		initText(note, holder);

		initIcons(note, holder);

		initDates(note, holder);


		// Highlighted if is part of multiselection of notes. Remember to search for child with card ui
		if (selectedItems.get(position)) {
			holder.cardLayout.setBackgroundColor(mActivity.getResources().getColor(
					R.color.list_bg_selected));
		} else {
			restoreDrawable(note, holder.cardLayout, holder);
		}
		initThumbnail(note, holder);


//		Animation animation = AnimationUtils.loadAnimation(mActivity, R.animator.fade_in_support);
//		animation.setDuration(60);
//		convertView.startAnimation(animation);

		return convertView;
	}


	private void initThumbnail(Note note, NoteViewHolder holder) {
		// Attachment thumbnail
		if (expandedView) {
			// If note is locked or without attachments nothing is shown
			if ((note.isLocked() && !mActivity.getSharedPreferences(Constants.PREFS_NAME, mActivity.MODE_MULTI_PROCESS).getBoolean("settings_password_access", false))
					|| note.getAttachmentsList().size() == 0) {
				holder.attachmentThumbnail.setVisibility(View.GONE);
			}
			// Otherwise...
			else {
				holder.attachmentThumbnail.setVisibility(View.VISIBLE);
				Attachment mAttachment = note.getAttachmentsList().get(0);
				Uri thumbnailUri = BitmapHelper.getThumbnailUri(mActivity, mAttachment);
				Glide.with(mActivity)
						.load(thumbnailUri)
						.centerCrop()
						.crossFade()
						.into(holder.attachmentThumbnail);
			}
		}
	}


	private void initDates(Note note, NoteViewHolder holder) {
		String dateText = getDateText(mActivity, note);
		holder.date.setText(dateText);
	}


	private void initIcons(Note note, NoteViewHolder holder) {
		// ...the location
		holder.locationIcon.setVisibility(note.getLongitude() != null && note.getLongitude() != 0 ? View.VISIBLE : View.GONE);

		// ...the presence of an alarm
		holder.alarmIcon.setVisibility(note.getAlarm() != null ? View.VISIBLE : View.GONE);
		// ...the attachment icon for contracted view
		if (!expandedView) {
			holder.attachmentIcon.setVisibility(note.getAttachmentsList().size() > 0 ? View.VISIBLE : View.GONE);
		}
	}


	private void initText(Note note, NoteViewHolder holder) {
		try {
			if (note.isChecklist()) {
				TextWorkerTask task = new TextWorkerTask(mActivity, holder.title, holder.content, expandedView);
				task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, note);
			} else {
				Spanned[] titleAndContent = TextHelper.parseTitleAndContent(mActivity, note);
				holder.title.setText(titleAndContent[0]);
				holder.content.setText(titleAndContent[1]);
				holder.title.setText(titleAndContent[0]);
				if (titleAndContent[1].length() > 0) {
					holder.content.setText(titleAndContent[1]);
					holder.content.setVisibility(View.VISIBLE);
				} else {
					if (expandedView) {
						holder.content.setVisibility(View.INVISIBLE);
					} else {
						holder.content.setVisibility(View.GONE);
					}
				}
			}
		} catch (RejectedExecutionException e) {

		}
	}


	/**
	 * Choosing which date must be shown depending on sorting criteria
	 *
	 * @return String ith formatted date
	 */
	public static String getDateText(Context mContext, Note note) {
		String dateText;
		String sort_column;
		SharedPreferences prefs = mContext.getSharedPreferences(Constants.PREFS_NAME, mContext.MODE_MULTI_PROCESS);

		// Reminder screen forces sorting
		if (Navigation.checkNavigation(Navigation.REMINDERS)) {
			sort_column = DbHelper.KEY_REMINDER;
		} else {
			sort_column = prefs.getString(Constants.PREF_SORTING_COLUMN, "");
		}

		// Creation
		if (sort_column.equals(DbHelper.KEY_CREATION)) {
			dateText = mContext.getString(R.string.creation) + " " + note.getCreationShort(mContext);
		}
		// Reminder
		else if (sort_column.equals(DbHelper.KEY_REMINDER)) {
			String alarmShort = note.getAlarmShort(mContext);

			if (alarmShort.length() == 0) {
				dateText = mContext.getString(R.string.no_reminder_set);
			} else {
				dateText = mContext.getString(R.string.alarm_set_on) + " "
						+ note.getAlarmShort(mContext);
			}
		}
		// Others
		else {
			dateText = mContext.getString(R.string.last_update) + " "
					+ note.getLastModificationShort(mContext);
		}
		return dateText;
	}


	public SparseBooleanArray getSelectedItems() {
		return selectedItems;
	}

	public void addSelectedItem(Integer selectedItem) {
		this.selectedItems.put(selectedItem, true);
	}

	public void removeSelectedItem(Integer selectedItem) {
		this.selectedItems.delete(selectedItem);
	}

	public void clearSelectedItems() {
		this.selectedItems.clear();
	}


	public void restoreDrawable(Note note, View v) {
		restoreDrawable(note, v, null);
	}


	public void restoreDrawable(Note note, View v, NoteViewHolder holder) {
		final int paddingBottom = v.getPaddingBottom(), paddingLeft = v.getPaddingLeft();
		final int paddingRight = v.getPaddingRight(), paddingTop = v.getPaddingTop();
		v.setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom);
		colorNote(note, v, holder);
	}


	@SuppressWarnings("unused")
	private void colorNote(Note note, View v) {
		colorNote(note, v, null);
	}

	/**
	 * Color of category marker if note is categorized a function is active in preferences
	 */
	private void colorNote(Note note, View v, NoteViewHolder holder) {

		String colorsPref = mActivity.getSharedPreferences(Constants.PREFS_NAME, mActivity.MODE_MULTI_PROCESS)
				.getString("settings_colors_app", Constants.PREF_COLORS_APP_DEFAULT);

		// Checking preference
		if (!colorsPref.equals("disabled")) {

			// Resetting transparent color to the view
			v.setBackgroundColor(Color.parseColor("#00000000"));

			// If category is set the color will be applied on the appropriate target
			if (note.getCategory() != null && note.getCategory().getColor() != null) {
				if (colorsPref.equals("complete") || colorsPref.equals("list")) {
					v.setBackgroundColor(Integer.parseInt(note.getCategory().getColor()));
				} else {
					if (holder != null) {
						holder.categoryMarker.setBackgroundColor(Integer.parseInt(note.getCategory().getColor()));
					} else {
						v.findViewById(R.id.category_marker).setBackgroundColor(Integer.parseInt(note.getCategory().getColor()));
					}
				}
			} else {
				v.findViewById(R.id.category_marker).setBackgroundColor(0);
			}
		}
	}


	/**
	 * Replaces notes
	 */
	public void replace(Note note, int index) {
		if (notes.indexOf(note) != -1) {
			notes.remove(index);
		} else {
			index = notes.size();
		}
		notes.add(index, note);
	}


	@Override
	public void add(int i, @NonNull Object o) {
		insert((Note) o, i);

	}
}



