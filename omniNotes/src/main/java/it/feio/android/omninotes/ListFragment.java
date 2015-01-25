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
package it.feio.android.omninotes;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.AnimationDrawable;
import android.nfc.Tag;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.ViewPropertyAnimatorListener;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.SearchView.OnQueryTextListener;
import android.text.Html;
import android.text.TextUtils;
import android.util.Pair;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SubMenu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.getbase.floatingactionbutton.AddFloatingActionButton;
import com.getbase.floatingactionbutton.FloatingActionButton;
import com.getbase.floatingactionbutton.FloatingActionsMenu;
import com.neopixl.pixlui.components.textview.TextView;
import com.nhaarman.listviewanimations.itemmanipulation.DynamicListView;
import com.nhaarman.listviewanimations.itemmanipulation.swipedismiss.OnDismissCallback;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.keyboardsurfer.android.widget.crouton.Crouton;
import it.feio.android.checklistview.utils.DensityUtil;
import it.feio.android.omninotes.async.NoteLoaderTask;
import it.feio.android.omninotes.db.DbHelper;
import it.feio.android.omninotes.models.Attachment;
import it.feio.android.omninotes.models.Category;
import it.feio.android.omninotes.models.Note;
import it.feio.android.omninotes.models.ONStyle;
import it.feio.android.omninotes.models.UndoBarController;
import it.feio.android.omninotes.models.adapters.NavDrawerCategoryAdapter;
import it.feio.android.omninotes.models.adapters.NoteAdapter;
import it.feio.android.omninotes.models.listeners.AbsListViewScrollDetector;
import it.feio.android.omninotes.models.listeners.OnNotesLoadedListener;
import it.feio.android.omninotes.models.listeners.OnViewTouchedListener;
import it.feio.android.omninotes.models.views.InterceptorLinearLayout;
import it.feio.android.omninotes.utils.BitmapHelper;
import it.feio.android.omninotes.utils.Constants;
import it.feio.android.omninotes.utils.Display;
import it.feio.android.omninotes.utils.KeyboardUtils;
import it.feio.android.omninotes.utils.Navigation;
import it.feio.android.pixlui.links.UrlCompleter;

import static android.support.v4.view.ViewCompat.animate;


public class ListFragment extends Fragment implements OnNotesLoadedListener, OnViewTouchedListener, UndoBarController.UndoListener {

	static final int REQUEST_CODE_DETAIL = 1;
	private static final int REQUEST_CODE_CATEGORY = 2;
	private static final int REQUEST_CODE_CATEGORY_NOTES = 3;

	private DynamicListView list;
	private List<Note> selectedNotes = new ArrayList<Note>();
	private Note swipedNote;
	private List<Note> modifiedNotes = new ArrayList<Note>();
	private SearchView searchView;
	private MenuItem searchMenuItem;
	private Menu menu;
	private TextView empyListItem;
	private AnimationDrawable jinglesAnimation;
	private int listViewPosition;
	private int listViewPositionOffset;
	private boolean sendToArchive;
	private SharedPreferences prefs;
	private ListFragment mFragment;
	private android.support.v7.view.ActionMode actionMode;
	private boolean keepActionMode = false;

	// Undo archive/trash
	private boolean undoTrash = false;
	private boolean undoCategorize = false;
	private Category undoCategorizeCategory = null;
	// private Category removedCategory;
	private SparseArray<Note> undoNotesList = new SparseArray<Note>();
	// Used to remember removed categories from notes
	private Map<Note, Category> undoCategoryMap = new HashMap<Note, Category>();

	// Search variables
	private String searchQuery;
	private String searchTags;
	private boolean goBackOnToggleSearchLabel = false;
	private TextView listFooter;

	//    private NoteCardArrayMultiChoiceAdapter listAdapter;
	private NoteAdapter listAdapter;
	private int layoutSelected;
	private UndoBarController ubc;

	//    Fab
	private FloatingActionsMenu fab;
	private FloatingActionButton fabAddNote;
	private FloatingActionButton fabAddChecklist;
	private boolean fabAllowed;
	private boolean fabHidden = true;


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mFragment = this;
		prefs = getMainActivity().prefs;

		setHasOptionsMenu(true);
		setRetainInstance(false);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		if (savedInstanceState != null) {
			if (savedInstanceState.containsKey("listViewPosition")) {
				listViewPosition = savedInstanceState.getInt("listViewPosition");
				listViewPositionOffset = savedInstanceState.getInt("listViewPositionOffset");
				searchQuery = savedInstanceState.getString("searchQuery");
				searchTags = savedInstanceState.getString("searchTags");
			}
			keepActionMode = false;
		}
		return inflater.inflate(R.layout.fragment_list, container, false);
	}


	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		// Restores savedInstanceState
		if (savedInstanceState != null) {
			getMainActivity().navigationTmp = savedInstanceState.getString("navigationTmp");
		}

		// Easter egg initialization
		initEasterEgg();

		// Listview initialization
		initListView();
