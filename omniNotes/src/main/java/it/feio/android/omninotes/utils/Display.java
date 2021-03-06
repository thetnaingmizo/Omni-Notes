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

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Build;
import android.support.v7.app.ActionBarActivity;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.WindowManager;


public class Display {

	@SuppressWarnings("deprecation")
	@SuppressLint("NewApi")
	public static Point getUsableSize(Context mContext) {
		Point displaySize = new Point();
		try {
			WindowManager manager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
			if (manager != null) {
				android.view.Display display = manager.getDefaultDisplay();
				if (display != null) {
					if (android.os.Build.VERSION.SDK_INT < 13) {
						displaySize.set(display.getWidth(), display.getHeight());
					} else {
						display.getSize(displaySize);
					}
				}
			}
		} catch (Exception e) {

		}
		return displaySize;
	}

	public static Point getVisibleSize(View view) {
		Point displaySize = new Point();
		Rect r = new Rect();
		view.getWindowVisibleDisplayFrame(r);
		displaySize.x = r.right - r.left;
		displaySize.y = r.bottom - r.top;
		return displaySize;
	}

	public static int getStatusBarHeight(Context mContext) {
		int result = 0;
		int resourceId = mContext.getResources().getIdentifier("status_bar_height", "dimen", "android");
		if (resourceId > 0) {
			result = mContext.getResources().getDimensionPixelSize(resourceId);
		}
		return result;
	}

	@SuppressLint("NewApi")
	public static int getActionbarHeight(Object mObject) {
		int res = 0;
		if (Activity.class.isAssignableFrom(mObject.getClass())) {
			res = ((Activity) mObject).getActionBar().getHeight();
		} else if (ActionBarActivity.class.isAssignableFrom(mObject.getClass())) {
			res = ((ActionBarActivity) mObject).getSupportActionBar().getHeight();
		}
		return res;
	}

	@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
	public static Point getScreenDimensions(Context mContext) {
		WindowManager wm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
		android.view.Display display = wm.getDefaultDisplay();
		Point size = new Point();
		DisplayMetrics metrics = new DisplayMetrics();
		display.getRealMetrics(metrics);
		size.x = metrics.widthPixels;
		size.y = metrics.heightPixels;
		return size;
	}


	@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
	public static int getNavigationBarHeightKitkat(Context mContext) {
		return getScreenDimensions(mContext).y - getUsableSize(mContext).y;
	}


}
