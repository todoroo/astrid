/*
 * Copyright (C) 2011 Peter Kuterna
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.peterkuterna.android.apps.pinnedheader;

import android.app.ListActivity;

@SuppressWarnings("nls")
public class PinnedHeaderActivity extends ListActivity {

	/**
	 * List of names generated with http://www.identitygenerator.com
	 */
	private static final String [] names = {
		"Geoffrey Hampton",
		"Ciaran Holcomb",
		"Marshall Kelly",
		"Mufutau Saunders",
		"Ishmael Durham",
		"Brock Golden",
		"Dalton Britt",
		"Tad Wright",
		"Carl Olsen",
		"Jack Cote",
		"Damian Carpenter",
		"Burke Cochran",
		"Sebastian Mcmahon",
		"Talon Stout",
		"Anthony Johnston",
		"Calvin Howell",
		"Simon Hale",
		"Talon Leon",
		"Stephen Mayo",
		"Ezra Graham",
		"Ryan Juarez",
		"Nathan Bowman",
		"Kermit Mcclure",
		"Axel Rhodes",
		"David Maynard",
		"Wing Larsen",
		"Noah Buchanan",
		"Nathan Mayer",
		"Nigel Mccormick",
		"Herrod Rivera",
		"Armando Meyers",
		"Colin Velasquez",
		"Zeus Brooks",
		"Hilel Stafford",
		"Merrill Russo",
		"Cole Lang",
		"Dieter Velez",
		"Lance Stokes",
		"Jarrod Oneil",
		"Louis Robbins",
		"Daquan Mclean",
		"Dorian Wong",
		"Nicholas Adams",
		"Kaseem Holt",
		"Kevin Alvarado",
		"Clarke Munoz",
		"Logan Holmes",
		"Kennedy Moody",
		"Joshua Barker",
		"Jamal David"
	};

//	static {
//		Arrays.sort(names);
//	}
//
//	private NamesAdapter mAdapter;
//
//    private int mPinnedHeaderBackgroundColor;
//    private int mPinnedHeaderTextColor;
//
//	@Override
//	public void onCreate(Bundle savedInstanceState) {
//	    super.onCreate(savedInstanceState);
//
//	    setContentView(R.layout.activity_pinnedheader);
//
//	    mAdapter = new NamesAdapter(this, R.layout.list_item, android.R.id.text1, names);
//	    setListAdapter(mAdapter);
//
//	    mPinnedHeaderBackgroundColor = getResources().getColor(R.color.pinned_header_background);
//	    mPinnedHeaderTextColor = getResources().getColor(R.color.pinned_header_text);
//
//	    setupListView();
//	}
//
//	private void setupListView() {
//        PinnedHeaderListView listView = (PinnedHeaderListView) findViewById(android.R.id.list);
//	    listView.setPinnedHeaderView(LayoutInflater.from(this).inflate(R.layout.list_item_header, listView, false));
//	    listView.setOnScrollListener(mAdapter);
//	    listView.setDividerHeight(0);
//	}
//
//    private final class NamesAdapter extends ArrayAdapter<String> implements SectionIndexer, OnScrollListener, PinnedHeaderAdapter {
//
//		private SectionIndexer mIndexer;
//
//		public NamesAdapter(Context context, int resourceId, int textViewResourceId, String[] objects) {
//			super(context, resourceId, textViewResourceId, objects);
//
//			this.mIndexer = new StringArrayAlphabetIndexer(objects, "ABCDEFGHIJKLMNOPQRSTUVWXYZ");
//		}
//
//        @Override
//		public View getView(int position, View convertView, ViewGroup parent) {
//			View v = super.getView(position, convertView, parent);
//
//			bindSectionHeader(v, position);
//
//			return v;
//		}
//
//        private void bindSectionHeader(View itemView, int position) {
//        	final TextView headerView = (TextView) itemView.findViewById(R.id.header_text);
//        	final View dividerView = itemView.findViewById(R.id.list_divider);
//
//	        final int section = getSectionForPosition(position);
//	        if (getPositionForSection(section) == position) {
//	            String title = (String) mIndexer.getSections()[section];
//	            headerView.setText(title);
//	            headerView.setVisibility(View.VISIBLE);
//		    	dividerView.setVisibility(View.GONE);
//	        } else {
//	        	headerView.setVisibility(View.GONE);
//		    	dividerView.setVisibility(View.VISIBLE);
//	        }
//
//	        // move the divider for the last item in a section
//	        if (getPositionForSection(section + 1) - 1 == position) {
//		    	dividerView.setVisibility(View.GONE);
//	        } else {
//		    	dividerView.setVisibility(View.VISIBLE);
//	        }
//        }
//
//		public int getPositionForSection(int sectionIndex) {
//            if (mIndexer == null) {
//                return -1;
//            }
//
//            return mIndexer.getPositionForSection(sectionIndex);
//        }
//
//		public int getSectionForPosition(int position) {
//            if (mIndexer == null) {
//                return -1;
//            }
//
//            return mIndexer.getSectionForPosition(position);
//        }
//
//		@Override
//		public Object[] getSections() {
//            if (mIndexer == null) {
//                return new String[] { " " };
//            } else {
//                return mIndexer.getSections();
//            }
//		}
//
//		@Override
//		public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
//            if (view instanceof PinnedHeaderListView) {
//                ((PinnedHeaderListView) view).configureHeaderView(firstVisibleItem);
//            }
//		}
//
//		@Override
//		public void onScrollStateChanged(AbsListView arg0, int arg1) {
//		}
//
//		@Override
//		public int getPinnedHeaderState(int position) {
//            if (mIndexer == null || getCount() == 0) {
//                return PINNED_HEADER_GONE;
//            }
//
//            if (position < 0) {
//                return PINNED_HEADER_GONE;
//            }
//
//            // The header should get pushed up if the top item shown
//            // is the last item in a section for a particular letter.
//            int section = getSectionForPosition(position);
//            int nextSectionPosition = getPositionForSection(section + 1);
//
//            if (nextSectionPosition != -1 && position == nextSectionPosition - 1) {
//                return PINNED_HEADER_PUSHED_UP;
//            }
//
//            return PINNED_HEADER_VISIBLE;
//		}
//
//		@Override
//		public void configurePinnedHeader(View v, int position, int alpha) {
//			TextView header = (TextView) v;
//
//			final int section = getSectionForPosition(position);
//			final String title = (String) getSections()[section];
//
//			header.setText(title);
//			if (alpha == 255) {
//				header.setBackgroundColor(mPinnedHeaderBackgroundColor);
//				header.setTextColor(mPinnedHeaderTextColor);
//			} else {
//				header.setBackgroundColor(Color.argb(alpha,
//						Color.red(mPinnedHeaderBackgroundColor),
//						Color.green(mPinnedHeaderBackgroundColor),
//						Color.blue(mPinnedHeaderBackgroundColor)));
//				header.setTextColor(Color.argb(alpha,
//						Color.red(mPinnedHeaderTextColor),
//						Color.green(mPinnedHeaderTextColor),
//						Color.blue(mPinnedHeaderTextColor)));
//			}
//		}
//
//	}

}