//        list = (CardListView) getActivity().findViewById(R.id.list);

		initFab();

		// Activity title initialization
		initTitle();

		ubc = new UndoBarController(getActivity().findViewById(R.id.undobar), this);
	}

	boolean fabExpanded = false;

	private void initFab() {
		fab = (FloatingActionsMenu) getActivity().findViewById(R.id.fab);
		AddFloatingActionButton fabAddButton = (AddFloatingActionButton) fab.findViewById(com.getbase
				.floatingactionbutton.R.id.fab_expand_menu_button);
		fabAddButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (fabExpanded) {
					fab.toggle();
					fabExpanded = false;
				} else {
					editNote(new Note(), v);
				}
			}
		});
		fabAddButton.setOnLongClickListener(new View.OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				fabExpanded = !fabExpanded;
				fab.toggle();
				return true;
			}
		});
		list.setOnScrollListener(
				new AbsListViewScrollDetector() {
					public void onScrollUp() {
						if (fab != null) {
							fab.collapse();
							hideFab();
						}
					}

					public void onScrollDown() {
						if (fab != null) {
							fab.collapse();
							showFab();
						}
					}
				});

		fab.findViewById(R.id.fab_checklist).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Note note = new Note();
				note.setChecklist(true);
				editNote(note, v);
			}
		});
		fab.findViewById(R.id.fab_camera).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent i = getActivity().getIntent();
				i.setAction(Constants.ACTION_TAKE_PHOTO);
				getActivity().setIntent(i);
				editNote(new Note(), v);
			}
		});

		// In KitKat bottom padding is added by navbar height
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
			int navBarHeight = Display.getNavigationBarHeightKitkat(getActivity());
			FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) fab.getLayoutParams();
			params.setMargins(params.leftMargin, params.topMargin, params.rightMargin,
					navBarHeight + DensityUtil.pxToDp(params.bottomMargin, getActivity()));
			fab.setLayoutParams(params);
		}
	}


	/**
	 * Activity title initialization based on navigation
	 */
	private void initTitle() {
		String[] navigationList = getResources().getStringArray(R.array.navigation_list);
		String[] navigationListCodes = getResources().getStringArray(R.array.navigation_list_codes);
		String navigation = prefs.getString(Constants.PREF_NAVIGATION, navigationListCodes[0]);
		int index = Arrays.asList(navigationListCodes).indexOf(navigation);
		CharSequence title = "";
		// If is a traditional navigation item
		if (index >= 0 && index < navigationListCodes.length) {
			title = navigationList[index];
		} else {
			ArrayList<Category> categories = DbHelper.getInstance(getActivity()).getCategories();
			for (Category tag : categories) {
				if (navigation.equals(String.valueOf(tag.getId()))) title = tag.getName();
			}
		}

		title = title == null ? getString(R.string.title_activity_list) : title;
		getMainActivity().setActionBarTitle(title.toString());
	}


	/**
	 * Starts a little animation on Mr.Jingles!
	 */
	private void initEasterEgg() {
		empyListItem = (TextView) getActivity().findViewById(R.id.empty_list);
		empyListItem.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (jinglesAnimation == null) {
					jinglesAnimation = (AnimationDrawable) empyListItem.getCompoundDrawables()[1];
					empyListItem.post(new Runnable() {
						public void run() {
							if (jinglesAnimation != null) jinglesAnimation.start();
						}
					});
				} else {
					stopJingles();
				}
			}
		});
	}


	private void stopJingles() {
		if (jinglesAnimation != null) {
			jinglesAnimation.stop();
			jinglesAnimation = null;
			empyListItem.setCompoundDrawablesWithIntrinsicBounds(0, R.animator.jingles_animation, 0, 0);

		}
	}


	@Override
	public void onPause() {
		super.onPause();
		stopJingles();
		Crouton.cancelAllCroutons();

		// Clears data structures
		// getSelectedNotes().clear();
//		if (listAdapter != null) {
//			listAdapter.clearSelectedItems();
//		}
		if (!keepActionMode) {
			commitPending();
			list.clearChoices();
			if (getActionMode() != null) {
				getActionMode().finish();
			}
		}
	}


	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		refreshListScrollPosition();
		outState.putInt("listViewPosition", listViewPosition);
		outState.putInt("listViewPositionOffset", listViewPositionOffset);
		outState.putString("searchQuery", searchQuery);
		outState.putString("searchTags", searchTags);
	}


	private void refreshListScrollPosition() {
		if (list != null) {
			listViewPosition = list.getFirstVisiblePosition();
			View v = list.getChildAt(0);
			listViewPositionOffset = (v == null) ? 0 : v.getTop();
		}
	}


	@SuppressWarnings("static-access")
	@Override
	public void onResume() {
		super.onResume();
		initNotesList(getActivity().getIntent());

		// Navigation drawer initialization to ensure data refresh
		getMainActivity().initNavigationDrawer();
		// Removes navigation drawer forced closed status
		if (getMainActivity().getDrawerLayout() != null) {
			getMainActivity().getDrawerLayout().setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
		}

		// Restores again DefaultSharedPreferences too reload in case of data
		// erased from Settings
		prefs = getActivity().getSharedPreferences(Constants.PREFS_NAME, getActivity().MODE_MULTI_PROCESS);

//        // Menu is invalidated to start again instructions tour if requested
//        if (!prefs.getBoolean(Constants.PREF_TOUR_PREFIX + "list", false)) {
//            getActivity().supportInvalidateOptionsMenu();
//        }
	}

	private final class ModeCallback implements android.support.v7.view.ActionMode.Callback {

		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			// Inflate the menu for the CAB
			MenuInflater inflater = mode.getMenuInflater();
			inflater.inflate(R.menu.menu_list, menu);
			actionMode = mode;

			fabAllowed = false;
			hideFab();

			return true;
		}


		@Override
		public void onDestroyActionMode(ActionMode mode) {
			// Here you can make any necessary updates to the activity when
			// the CAB is removed. By default, selected items are
			// deselected/unchecked.
			for (int i = 0; i < listAdapter.getSelectedItems().size(); i++) {
				int key = listAdapter.getSelectedItems().keyAt(i);
				View v = list.getChildAt(key - list.getFirstVisiblePosition());
				if (listAdapter.getCount() > key && listAdapter.getItem(key) != null && v != null) {
					listAdapter.restoreDrawable(listAdapter.getItem(key), v.findViewById(R.id.card_layout));
				}
			}

			// Backups modified notes in another structure to perform post-elaborations
			modifiedNotes = new ArrayList<Note>(getSelectedNotes());

			// Clears data structures
			selectedNotes.clear();
			listAdapter.clearSelectedItems();
			list.clearChoices();

			setFabAllowed(true);
			if (undoNotesList.size() == 0) {
				showFab();
			}

			actionMode = null;


			// Updates app widgets
			BaseActivity.notifyAppWidgets(getActivity());
		}


		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			prepareActionModeMenu();
			return true;
		}


		@Override
		public boolean onActionItemClicked(final ActionMode mode, final MenuItem item) {
			performAction(item, mode);
			return true;
		}
	}


	private void setFabAllowed(boolean allowed) {
		if (allowed) {
			boolean showFab = Navigation.checkNavigation(new Integer[]{Navigation.NOTES, Navigation.CATEGORY});
			if (showFab) {
				fabAllowed = true;
			}
		} else {
			fabAllowed = false;
		}
	}


	private void showFab() {
		if (fab != null && fabAllowed && isFabHidden()) {
			animateFab(0, View.VISIBLE, View.VISIBLE);
			fabHidden = false;
		}
	}


	private void hideFab() {
		if (fab != null && !isFabHidden()) {
			fab.collapse();
			animateFab(fab.getHeight() + getMarginBottom(fab), View.VISIBLE, View.INVISIBLE);
			fabHidden = true;
		}
	}


	private boolean isFabHidden() {
		return fabHidden;
	}


	private void animateFab(int translationY, final int visibilityBefore, final int visibilityAfter) {
		animate(fab).setInterpolator(new AccelerateDecelerateInterpolator())
				.setDuration(Constants.FAB_ANIMATION_TIME)
				.translationY(translationY)
				.setListener(new ViewPropertyAnimatorListener() {
					@Override
					public void onAnimationStart(View view) {
						fab.setVisibility(visibilityBefore);
					}

					@Override
					public void onAnimationEnd(View view) {
						fab.setVisibility(visibilityAfter);
					}

					@Override
					public void onAnimationCancel(View view) {
					}
				});
	}


	private int getMarginBottom(View view) {
		int marginBottom = 0;
		final ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
		if (layoutParams instanceof ViewGroup.MarginLayoutParams) {
			marginBottom = ((ViewGroup.MarginLayoutParams) layoutParams).bottomMargin;
		}
		return marginBottom;
	}


