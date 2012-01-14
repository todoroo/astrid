Change Log
==========

Version 2.2.2 *(2011-12-31)*
----------------------------

 * Fix incorrect `R.java` imports in all of the sample activities.


Version 2.2.1 *(2011-12-31)*
----------------------------

 * New `setTypeface(Typeface)` and `getTypeface()` methods for title indicator.
   (Thanks Dimitri Fedorov)
 * Added styled tab indicator sample.
 * Support for widths other than those that could be measured exactly.


Version 2.2.0 *(2011-12-13)*
----------------------------

 * Default title indicator style is now 'underline'.
 * Title indicator now allows specifying an `OnCenterItemClickListener` which
   will give you callbacks when the current item title has been clicked.
   (Thanks Chris Banes)


Version 2.1.0 *(2011-11-30)*
----------------------------

 * Indicators now have a `notifyDataSetChanged` method which should be called
   when changes are made to the adapter.
 * Fix: Avoid `NullPointerException`s when the `ViewPager` is not immediately
   bound to the indicator.


Version 2.0.0 *(2011-11-20)*
----------------------------

 * New `TabPageIndicator`! Uses the Ice Cream Sandwich-style action bar tabs
   which fill the width of the view when there are only a few tabs or provide
   horizontal animated scrolling when there are many.
 * Update to link against ACLv4r4. This will now be required in all implementing
   applications.
 * Allow dragging the title and circle indicators to drag the pager.
 * Remove orientation example as the DirectionalViewPager library has not been
   updated to ACLv4r4.


Version 1.2.1 *(2011-10-20)*
----------------------------

Maven 3 is now required when building from the command line.

 * Update to support ADT 14.


Version 1.2.0 *(2011-10-04)*
----------------------------

 * Move to `com.viewpagerindicator` package.
 * Move maven group and artifact to `com.viewpagerindicator:library`.


Version 1.1.0 *(2011-10-02)*
----------------------------

 * Package changed from `com.jakewharton.android.viewpagerindicator` to
   `com.jakewharton.android.view`.
 * Add vertical orientation support to the circle indicator.
 * Fix: Corrected drawing bug where a single frame would be drawn as if the
   pager had completed its scroll when in fact it was still scrolling.
   (Thanks SimonVT!)


Version 1.0.1 *(2011-09-15)*
----------------------------

 * Fade selected title color to normal text color during the swipe to and from
   the center position.
 * Fix: Ensure both the indicator and footer line are updated when changing the
   footer color via the `setFooterColor` method.


Version 1.0.0 *(2011-08-07)*
----------------------------

Initial release.
