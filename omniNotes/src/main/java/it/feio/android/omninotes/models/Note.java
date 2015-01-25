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
package it.feio.android.omninotes.models;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

import it.feio.android.omninotes.utils.date.DateHelper;

public class Note extends it.feio.android.omninotes.commons.models.Note implements Parcelable {

	// Not saved in DB
	private boolean passwordChecked = false;


	public Note() {
		super();
	}


	public Note(int _id, Long creation, Long lastModification, String title, String content, Integer trashed, String alarm, String latitude, String longitude, Category category, Integer locked,
			Integer checklist) {
		super(_id, creation, lastModification, title, content, trashed, alarm, latitude, longitude, category,
				locked, checklist);
	}


	public Note(Note note) {
		super(note);
		setPasswordChecked(note.isPasswordChecked());
	}


	private Note(Parcel in) {
		set_id(in.readInt());
		setCreation(in.readString());
		setLastModification(in.readString());
		setTitle(in.readString());
		setContent(in.readString());
		setTrashed(in.readInt());
		setAlarm(in.readString());
		setLatitude(in.readString());
		setLongitude(in.readString());
		setAddress(in.readString());
		setCategory((Category) in.readParcelable(Category.class.getClassLoader()));
		setLocked(in.readInt());
		setChecklist(in.readInt());
		in.readList(getAttachmentsList(), Attachment.class.getClassLoader());
	}


	public String getCreationShort(Context mContext) {
		return DateHelper.getDateTimeShort(mContext, getCreation());
	}


	public String getLastModificationShort(Context mContext) {
		return DateHelper.getDateTimeShort(mContext, getLastModification());
	}


	public String getAlarmShort(Context mContext) {
		if (getAlarm() == null) return "";
		return DateHelper.getDateTimeShort(mContext, Long.parseLong(getAlarm()));
	}


	public List<Attachment> getAttachmentsList() {
		return (List<Attachment>)super.getAttachmentsList();
	}


	public void setAttachmentsList(ArrayList<Attachment> attachmentsList) {
		super.setAttachmentsList(attachmentsList);
	}


	public void addAttachment(Attachment attachment) {
        List<Attachment> attachmentsList = ((List<Attachment>)super.getAttachmentsList());
        attachmentsList.add(attachment);
        setAttachmentsList(attachmentsList);
	}


	public List<Attachment> getAttachmentsListOld() {
        return (List<Attachment>)super.getAttachmentsListOld();
	}


	public void setAttachmentsListOld(ArrayList<Attachment> attachmentsListOld) {
		super.setAttachmentsListOld(attachmentsListOld);
	}


	public boolean isPasswordChecked() {
		return passwordChecked;
	}


	public void setPasswordChecked(boolean passwordChecked) {
		this.passwordChecked = passwordChecked;
	}
	
	@Override
	public Category getCategory() {
		return (Category) super.getCategory();
	}


	@Override
	public int describeContents() {
		return 0;
	}


	@Override
	public void writeToParcel(Parcel parcel, int flags) {
		parcel.writeInt(get_id());
		parcel.writeString(String.valueOf(getCreation()));
		parcel.writeString(String.valueOf(getLastModification()));
		parcel.writeString(getTitle());
		parcel.writeString(getContent());
		parcel.writeInt(isTrashed() ? 1 : 0);
		parcel.writeString(getAlarm());
		parcel.writeString(String.valueOf(getLatitude()));
		parcel.writeString(String.valueOf(getLongitude()));
		parcel.writeString(getAddress());
		parcel.writeParcelable(getCategory(), 0);
		parcel.writeInt(isLocked() ? 1 : 0);
		parcel.writeInt(isChecklist() ? 1 : 0);
		parcel.writeList(getAttachmentsList());
	}

	/*
	 * Parcelable interface must also have a static field called CREATOR, which is an object implementing the
	 * Parcelable.Creator interface. Used to un-marshal or de-serialize object from Parcel.
	 */
	public static final Parcelable.Creator<Note> CREATOR = new Parcelable.Creator<Note>() {

		public Note createFromParcel(Parcel in) {
			return new Note(in);
		}


		public Note[] newArray(int size) {
			return new Note[size];
		}
	};
}