//    public void onCABItemClicked(final MenuItem item) {
//        Integer[] protectedActions = {R.id.menu_select_all, R.id.menu_merge};
//        if (!Arrays.asList(protectedActions).contains(item.getItemId())) {
//            getMainActivity().requestPassword(getActivity(), getSelectedNotes(), new PasswordValidator() {
//                @Override
//                public void onPasswordValidated(boolean passwordConfirmed) {
//                    if (passwordConfirmed) {
//                        performAction(item, getActionMode());
//                    }
//                }
//            });
//        } else {
//            performAction(item, getActionMode());
//        }
//    }

	public void finishActionMode() {
		if (getActionMode() != null) {
			getActionMode().finish();
		}
	}


	/**
	 * Manage check/uncheck of notes in list during multiple selection phase
	 */
	private void toggleListViewItem(View view, int position) {
		Note note = listAdapter.getItem(position);
		LinearLayout v = (LinearLayout) view.findViewById(R.id.card_layout);
		if (!getSelectedNotes().contains(note)) {
			getSelectedNotes().add(note);
			listAdapter.addSelectedItem(position);
			v.setBackgroundColor(getResources().getColor(R.color.list_bg_selected));
		} else {
			getSelectedNotes().remove(note);
			listAdapter.removeSelectedItem(position);
			listAdapter.restoreDrawable(note, v);
		}
		prepareActionModeMenu();

		// Close CAB if no items are selected
		if (getSelectedNotes().size() == 0) {
			finishActionMode();
		}

	}


	/**
	 * Notes list initialization. Data, actions and callback are defined here.
	 */
	private void initListView() {
		list = (DynamicListView) getActivity().findViewById(R.id.list);

		list.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
		list.setItemsCanFocus(false);

		// If device runs KitKat a footer is added to list to avoid
		// navigation bar transparency covering items
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
			int navBarHeight = Display.getNavigationBarHeightKitkat(getActivity());
			listFooter = new TextView(getActivity().getApplicationContext());
			listFooter.setHeight(navBarHeight);
			// To avoid useless events on footer
			listFooter.setOnClickListener(null);
			list.addFooterView(listFooter);
		}

		// Note long click to start CAB mode
		list.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView<?> arg0, View view, int position, long arg3) {
				if (view.equals(listFooter)) return true;
				if (getActionMode() != null) {
					return false;
				}
				// Start the CAB using the ActionMode.Callback defined above
				((MainActivity) getActivity()).startSupportActionMode(new ModeCallback());
				toggleListViewItem(view, position);
				setCabTitle();
				return true;
			}
		});

		// Note single click listener managed by the activity itself
		list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View view, int position, long arg3) {
				if (view.equals(listFooter)) return;
				if (getActionMode() == null) {
					editNote(listAdapter.getItem(position), view);
					return;
				}
				// If in CAB mode
				toggleListViewItem(view, position);
				setCabTitle();
			}
		});

		((InterceptorLinearLayout) getActivity().findViewById(R.id.list_root))
				.setOnViewTouchedListener(this);
	}


	private void zoomListItem(final View view, final Note note) {
		final long animationDuration = getResources().getInteger(R.integer.zooming_view_anim_time);

		final ImageView expandedImageView = getZoomListItemView(view, note);

		// Calculate the starting and ending bounds for the zoomed-in image.
		// This step involves lots of math. Yay, math.
		final Rect startBounds = new Rect();
		final Rect finalBounds = new Rect();
		final Point globalOffset = new Point();

		// The start bounds are the global visible rectangle of the thumbnail,
		// and the final bounds are the global visible rectangle of the container
		// view. Also set the container view's offset as the origin for the
		// bounds, since that's the origin for the positioning animation
		// properties (X, Y).
		view.getGlobalVisibleRect(startBounds);
		getActivity().findViewById(R.id.list_root)
				.getGlobalVisibleRect(finalBounds, globalOffset);
		startBounds.offset(-globalOffset.x, -globalOffset.y);
		finalBounds.offset(-globalOffset.x, -globalOffset.y);

		// Adjust the start bounds to be the same aspect ratio as the final
		// bounds using the "center crop" technique. This prevents undesirable
		// stretching during the animation. Also calculate the start scaling
		// factor (the end scaling factor is always 1.0).
		float startScale;
		if ((float) finalBounds.width() / finalBounds.height()
				> (float) startBounds.width() / startBounds.height()) {
			// Extend start bounds horizontally
			startScale = (float) startBounds.height() / finalBounds.height();
			float startWidth = startScale * finalBounds.width();
			float deltaWidth = (startWidth - startBounds.width()) / 2;
			startBounds.left -= deltaWidth;
			startBounds.right += deltaWidth;
		} else {
			// Extend start bounds vertically
			startScale = (float) startBounds.width() / finalBounds.width();
			float startHeight = startScale * finalBounds.height();
			float deltaHeight = (startHeight - startBounds.height()) / 2;
			startBounds.top -= deltaHeight;
			startBounds.bottom += deltaHeight;
		}

		// Hide the thumbnail and show the zoomed-in view. When the animation
		// begins, it will position the zoomed-in view in the place of the
		// thumbnail.
		view.setAlpha(0f);
		expandedImageView.setVisibility(View.VISIBLE);

		// Construct and run the parallel animation of the four translation and
		// scale properties (X, Y, SCALE_X, and SCALE_Y).
		AnimatorSet set = new AnimatorSet();
		set.play(ObjectAnimator.ofFloat(expandedImageView, View.X, startBounds.left, finalBounds.left))
				.with(ObjectAnimator.ofFloat(expandedImageView, View.Y, startBounds.top, finalBounds.top))
				.with(ObjectAnimator.ofFloat(expandedImageView, View.SCALE_X, startScale, 1f))
				.with(ObjectAnimator.ofFloat(expandedImageView, View.SCALE_Y, startScale, 1f));
		set.setDuration(animationDuration);
		set.setInterpolator(new DecelerateInterpolator());
		set.addListener(new AnimatorListenerAdapter() {
			@Override
			public void onAnimationEnd(Animator animation) {
				editNote2(note);
			}
		});
		set.start();
	}


	private ImageView getZoomListItemView(View view, Note note) {
		final ImageView expandedImageView = (ImageView) getActivity().findViewById(R.id.expanded_image);
		View targetView = null;
		if (note.getAttachmentsList().size() > 0) {
			targetView = view.findViewById(R.id.attachmentThumbnail);
		}
		if (targetView == null && note.getCategory() != null) {
			targetView = view.findViewById(R.id.category_marker);
		}
		if (targetView == null) {
			targetView = new ImageView(getActivity());
			targetView.setBackgroundColor(Color.WHITE);
		}
		targetView.setDrawingCacheEnabled(true);
		targetView.buildDrawingCache();
		Bitmap bmp = targetView.getDrawingCache();
		expandedImageView.setBackgroundColor(BitmapHelper.getDominantColor(bmp));
		return expandedImageView;
	}


	@Override
	public void onViewTouchOccurred(MotionEvent ev) {
		commitPending();
	}


	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.menu_list, menu);
		super.onCreateOptionsMenu(menu, inflater);
		this.menu = menu;
		// Initialization of SearchView
		initSearchView(menu);
