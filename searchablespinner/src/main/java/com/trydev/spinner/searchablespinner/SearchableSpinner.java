package com.trydev.spinner.searchablespinner;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.AttrRes;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.Px;
import androidx.annotation.RequiresApi;
import androidx.annotation.StyleRes;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

import com.trydev.spinner.searchablespinner.interfaces.ISpinnerSelectedView;
import com.trydev.spinner.searchablespinner.interfaces.IStatusListener;
import com.trydev.spinner.searchablespinner.interfaces.OnItemSelectedListener;
import com.trydev.spinner.searchablespinner.tools.EditCursorColor;



public class SearchableSpinner extends RelativeLayout implements View.OnClickListener {

    private static final int DefaultElevation = 16;
    private static final int DefaultAnimationDuration = 400;

    private ViewState mViewState = ViewState.ShowingRevealedLayout;
    private IStatusListener mStatusListener;
    private CardView mRevealContainerCardView;
    private LinearLayout mRevealItem;
    private ImageView mStartSearchImageView;
    private EditText mSearchEditText;
    private CardView mSpinnerListContainer;
    private CardView cardViewSearch;
    private PopupWindow mPopupWindow;
    private ListView mSpinnerListView;
    private TextView mEmptyTextView;
    private Context mContext;
    private OnItemSelectedListener mOnItemSelected;
    private SpinnerSelectedView mCurrSelectedView;
    private int mScreenHeightPixels;
    private int mScreenWidthPixels;
    /* Attributes */
    private @ColorInt
    int mainDropDownListBackgroundColor;

    private @ColorInt
    int mDropDownMainTextColor;
    private @ColorInt
    int mSearchViewHintColor;
    private @ColorInt
    int mEditViewBackgroundColor;

    private @ColorInt
    int mListItemColor;
    private @ColorInt
    int mEditViewTextColor;
    private Drawable mListItemDivider;
    private @Px
    int mDropDownViewCornerRadius;

    private @Px
    int mSearchCornerRadius;

