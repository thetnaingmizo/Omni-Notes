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

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LightingColorFilter;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import com.neopixl.pixlui.components.textview.TextView;
import it.feio.android.omninotes.BaseActivity;
import it.feio.android.omninotes.ListFragment;
import it.feio.android.omninotes.R;
import it.feio.android.omninotes.models.Category;
import it.feio.android.omninotes.utils.Constants;

import java.util.ArrayList;

public class NavDrawerCategoryAdapter extends BaseAdapter {

	private Activity mActivity;
	private int layout;
	private ArrayList<Category> categories;
	private LayoutInflater inflater;

	public NavDrawerCategoryAdapter(Activity mActivity, ArrayList<Category> categories) {
		this(mActivity, categories, null);		
	}

	public NavDrawerCategoryAdapter(Activity mActivity, ArrayList<Category> categories, String navigationTmp) {
		this.mActivity = mActivity;
		this.layout = R.layout.drawer_list_item;		
		this.categories = categories;	
		inflater = (LayoutInflater) mActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);		
	}

	@Override
	public int getCount() {
		return categories.size();
	}

	@Override
	public Object getItem(int position) {
		return categories.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	public View getView(int position, View convertView, ViewGroup parent) {
		
		// Finds elements
		Category category = categories.get(position);
		
		NoteDrawerCategoryAdapterViewHolder holder;
	    if (convertView == null) {
	    	convertView = inflater.inflate(layout, parent, false);

	    	holder = new NoteDrawerCategoryAdapterViewHolder();
    		    	
	    	holder.imgIcon = (ImageView) convertView.findViewById(R.id.icon);
	    	holder.txtTitle = (TextView) convertView.findViewById(R.id.title);
	    	holder.count = (android.widget.TextView) convertView.findViewById(R.id.count);
	    	convertView.setTag(holder);
	    } else {
	        holder = (NoteDrawerCategoryAdapterViewHolder) convertView.getTag();
	    }
	
		// Set the results into TextViews	
	    holder.txtTitle.setText(category.getName());
		
	    if (isSelected(parent, position)) {
//			holder.txtTitle.setTextColor(mActivity.getResources().getColor(
//					R.color.drawer_text_selected));
			holder.txtTitle.setTypeface(null,Typeface.BOLD);
		} else {
//			holder.txtTitle.setTextColor(mActivity.getResources().getColor(
//					R.color.text_color));
			holder.txtTitle.setTypeface(null,Typeface.NORMAL);
		}

		// Set the results into ImageView checking if an icon is present before
		if (category.getColor() != null && category.getColor().length() > 0) {
			Drawable img = mActivity.getResources().getDrawable(R.drawable.square);
			ColorFilter cf = new LightingColorFilter(Color.parseColor("#000000"), Integer.parseInt(category.getColor()));
			// Before API 16 the object is mutable yet
			if (Build.VERSION.SDK_INT >= 16) {
				img.mutate().setColorFilter(cf);
			} else {
				img.setColorFilter(cf);				
			}
			holder.imgIcon.setImageDrawable(img);
			int padding = 12;
			holder.imgIcon.setPadding(padding,padding,padding,padding);
		}
		
		// Sets category count if set in preferences
		if (mActivity.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_MULTI_PROCESS).getBoolean("settings_show_category_count", false)) {
			holder.count.setText(String.valueOf(category.getCount()));
		}

		return convertView;
	}

	
	
	private boolean isSelected(ViewGroup parent, int position) {	
		
		// Getting actual navigation selection
		String[] navigationListCodes = mActivity.getResources().getStringArray(
				R.array.navigation_list_codes);
		
		// Managing temporary navigation indicator when coming from a widget
		String navigationTmp = ListFragment.class.isAssignableFrom(mActivity
				.getClass()) ? ((BaseActivity) mActivity).getNavigationTmp()
				: null;
				
		String navigation = navigationTmp != null ? navigationTmp
				: mActivity.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_MULTI_PROCESS)
						.getString(Constants.PREF_NAVIGATION,
								navigationListCodes[0]);
		
		if (navigation.equals(String.valueOf(categories.get(position).getId()))) {
			return true;
		} else {
			return false;
		}			
	}

}



/**
 * Holder object
 * @author fede
 *
 */
class NoteDrawerCategoryAdapterViewHolder {	
	ImageView imgIcon;
	TextView txtTitle;
	android.widget.TextView count;
}