//		initShowCase();
	}


	private void initSortingSubmenu() {
		final String[] arrayDb = getResources().getStringArray(R.array.sortable_columns);
		final String[] arrayDialog = getResources().getStringArray(R.array.sortable_columns_human_readable);
		int selected = Arrays.asList(arrayDb).indexOf(prefs.getString(Constants.PREF_SORTING_COLUMN, arrayDb[0]));

		SubMenu sortMenu = this.menu.findItem(R.id.menu_sort).getSubMenu();
		for (int i = 0; i < arrayDialog.length; i++) {
			if (sortMenu.findItem(i) == null) {
				sortMenu.add(Constants.MENU_SORT_GROUP_ID, i, i, arrayDialog[i]);
			}
			if (i == selected) sortMenu.getItem(i).setChecked(true);
		}
		sortMenu.setGroupCheckable(Constants.MENU_SORT_GROUP_ID, true, true);
	}


	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		setActionItemsVisibility(menu, false);
	}

	private void prepareActionModeMenu() {
		Menu menu = getActionMode().getMenu();
		int navigation = Navigation.getNavigation();

		if (navigation == Navigation.TRASH) {
			menu.findItem(R.id.menu_untrash).setVisible(true);
			menu.findItem(R.id.menu_delete).setVisible(true);
		} else {
			if (getSelectedCount() == 1) {
				menu.findItem(R.id.menu_share).setVisible(true);
				menu.findItem(R.id.menu_merge).setVisible(false);
			} else {
				menu.findItem(R.id.menu_share).setVisible(false);
				menu.findItem(R.id.menu_merge).setVisible(true);
			}
			menu.findItem(R.id.menu_category).setVisible(true);
			menu.findItem(R.id.menu_trash).setVisible(true);
		}
		menu.findItem(R.id.menu_select_all).setVisible(true);

		setCabTitle();
	}


	private int getSelectedCount() {
		return getSelectedNotes().size();
	}


	private void setCabTitle() {
		if (getActionMode() != null) {
			int title = getSelectedCount();
			getActionMode().setTitle(String.valueOf(title));
		}
	}


	/**
	 * SearchView initialization. It's a little complex because it's not using SearchManager but is implementing on its
	 * own.
	 */
	@SuppressLint("NewApi")
	private void initSearchView(final Menu menu) {

		// Save item as class attribute to make it collapse on drawer opening
		searchMenuItem = menu.findItem(R.id.menu_search);

		// Associate searchable configuration with the SearchView
		SearchManager searchManager = (SearchManager) getActivity().getSystemService(Context.SEARCH_SERVICE);
		searchView = (SearchView) MenuItemCompat.getActionView(menu.findItem(R.id.menu_search));
		searchView.setSearchableInfo(searchManager.getSearchableInfo(getActivity().getComponentName()));
		searchView.setImeOptions(EditorInfo.IME_ACTION_SEARCH);

		// Expands the widget hiding other actionbar icons
		searchView.setOnQueryTextFocusChangeListener(new OnFocusChangeListener() {
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				setActionItemsVisibility(menu, hasFocus);
//                if (!hasFocus) {
//                    MenuItemCompat.collapseActionView(searchMenuItem);
//                }
			}
		});

		MenuItemCompat.setOnActionExpandListener(searchMenuItem, new MenuItemCompat.OnActionExpandListener() {

			@Override
			public boolean onMenuItemActionCollapse(MenuItem item) {
				// Reinitialize notes list to all notes when search is
				// collapsed
				searchQuery = null;
				if (getActivity().findViewById(R.id.search_layout).getVisibility() == View.VISIBLE) {
					toggleSearchLabel(false);
				}
				getActivity().getIntent().setAction(Intent.ACTION_MAIN);
				initNotesList(getActivity().getIntent());
				getActivity().supportInvalidateOptionsMenu();
				return true;
			}


			@Override
			public boolean onMenuItemActionExpand(MenuItem item) {
				searchView.setOnQueryTextListener(new OnQueryTextListener() {
					@Override
					public boolean onQueryTextSubmit(String arg0) {
						return prefs.getBoolean("settings_instant_search", false);
					}

					@Override
					public boolean onQueryTextChange(String pattern) {
						View searchLayout = getActivity().findViewById(R.id.search_layout);
						if (prefs.getBoolean("settings_instant_search", false) && searchLayout != null) {
							searchTags = null;
							searchQuery = pattern;
							NoteLoaderTask mNoteLoaderTask = new NoteLoaderTask(mFragment, mFragment);
							mNoteLoaderTask.execute("getNotesByPattern", pattern);
							return true;
						} else {
							return false;
						}
					}
				});
				return true;
			}
		});
	}


	private void setActionItemsVisibility(Menu menu, boolean searchViewHasFocus) {
		// Defines the conditions to set actionbar items visible or not
		boolean drawerOpen = (getMainActivity().getDrawerLayout() != null && getMainActivity()
				.getDrawerLayout().isDrawerOpen(GravityCompat.START));
		boolean expandedView = prefs.getBoolean(Constants.PREF_EXPANDED_VIEW, true);
		boolean filterPastReminders = prefs.getBoolean(Constants.PREF_FILTER_PAST_REMINDERS, true);
		boolean navigationReminders = Navigation.checkNavigation(Navigation.REMINDERS);
		boolean navigationTrash = Navigation.checkNavigation(Navigation.TRASH);

		if (!navigationReminders && !navigationTrash) {
			setFabAllowed(true);
			if (!drawerOpen) {
				showFab();
			}
		} else {
			setFabAllowed(false);
			hideFab();
		}
		menu.findItem(R.id.menu_search).setVisible(!drawerOpen);
		menu.findItem(R.id.menu_filter).setVisible(!drawerOpen && !filterPastReminders && navigationReminders && !searchViewHasFocus);
		menu.findItem(R.id.menu_filter_remove).setVisible(!drawerOpen && filterPastReminders && navigationReminders && !searchViewHasFocus);
		menu.findItem(R.id.menu_sort).setVisible(!drawerOpen && !navigationReminders && !searchViewHasFocus);
		menu.findItem(R.id.menu_expanded_view).setVisible(!drawerOpen && !expandedView && !searchViewHasFocus);
		menu.findItem(R.id.menu_contracted_view).setVisible(!drawerOpen && expandedView && !searchViewHasFocus);
		menu.findItem(R.id.menu_empty_trash).setVisible(!drawerOpen && navigationTrash);
	}


	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		performAction(item, null);
		return super.onOptionsItemSelected(item);
	}


	/**
	 * Performs one of the ActionBar button's actions after checked notes protection
	 */
	public boolean performAction(MenuItem item, ActionMode actionMode) {
		if (actionMode == null) {
			switch (item.getItemId()) {
				case android.R.id.home:
					if (getMainActivity().getDrawerLayout().isDrawerOpen(GravityCompat.START)) {
						getMainActivity().getDrawerLayout().closeDrawer(GravityCompat.START);
					} else {
						getMainActivity().getDrawerLayout().openDrawer(GravityCompat.START);
					}
					break;
				case R.id.menu_filter:
					filterReminders(true);
					break;
				case R.id.menu_filter_remove:
					filterReminders(false);
					break;
				case R.id.menu_sort:
					initSortingSubmenu();
					break;
				case R.id.menu_expanded_view:
					switchNotesView();
					break;
				case R.id.menu_contracted_view:
					switchNotesView();
					break;
				case R.id.menu_empty_trash:
					emptyTrash();
					break;
			}
		} else {
			switch (item.getItemId()) {
				case R.id.menu_category:
					categorizeNotes();
					break;
				case R.id.menu_share:
					share();
					break;
				case R.id.menu_merge:
					merge();
					break;
				case R.id.menu_trash:
					trashNotes(true);
					break;
				case R.id.menu_untrash:
					trashNotes(false);
					break;
				case R.id.menu_delete:
					deleteNotes();
					break;
				case R.id.menu_select_all:
					selectAllNotes();
					break;
			}
		}

		checkSortActionPerformed(item);

		return super.onOptionsItemSelected(item);
	}


	private void switchNotesView() {
		boolean expandedView = prefs.getBoolean(Constants.PREF_EXPANDED_VIEW, true);
		prefs.edit().putBoolean(Constants.PREF_EXPANDED_VIEW, !expandedView).commit();
		// Change list view
		initNotesList(getActivity().getIntent());
		// Called to switch menu voices
		getActivity().supportInvalidateOptionsMenu();
	}


	void editNote(final Note note, final View view) {
		hideFab();
		zoomListItem(view, note);
	}


	void editNote2(Note note) {
		if (note.get_id() == 0) {

			// if navigation is a tag it will be set into note
			try {
				int tagId;
				if (!TextUtils.isEmpty(getMainActivity().navigationTmp)) {
					tagId = Integer.parseInt(getMainActivity().navigationTmp);
				} else {
					tagId = Integer.parseInt(getMainActivity().navigation);
				}
				note.setCategory(DbHelper.getInstance(getActivity()).getCategory(tagId));
			} catch (NumberFormatException e) {
			}
		} else {

		}

		// Current list scrolling position is saved to be restored later
		refreshListScrollPosition();

		// Fragments replacing
		getMainActivity().switchToDetail(note);
	}


	@Override
	public// Used to show a Crouton dialog after saved (or tried to) a note
	void onActivityResult(int requestCode, final int resultCode, Intent intent) {
		super.onActivityResult(requestCode, resultCode, intent);

		switch (requestCode) {
			case REQUEST_CODE_CATEGORY:
				// Dialog retarded to give time to activity's views of being
				// completely initialized
				// The dialog style is choosen depending on result code
				switch (resultCode) {
					case Activity.RESULT_OK:
						getMainActivity().showMessage(R.string.category_saved, ONStyle.CONFIRM);
						getMainActivity().initNavigationDrawer();
						break;
					case Activity.RESULT_FIRST_USER:
						getMainActivity().showMessage(R.string.category_deleted, ONStyle.ALERT);
						break;
					default:
						break;
				}

				break;

			case REQUEST_CODE_CATEGORY_NOTES:
				if (intent != null) {
					Category tag = intent.getParcelableExtra(Constants.INTENT_TAG);
					categorizeNotesExecute(tag);
				}
				break;

			default:
				break;
		}

	}


	private void checkSortActionPerformed(MenuItem item) {
		if (item.getGroupId() == Constants.MENU_SORT_GROUP_ID) {
			final String[] arrayDb = getResources().getStringArray(R.array.sortable_columns);
			prefs.edit().putString(Constants.PREF_SORTING_COLUMN, arrayDb[item.getOrder()]).commit();
			initNotesList(getActivity().getIntent());
			// Resets list scrolling position
			listViewPositionOffset = 0;
			listViewPosition = 0;
			list.setSelectionFromTop(listViewPosition, listViewPositionOffset);
			// Updates app widgets
			BaseActivity.notifyAppWidgets(getActivity());
		}
	}


	/**
	 * Empties trash deleting all the notes
	 */
	private void emptyTrash() {
//		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
//		alertDialogBuilder.setMessage(R.string.empty_trash_confirmation)
//				.setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialog, int id) {
//                        for (int i = 0; i < listAdapter.getCount(); i++) {
//                            getSelectedNotes().add(getSelectedNotes().get(i));
//                        }
//                        deleteNotesExecute();
//                    }
//                }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
//            @Override
//            public void onClick(DialogInterface dialog, int id) {
//            }
//        });
//		alertDialogBuilder.create().show();
		MaterialDialog dialog = new MaterialDialog.Builder(getActivity())
				.content(R.string.empty_trash_confirmation)
				.positiveText(R.string.ok)
				.callback(new MaterialDialog.SimpleCallback() {
					@Override
					public void onPositive(MaterialDialog materialDialog) {
						for (int i = 0; i < listAdapter.getCount(); i++) {
							getSelectedNotes().add(listAdapter.getItem(i));
						}
						deleteNotesExecute();
					}
				}).build();
		dialog.show();
	}


	/**
	 * Notes list adapter initialization and association to view
	 */
	void initNotesList(Intent intent) {


		NoteLoaderTask mNoteLoaderTask = new NoteLoaderTask(mFragment, mFragment);

		// Search for a tag
		// A workaround to simplify it's to simulate normal search
		if (Intent.ACTION_VIEW.equals(intent.getAction()) && intent.getCategories() != null
				&& intent.getCategories().contains(Intent.CATEGORY_BROWSABLE)) {
			searchTags = intent.getDataString().replace(UrlCompleter.HASHTAG_SCHEME, "");
			goBackOnToggleSearchLabel = true;
		}

		// Searching
		if (searchTags != null || searchQuery != null || Intent.ACTION_SEARCH.equals(intent.getAction())) {

			// Using tags
			if (searchTags != null && intent.getStringExtra(SearchManager.QUERY) == null) {
				searchQuery = searchTags;
				mNoteLoaderTask.execute("getNotesByTag", searchQuery);
			} else {
				// Get the intent, verify the action and get the query
				if (intent.getStringExtra(SearchManager.QUERY) != null) {
					searchQuery = intent.getStringExtra(SearchManager.QUERY);
					searchTags = null;
				}
				if (getMainActivity().loadNotesSync) {
					onNotesLoaded((ArrayList<Note>) DbHelper.getInstance(getActivity()).getNotesByPattern(searchQuery));
				} else {
					mNoteLoaderTask.execute("getNotesByPattern", searchQuery);
				}
				getMainActivity().loadNotesSync = Constants.LOAD_NOTES_SYNC;
			}

			toggleSearchLabel(true);

		} else {
			// Check if is launched from a widget with categories to set tag
			if ((Constants.ACTION_WIDGET_SHOW_LIST.equals(intent.getAction()) && intent
					.hasExtra(Constants.INTENT_WIDGET))
					|| !TextUtils.isEmpty(getMainActivity().navigationTmp)) {
				String widgetId = intent.hasExtra(Constants.INTENT_WIDGET) ? intent.getExtras()
						.get(Constants.INTENT_WIDGET).toString() : null;
				if (widgetId != null) {
					String sqlCondition = prefs.getString(Constants.PREF_WIDGET_PREFIX + widgetId, "");
					String pattern = DbHelper.KEY_CATEGORY + " = ";
					if (sqlCondition.lastIndexOf(pattern) != -1) {
						String tagId = sqlCondition.substring(sqlCondition.lastIndexOf(pattern) + pattern.length())
								.trim();
						getMainActivity().navigationTmp = !TextUtils.isEmpty(tagId) ? tagId : null;
					}
				}
				intent.removeExtra(Constants.INTENT_WIDGET);
				if (getMainActivity().loadNotesSync) {
					onNotesLoaded((ArrayList<Note>) DbHelper.getInstance(getActivity()).getNotesByCategory(
							getMainActivity().navigationTmp));
				} else {
					mNoteLoaderTask.execute("getNotesByTag", getMainActivity().navigationTmp);
				}
				getMainActivity().loadNotesSync = Constants.LOAD_NOTES_SYNC;

				// Gets all notes
			} else {
				if (getMainActivity().loadNotesSync) {
					onNotesLoaded((ArrayList<Note>) DbHelper.getInstance(getActivity()).getAllNotes(true));
				} else {
					mNoteLoaderTask.execute("getAllNotes", true);
				}
				getMainActivity().loadNotesSync = Constants.LOAD_NOTES_SYNC;
			}
		}
	}


	public void toggleSearchLabel(boolean activate) {
		View searchLabel = getActivity().findViewById(R.id.search_layout);
		boolean isActive = searchLabel.getVisibility() == View.VISIBLE;
		if (activate) {
			((android.widget.TextView) getActivity().findViewById(R.id.search_query)).setText(Html.fromHtml("<i>"
					+ getString(R.string.search) + ":</i> " + searchQuery));
			searchLabel.setVisibility(View.VISIBLE);
			getActivity().findViewById(R.id.search_cancel).setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					toggleSearchLabel(false);
				}
			});
		} else {
			if (isActive) {
				getActivity().findViewById(R.id.search_layout).setVisibility(View.GONE);
				searchTags = null;
				searchQuery = null;
				if (!goBackOnToggleSearchLabel) {
					getActivity().getIntent().setAction(Intent.ACTION_MAIN);
					if (searchView != null) {
						MenuItemCompat.collapseActionView(searchMenuItem);
					}
					initNotesList(getActivity().getIntent());
				} else {
					getActivity().onBackPressed();
				}
				goBackOnToggleSearchLabel = false;
				if (Intent.ACTION_VIEW.equals(getActivity().getIntent().getAction())) {
					getActivity().getIntent().setAction(null);
				}
			}
		}
	}


	@Override
	public void onNotesLoaded(ArrayList<Note> notes) {
		layoutSelected = prefs.getBoolean(Constants.PREF_EXPANDED_VIEW, true) ? R.layout.note_layout_expanded
				: R.layout.note_layout;

//        initCards(notes);


		listAdapter = new NoteAdapter(getActivity(), layoutSelected, notes);

		// A specifical behavior is performed basing on navigation
//		SwipeDismissAdapter adapter = new SwipeDismissAdapter(listAdapter, new OnDismissCallback() {
//			@Override
//            public void onDismiss(@NonNull ViewGroup viewGroup, @NonNull int[] reverseSortedPositions) {

		list.enableSwipeToDismiss(new OnDismissCallback() {
			@Override
			public void onDismiss(@NonNull ViewGroup viewGroup, @NonNull int[] reverseSortedPositions) {

				// Avoids conflicts with action mode
				finishActionMode();

				for (int position : reverseSortedPositions) {
					Note note;
					try {
						note = listAdapter.getItem(position);
					} catch (IndexOutOfBoundsException e) {

						continue;
					}
					getSelectedNotes().add(note);

					// Depending on settings and note status this action will...
					// ...restore
					if (Navigation.checkNavigation(Navigation.TRASH)) {
						trashNotes(false);
					}
					// ...removes category
					else if (Navigation.checkNavigation(Navigation.CATEGORY)) {
						categorizeNotesExecute(null);
					} else {
							trashNotes(true);
					}
				}
			}
		});
//		adapter.setAbsListView(list);
		list.setAdapter(listAdapter);

		// Replace listview with Mr. Jingles if it is empty
		if (notes.size() == 0) list.setEmptyView(getActivity().findViewById(R.id.empty_list));

		// Restores listview position when turning back to list
		if (list != null && notes.size() > 0) {
			if (list.getCount() > listViewPosition) {
				list.setSelectionFromTop(listViewPosition, listViewPositionOffset);
			} else {
				list.setSelectionFromTop(0, 0);
			}
		}

		// Fade in the list view
		animate(list).setDuration(getResources().getInteger(R.integer.list_view_fade_anim)).alpha(1);
	}


	/**
	 * Batch note trashing
	 */
	public void trashNotes(boolean trash) {
		int selectedNotesSize = getSelectedNotes().size();
		for (Note note : getSelectedNotes()) {
			// Restore it performed immediately, otherwise undo bar
			if (trash) {
				// Saves notes to be eventually restored at right position
				undoNotesList.put(listAdapter.getPosition(note) + undoNotesList.size(), note);
				modifiedNotes.add(note);
			} else {
				trashNote(note, false);
			}
			// Removes note adapter
			listAdapter.remove(note);
		}

		// If list is empty again Mr Jingles will appear again
		if (listAdapter.getCount() == 0)
			list.setEmptyView(getActivity().findViewById(R.id.empty_list));

		finishActionMode();

		// Advice to user
		if (trash) {
			getMainActivity().showMessage(R.string.note_trashed, ONStyle.WARN);
		} else {
			getMainActivity().showMessage(R.string.note_untrashed, ONStyle.INFO);
		}

		// Creation of undo bar
		if (trash) {
			ubc.showUndoBar(false, selectedNotesSize + " " + getString(R.string.trashed), null);
			hideFab();
			undoTrash = true;
		} else {
			getSelectedNotes().clear();
		}
		getMainActivity().initNavigationDrawer();
	}

	private android.support.v7.view.ActionMode getActionMode() {
		return actionMode;
	}


	private List<Note> getSelectedNotes() {
//        return listAdapter.getSelectedNotes();
		return selectedNotes;
	}


	/**
	 * Single note logical deletion
	 *
	 * @param note Note to be deleted
	 */
	@SuppressLint("NewApi")
	protected void trashNote(Note note, boolean trash) {
		DbHelper.getInstance(getActivity()).trashNote(note, trash);
		// Update adapter content
		listAdapter.remove(note);
		// Informs about update

	}


	/**
	 * Selects all notes in list
	 */
	private void selectAllNotes() {
		for (int i = 0; i < list.getChildCount(); i++) {
			LinearLayout v = (LinearLayout) list.getChildAt(i).findViewById(R.id.card_layout);
			// Checks null to avoid the footer
			if (v != null) {
				v.setBackgroundColor(getResources().getColor(R.color.list_bg_selected));
			}
		}
		selectedNotes.clear();
		for (int i = 0; i < listAdapter.getCount(); i++) {
			selectedNotes.add(listAdapter.getItem(i));
			listAdapter.addSelectedItem(i);
		}
		prepareActionModeMenu();
		setCabTitle();
	}


	/**
	 * Batch note permanent deletion
	 */
	private void deleteNotes() {
		// Confirm dialog creation
//		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
//		alertDialogBuilder.setMessage(R.string.delete_note_confirmation)
//				.setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
//					@Override
//					public void onClick(DialogInterface dialog, int id) {
//                        ((MainActivity)getActivity()).requestPassword(getActivity(), getSelectedNotes(), new PasswordValidator() {
//                            @Override
//                            public void onPasswordValidated(boolean passwordConfirmed) {
//                                if (passwordConfirmed) {
//                                    deleteNotesExecute();
//                                }
//                            }
//                        });
//					}
//				}).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
//					@Override
//					public void onClick(DialogInterface dialog, int id) {}
//				});
//		alertDialogBuilder.create().show();

		new MaterialDialog.Builder(getActivity())
				.content(R.string.delete_note_confirmation)
				.positiveText(R.string.ok)
				.callback(new MaterialDialog.SimpleCallback() {
					@Override
					public void onPositive(MaterialDialog materialDialog) {
						deleteNotesExecute();
					}
				}).build().show();
	}


	/**
	 * Performs notes permanent deletion after confirmation by the user
	 */
	private void deleteNotesExecute() {
		for (Note note : getSelectedNotes()) {
			listAdapter.remove(note);
			getMainActivity().deleteNote(note);
		}

		// Clears data structures
//		listAdapter.clearSelectedItems();
		list.clearChoices();

		finishActionMode();

		// If list is empty again Mr Jingles will appear again
		if (listAdapter.getCount() == 0)
			list.setEmptyView(getActivity().findViewById(R.id.empty_list));

		// Advice to user
		getMainActivity().showMessage(R.string.note_deleted, ONStyle.ALERT);

		getMainActivity().initNavigationDrawer();
	}

	/**
	 * Categories addition and editing
	 */
	void editCategory(Category category) {
		Intent categoryIntent = new Intent(getActivity(), CategoryActivity.class);
		categoryIntent.putExtra(Constants.INTENT_TAG, category);
		startActivityForResult(categoryIntent, REQUEST_CODE_CATEGORY);
	}


	/**
	 * Associates to or removes categories
	 */
	private void categorizeNotes() {
		// Retrieves all available categories
		final ArrayList<Category> categories = DbHelper.getInstance(getActivity()).getCategories();

		final MaterialDialog dialog = new MaterialDialog.Builder(getActivity())
				.title(R.string.categorize_as)
				.adapter(new NavDrawerCategoryAdapter(getActivity(), categories))
				.positiveText(R.string.add_category)
//                .neutralText(R.string.cancel)
				.negativeText(R.string.remove_category)
				.callback(new MaterialDialog.Callback() {
					@Override
					public void onPositive(MaterialDialog dialog) {
						keepActionMode = true;
						Intent intent = new Intent(getActivity(), CategoryActivity.class);
						intent.putExtra("noHome", true);
						startActivityForResult(intent, REQUEST_CODE_CATEGORY_NOTES);
					}

					//                    @Override
//                    public void onNegative(MaterialDialog materialDialog) {
//                        selectedNotes.clear();
//                        finishActionMode();
//                    }
					@Override
					public void onNegative(MaterialDialog dialog) {
						categorizeNotesExecute(null);
					}
				}).build();

		dialog.getListView().setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				dialog.dismiss();
				categorizeNotesExecute(categories.get(position));
			}
		});