    private @Px
    int mSearchCardElevation;
    private @Px
    int mExpandSize;
    private @Px
    int mListDividerSize;
    private boolean mKeepLastSearch;
    private String mainEntryText;
    private String mSearchHintText;
    private String mNoItemsFoundText;
    private int mAnimDuration;
    private AdapterView.OnItemClickListener mOnItemSelectedListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            if (mCurrSelectedView == null) {
                Adapter adapter = parent.getAdapter();
                if (adapter instanceof ISpinnerSelectedView) {
                    View selectedView = ((ISpinnerSelectedView) adapter).getSelectedView(position);
                    mCurrSelectedView = new SpinnerSelectedView(selectedView, position, selectedView.getId());
                } else {
                    mCurrSelectedView = new SpinnerSelectedView(view, position, id);
                }
                mSpinnerListView.setSelection(position);
            } else {
                Adapter adapter = parent.getAdapter();
                if (adapter instanceof ISpinnerSelectedView) {
                    View selectedView = ((ISpinnerSelectedView) adapter).getSelectedView(position);
                    mCurrSelectedView = new SpinnerSelectedView(selectedView, position, selectedView.getId());
                } else {
                    mCurrSelectedView.setView(view);
                    mCurrSelectedView.setPosition(position);
                    mCurrSelectedView.setId(id);
                }
                mSpinnerListView.setSelection(position);
            }
            if (mCurrSelectedView == null) {
                if (mOnItemSelected != null)
                    mOnItemSelected.onNothingSelected();
            } else if (mCurrSelectedView != null) {
                mRevealItem.removeAllViews();
                mSpinnerListView.removeViewInLayout(mCurrSelectedView.getView());
                mRevealItem.addView(mCurrSelectedView.getView());
                ((BaseAdapter) mSpinnerListView.getAdapter()).notifyDataSetChanged();
                if (mOnItemSelected != null)
                    mOnItemSelected.onItemSelected(mCurrSelectedView.getView(), mCurrSelectedView.getPosition(), mCurrSelectedView.getId());
            }
            hideEdit();
        }
    };
    private TextWatcher mTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            Filterable filterable = (Filterable) mSpinnerListView.getAdapter();
            if (filterable != null)
                filterable.getFilter().filter(s.toString().toLowerCase());
        }

        @Override
        public void afterTextChanged(Editable s) {

        }
    };

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public SearchableSpinner(@NonNull Context context) {
        this(context, null);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public SearchableSpinner(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, -1);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public SearchableSpinner(@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public SearchableSpinner(@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr, @StyleRes int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        mContext = context;
        getAttributeSet(attrs, defStyleAttr, defStyleRes);

        final LayoutInflater factory = LayoutInflater.from(context);
        factory.inflate(R.layout.view_searchable_spinner, this, true);

        mSpinnerListContainer = (CardView) factory.inflate(R.layout.view_list, this, false);
        mSpinnerListView = mSpinnerListContainer.findViewById(R.id.LstVw_SpinnerListView);
        mSearchEditText = mSpinnerListContainer.findViewById(R.id.EdtTxt_SearchEditText);
        cardViewSearch = mSpinnerListContainer.findViewById(R.id.cardViewSearch);

        mSpinnerListContainer.setCardBackgroundColor(mListItemColor);

        cardViewSearch.setRadius(mSearchCornerRadius);
        cardViewSearch.setCardElevation(mSearchCardElevation);

        if (mListItemDivider != null) {
            mSpinnerListView.setDivider(mListItemDivider);
            mSpinnerListView.setDividerHeight(mListDividerSize);
        }

        mSearchEditText.setImeOptions(EditorInfo.IME_ACTION_DONE | EditorInfo.IME_FLAG_NO_EXTRACT_UI);
        mSearchEditText.addTextChangedListener(mTextWatcher);

        mEmptyTextView = mSpinnerListContainer.findViewById(R.id.TxtVw_EmptyText);
        mSpinnerListView.setEmptyView(mEmptyTextView);
    }

    private void getAttributeSet(@Nullable AttributeSet attrs, @AttrRes int defStyleAttr, @StyleRes int defStyleRes) {
        if (attrs != null) {
            try {
                TypedArray attributes = mContext.getTheme().obtainStyledAttributes(attrs, R.styleable.SearchableSpinner, defStyleAttr, defStyleRes);
                mainDropDownListBackgroundColor = attributes.getColor(R.styleable.SearchableSpinner_MainDropDownListBackgroundColor, Color.WHITE);
                mDropDownViewCornerRadius = attributes.getDimensionPixelSize(R.styleable.SearchableSpinner_DropDownViewCornerRadius, 4);
                mDropDownMainTextColor = attributes.getColor(R.styleable.SearchableSpinner_DropDownMainTextColor, Color.WHITE);
                mSearchViewHintColor = attributes.getColor(R.styleable.SearchableSpinner_SearchViewHintColor, Color.WHITE);
                mEditViewBackgroundColor = attributes.getColor(R.styleable.SearchableSpinner_SearchViewBackgroundColor, Color.GRAY);
                mListItemColor = attributes.getColor(R.styleable.SearchableSpinner_ListItemColor, Color.WHITE);
                mEditViewTextColor = attributes.getColor(R.styleable.SearchableSpinner_SearchViewTextColor, Color.BLACK);
                mExpandSize = attributes.getDimensionPixelSize(R.styleable.SearchableSpinner_SpinnerExpandHeight, 0);
                mAnimDuration = attributes.getColor(R.styleable.SearchableSpinner_AnimDuration, DefaultAnimationDuration);
                mKeepLastSearch = attributes.getBoolean(R.styleable.SearchableSpinner_KeepLastSearch, false);
                mainEntryText = attributes.getString(R.styleable.SearchableSpinner_MainEntryText);
                mSearchHintText = attributes.getString(R.styleable.SearchableSpinner_SearchHintText);
                mNoItemsFoundText = attributes.getString(R.styleable.SearchableSpinner_NoItemsFoundText);
                mListItemDivider = attributes.getDrawable(R.styleable.SearchableSpinner_ItemsDivider);
                mListDividerSize = attributes.getDimensionPixelSize(R.styleable.SearchableSpinner_DividerHeight, 0);
                mSearchCornerRadius = attributes.getDimensionPixelSize(R.styleable.SearchableSpinner_SearchCornerRadius, 20);
                mSearchCardElevation = attributes.getDimensionPixelSize(R.styleable.SearchableSpinner_SearchViewElevation, 10);
            } catch (UnsupportedOperationException e) {
                Log.e("SearchableSpinner", "getAttributeSet --> " + e.getLocalizedMessage());
            }
        }
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mRevealContainerCardView = findViewById(R.id.CrdVw_RevealContainer);
        mRevealContainerCardView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mViewState == ViewState.ShowingRevealedLayout) {
                    revealEditView();
                } else if (mViewState == ViewState.ShowingEditLayout) {
                    hideEditView();
                }
            }
        });
        mRevealItem = findViewById(R.id.FrmLt_SelectedItem);
        mStartSearchImageView = findViewById(R.id.ImgVw_StartSearch);
        init();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        getScreenSize();
        if (mExpandSize <= 0) {
            mPopupWindow.setHeight(WindowManager.LayoutParams.WRAP_CONTENT);
        } else {
            mPopupWindow.setHeight(heightMeasureSpec);
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {

        super.onLayout(changed, l, t, r, b);
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        requestLayout();
        super.onScrollChanged(l, t, oldl, oldt);
    }

    private void init() {
        setupColors();
        setupList();

        mStartSearchImageView.setOnClickListener(this);
        mPopupWindow = new PopupWindow(mContext);
        mPopupWindow.setContentView(mSpinnerListContainer);
        mPopupWindow.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
        mPopupWindow.setInputMethodMode(PopupWindow.INPUT_METHOD_NEEDED);
        mPopupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                hideEdit();
            }
        });
        mPopupWindow.setFocusable(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mPopupWindow.setElevation(DefaultElevation);
        }
        mPopupWindow.setBackgroundDrawable(ContextCompat.getDrawable(mContext, R.drawable.spinner_drawable));

        mSpinnerListView.setOnItemClickListener(mOnItemSelectedListener);
        if (mCurrSelectedView == null) {
            if (!TextUtils.isEmpty(mSearchHintText)) {
                mSearchEditText.setHint(mSearchHintText);
            }
            if (mCurrSelectedView == null && !TextUtils.isEmpty(mainEntryText)) {
                TextView textView = new TextView(mContext);
                textView.setText(mainEntryText);
                textView.setTextColor(mDropDownMainTextColor);
                mCurrSelectedView = new SpinnerSelectedView(textView, -1, 0);
                mRevealItem.addView(textView);
            }
        } else {
            mSpinnerListView.performItemClick(mCurrSelectedView.getView(), mCurrSelectedView.getPosition(), mCurrSelectedView.getId());
        }
        clearAnimation();
//        clearFocus();
    }

    public Object getSelectedItem() {
        if (mCurrSelectedView != null) {
            int position = mCurrSelectedView.getPosition();
            Adapter adapter = mSpinnerListView.getAdapter();
            if (adapter != null && adapter.getCount() > 0 && position >= 0) {
                return adapter.getItem(position);
            } else {
                return null;
            }
        }
        return null;
    }

    public void setSelectedItem(int position) {
        Adapter adapter = mSpinnerListView.getAdapter();
        if (adapter instanceof ISpinnerSelectedView) {
            View selectedView = ((ISpinnerSelectedView) adapter).getSelectedView(position);
            mCurrSelectedView = new SpinnerSelectedView(selectedView, position, selectedView.getId());
            mSpinnerListView.setSelection(position);
        } else {
            TextView textView = new TextView(mContext);
            textView.setText(mainEntryText);
            textView.setTextColor(mDropDownMainTextColor);
            mCurrSelectedView = new SpinnerSelectedView(textView, -1, 0);
            mRevealItem.addView(textView);
        }
        if (mCurrSelectedView == null) {
            if (mOnItemSelected != null)
                mOnItemSelected.onNothingSelected();
        } else if (mCurrSelectedView != null) {
            mRevealItem.removeAllViews();
            mSpinnerListView.removeViewInLayout(mCurrSelectedView.getView());
            mRevealItem.addView(mCurrSelectedView.getView());
            ((BaseAdapter) mSpinnerListView.getAdapter()).notifyDataSetChanged();
            if (mOnItemSelected != null)
                mOnItemSelected.onItemSelected(mCurrSelectedView.getView(), mCurrSelectedView.getPosition(), mCurrSelectedView.getId());
        }
        hideEdit();
    }

    public void setSelectedItem(Object item) {
        int itemPosition = getItemPosition(item);
        if (itemPosition >= 0) {
            setSelectedItem(itemPosition);
        }
    }

    public int getItemPosition(Object item) {
        if (item == null)
            return -1;
        Adapter adapter = mSpinnerListView.getAdapter();
        if (adapter != null) {
            for (int i = 0; i < adapter.getCount(); i++) {
                Object adpItem = adapter.getItem(i);
                if (adpItem != null && adpItem.equals(item)) {
                    return i;
                }
            }
        }
        return -1;
    }

    public int getSelectedPosition() {
        if (mCurrSelectedView != null) {
            return mCurrSelectedView.getPosition();
        }
        return -1;
    }

    private void setupColors() {
        mRevealContainerCardView.setCardBackgroundColor(mainDropDownListBackgroundColor);
        mRevealContainerCardView.setRadius(mDropDownViewCornerRadius);

        mSearchEditText.setBackgroundColor(mEditViewBackgroundColor);
        mSearchEditText.setTextColor(mEditViewTextColor);
        mSearchEditText.setHintTextColor(mSearchViewHintColor);
        EditCursorColor.setCursorColor(mSearchEditText, mEditViewTextColor);
    }

    private void setupList() {
        MarginLayoutParams spinnerListViewLayoutParams = (MarginLayoutParams) mSpinnerListView.getLayoutParams();
        ViewGroup.LayoutParams spinnerListContainerLayoutParams = mSpinnerListContainer.getLayoutParams();
        LinearLayout.LayoutParams listLayoutParams = (LinearLayout.LayoutParams) mSpinnerListView.getLayoutParams();

        spinnerListContainerLayoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;

        if (mExpandSize <= 0) {
            listLayoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        } else {
            listLayoutParams.height = mExpandSize;
        }
        spinnerListViewLayoutParams.setMargins(0, 0, 0, 0);
    }

    public void setAdapter(ListAdapter adapter) {
        if (!(adapter instanceof Filterable))
            throw new IllegalArgumentException("Adapter should implement the Filterable interface");
        mSpinnerListView.setAdapter(adapter);
    }

    public void setOnItemSelectedListener(OnItemSelectedListener onItemSelectedListener) {
        mOnItemSelected = onItemSelectedListener;
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        hideEdit();
        getScreenSize();
    }

    private void getScreenSize() {
        DisplayMetrics metrics = new DisplayMetrics();
        WindowManager wm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        wm.getDefaultDisplay().getMetrics(metrics);
        mScreenHeightPixels = metrics.heightPixels;
        mScreenWidthPixels = metrics.widthPixels;
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.ImgVw_StartSearch) {
            revealEdit();
        }
    }

    public void revealEdit() {
        if (mViewState == ViewState.ShowingRevealedLayout) {
            if (!mKeepLastSearch)
                mSearchEditText.setText(null);
            revealEditView();
        } else {
            hideEditView();
        }
    }

    public void hideEdit() {
        if (mViewState == ViewState.ShowingEditLayout) {
            hideEditView();
        }
    }

    private void revealEditView() {
        mViewState = ViewState.ShowingEditLayout;
        if (!mPopupWindow.isShowing())
            mPopupWindow.showAsDropDown(this, 0, 0);

        mSearchEditText.setVisibility(View.VISIBLE);
        mViewState = ViewState.ShowingEditLayout;
        mSpinnerListContainer.setVisibility(View.VISIBLE);
    }

    private void hideEditView() {
        mViewState = ViewState.ShowingAnimation;
        if (mStatusListener != null)
            mStatusListener.spinnerIsClosing();

        mViewState = ViewState.ShowingRevealedLayout;
        mRevealContainerCardView.setVisibility(View.VISIBLE);
        mSearchEditText.setVisibility(View.INVISIBLE);
        ((InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(mSearchEditText.getWindowToken(), 0);

        if (mPopupWindow.isShowing()) {

            mSpinnerListContainer.setVisibility(View.GONE);
            mPopupWindow.dismiss();
        }
    }

    public boolean isInsideSearchEditText(MotionEvent event) {
        Rect editTextRect = new Rect();
        mSearchEditText.getHitRect(editTextRect);
        return editTextRect.contains((int) event.getX(), (int) event.getY());
    }

    public void setStatusListener(IStatusListener statusListener) {
        mStatusListener = statusListener;
    }

    @Override
    public Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        SavedState ss = new SavedState(superState);
        ss.mViewState = ViewState.ShowingRevealedLayout;
        ss.mAnimDuration = mAnimDuration;

        ss.mExpandSize = mExpandSize;

        ss.mainDropDownListBackgroundColor = mainDropDownListBackgroundColor;
        ss.mSearchViewHintColor = mSearchViewHintColor;
        ss.mEditViewBackgroundColor = mEditViewBackgroundColor;
        ss.mEditViewTextColor = mEditViewTextColor;
        ss.mKeepLastSearch = mKeepLastSearch;
        ss.mainEntryText = mainEntryText;
        ss.mSearchHintText = mSearchHintText;
        ss.mNoItemsFoundText = mNoItemsFoundText;
        ss.mSelectedViewPosition = mCurrSelectedView != null ? mCurrSelectedView.getPosition() : -1;
        return ss;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        super.onRestoreInstanceState(state);
        SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());
        mViewState = ss.mViewState;
        mAnimDuration = ss.mAnimDuration;
        mExpandSize = ss.mExpandSize;
        mainDropDownListBackgroundColor = ss.mainDropDownListBackgroundColor;
        mSearchViewHintColor = ss.mSearchViewHintColor;
        mEditViewBackgroundColor = ss.mEditViewBackgroundColor;
        mEditViewTextColor = ss.mEditViewTextColor;
        mKeepLastSearch = ss.mKeepLastSearch;
        mainEntryText = ss.mainEntryText;
        mSearchHintText = ss.mSearchHintText;
        mNoItemsFoundText = ss.mNoItemsFoundText;
        int mSelectedViewPosition = ss.mSelectedViewPosition;

        if (mSelectedViewPosition >= 0) {
            View v = mSpinnerListView.getAdapter().getView(mSelectedViewPosition, null, null);
            mSpinnerListView.performItemClick(v, mSelectedViewPosition, v.getId());
        }
    }

    public enum ViewState {
        ShowingRevealedLayout,
        ShowingEditLayout,
        ShowingAnimation
    }

    static class SavedState extends BaseSavedState {
        public static final Creator<SavedState> CREATOR = new Creator<SavedState>() {
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
        ViewState mViewState;
        int mAnimDuration;
        @Px
        int mExpandSize;
        @ColorInt
        int mainDropDownListBackgroundColor;
        @ColorInt
        int mSearchViewHintColor;
        @ColorInt
        int mEditViewBackgroundColor;
        @ColorInt
        int mEditViewTextColor;
        boolean mKeepLastSearch;
        String mainEntryText;
        String mSearchHintText;
        String mNoItemsFoundText;
        int mSelectedViewPosition;

        SavedState(Parcelable superState) {
            super(superState);
        }

        private SavedState(Parcel in) {
            super(in);
            mViewState = ViewState.values()[in.readInt()];
            mAnimDuration = in.readInt();
            mExpandSize = in.readInt();
            mainDropDownListBackgroundColor = in.readInt();
            mSearchViewHintColor = in.readInt();
            mEditViewBackgroundColor = in.readInt();
            mEditViewTextColor = in.readInt();
            mKeepLastSearch = in.readInt() > 0;
            mainEntryText = in.readString();
            mSearchHintText = in.readString();
            mNoItemsFoundText = in.readString();
            mSelectedViewPosition = in.readInt();
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeInt(mViewState.ordinal());
            out.writeInt(mAnimDuration);
            out.writeInt(mExpandSize);
            out.writeInt(mainDropDownListBackgroundColor);
            out.writeInt(mSearchViewHintColor);
            out.writeInt(mEditViewBackgroundColor);
            out.writeInt(mEditViewTextColor);
            out.writeInt(mKeepLastSearch ? 1 : 0);
            out.writeString(mainEntryText);
            out.writeString(mSearchHintText);
            out.writeString(mNoItemsFoundText);
            out.writeInt(mSelectedViewPosition);
        }
    }
}