//        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
//            @Override
//            public void onCancel(DialogInterface dialog) {
//                selectedNotes.clear();
//                finishActionMode();
//            }
//        });


//        }).setNeutralButton(R.string.remove_category, new DialogInterface.OnClickListener() {
//					@Override
//					public void onClick(DialogInterface dialog, int id) {
//                        categorizeNotesExecute(null);
//					}
//				}).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
//					@Override
//					public void onClick(DialogInterface dialog, int id) {
//
//					}
//				});

		dialog.show();
	}


	private void categorizeNotesExecute(Category category) {
		for (Note note : getSelectedNotes()) {
			// If is restore it will be done immediately, otherwise the undo bar
			// will be shown
			if (category != null) {
				categorizeNote(note, category);
			} else {
				// Saves categories associated to eventually undo
				undoCategoryMap.put(note, note.getCategory());
				// Saves notes to be eventually restored at right position
				undoNotesList.put(listAdapter.getPosition(note) + undoNotesList.size(), note);
				modifiedNotes.add(note);
			}
			// Update adapter content if actual navigation is the category
			// associated with actually cycled note
			if (Navigation.checkNavigation(Navigation.CATEGORY) && !Navigation.checkNavigationCategory(category)) {
				listAdapter.remove(note);
			} else {
				note.setCategory(category);
				listAdapter.replace(note, listAdapter.getPosition(note));
			}
		}

		// Clears data structures
//		listAdapter.clearSelectedItems();
//		list.clearChoices();
		finishActionMode();

		// Refreshes list
//		list.invalidateViews();

		// If list is empty again Mr Jingles will appear again
		if (listAdapter.getCount() == 0)
			list.setEmptyView(getActivity().findViewById(R.id.empty_list));

		// Refreshes navigation drawer if is set to show categories count numbers
		if (prefs.getBoolean("settings_show_category_count", false)) {
			getMainActivity().initNavigationDrawer();
		}

		if (getActionMode() != null) {
			getActionMode().finish();
		}

		// Advice to user
		String msg;
		if (category != null) {
			msg = getResources().getText(R.string.notes_categorized_as) + " '" + category.getName() + "'";
		} else {
			msg = getResources().getText(R.string.notes_category_removed).toString();
		}
		getMainActivity().showMessage(msg, ONStyle.INFO);

		// Creation of undo bar
		if (category == null) {
			ubc.showUndoBar(false, getString(R.string.notes_category_removed), null);
			hideFab();
			undoCategorize = true;
			undoCategorizeCategory = category;
		} else {
			getSelectedNotes().clear();
		}
	}


	private void categorizeNote(Note note, Category category) {
		note.setCategory(category);
		DbHelper.getInstance(getActivity()).updateNote(note, false);
	}

	@Override
	public void onUndo(Parcelable undoToken) {
		// Cycles removed items to re-insert into adapter
		for (Note note : modifiedNotes) {
			//   Manages uncategorize or archive  undo
			if ((undoCategorize && !Navigation.checkNavigationCategory(undoCategoryMap.get(note)))) {
				if (undoCategorize) {
					note.setCategory(undoCategoryMap.get(note));
				}

				listAdapter.replace(note, listAdapter.getPosition(note));
				listAdapter.notifyDataSetChanged();
				// Manages trash undo
			} else {
				list.insert(undoNotesList.keyAt(undoNotesList.indexOfValue(note)), note);
			}
		}

		selectedNotes.clear();
		undoNotesList.clear();
		modifiedNotes.clear();

		undoTrash = false;
		undoCategorize = false;
		undoNotesList.clear();
		undoCategoryMap.clear();
		undoCategorizeCategory = null;
		Crouton.cancelAllCroutons();

		if (getActionMode() != null) {
			getActionMode().finish();
		}
		ubc.hideUndoBar(false);
	}


	void commitPending() {
		if (undoTrash || undoCategorize) {

			for (Note note : modifiedNotes) {
				if (undoTrash)
					trashNote(note, true);
				else if (undoCategorize) categorizeNote(note, undoCategorizeCategory);
			}
			// Refreshes navigation drawer if is set to show categories count numbers
//            if (prefs.getBoolean("settings_show_category_count", false)) {
			getMainActivity().initNavigationDrawer();
//            }

			undoTrash = false;
			undoCategorize = false;
			undoCategorizeCategory = null;

			// Clears data structures
			selectedNotes.clear();
			modifiedNotes.clear();
			undoNotesList.clear();
			undoCategoryMap.clear();
			list.clearChoices();

			ubc.hideUndoBar(false);
			showFab();

			BaseActivity.notifyAppWidgets(getActivity());
		}
	}

	/**
	 * Shares the selected note from the list
	 */
	private void share() {
		// Only one note should be selected to perform sharing but they'll be cycled anyhow
		for (final Note note : getSelectedNotes()) {
			getMainActivity().shareNote(note);
		}

		getSelectedNotes().clear();
		if (getActionMode() != null) {
			getActionMode().finish();
		}
	}


	/**
	 * Merges all the selected notes
	 */
	public void merge() {

		Note mergedNote = null;
		boolean locked = false;
		StringBuilder content = new StringBuilder();
		ArrayList<Attachment> attachments = new ArrayList<Attachment>();

		for (Note note : getSelectedNotes()) {

			if (mergedNote == null) {
				mergedNote = new Note();
				mergedNote.setTitle(note.getTitle());
				content.append(note.getContent());

			} else {
				if (content.length() > 0
						&& (!TextUtils.isEmpty(note.getTitle()) || !TextUtils.isEmpty(note.getContent()))) {
					content.append(System.getProperty("line.separator")).append(System.getProperty("line.separator"))
							.append("----------------------").append(System.getProperty("line.separator"))
							.append(System.getProperty("line.separator"));
				}
				if (!TextUtils.isEmpty(note.getTitle())) {
					content.append(note.getTitle());
				}
				if (!TextUtils.isEmpty(note.getTitle()) && !TextUtils.isEmpty(note.getContent())) {
					content.append(System.getProperty("line.separator")).append(System.getProperty("line.separator"));
				}
				if (!TextUtils.isEmpty(note.getContent())) {
					content.append(note.getContent());
				}
			}

			locked = locked || note.isLocked();
			attachments.addAll(note.getAttachmentsList());
		}

		// Resets all the attachments id to force their note re-assign when saved
		for (Attachment attachment : attachments) {
			attachment.setId(0);
		}

		// Sets content text and attachments list
		mergedNote.setContent(content.toString());
		mergedNote.setLocked(locked);
		mergedNote.setAttachmentsList(attachments);

		getSelectedNotes().clear();
		if (getActionMode() != null) {
			getActionMode().finish();
		}

		// Sets the intent action to be recognized from DetailFragment and switch fragment
		getActivity().getIntent().setAction(Constants.ACTION_MERGE);
		getMainActivity().switchToDetail(mergedNote);
	}


	/**
	 * Excludes past reminders
	 */
	private void filterReminders(boolean filter) {
		prefs.edit().putBoolean(Constants.PREF_FILTER_PAST_REMINDERS, filter).commit();
		// Change list view
		initNotesList(getActivity().getIntent());
		// Called to switch menu voices
		getActivity().supportInvalidateOptionsMenu();
	}


	public MenuItem getSearchMenuItem() {
		return searchMenuItem;
	}

	private MainActivity getMainActivity() {
		return (MainActivity) getActivity();
	}


}
