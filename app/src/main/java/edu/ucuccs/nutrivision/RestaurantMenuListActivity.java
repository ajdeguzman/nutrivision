package edu.ucuccs.nutrivision;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.support.design.widget.AppBarLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import edu.ucuccs.nutrivision.custom.CircleImageTransform;
import edu.ucuccs.nutrivision.custom.CustomRequest;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

public class RestaurantMenuListActivity extends AppCompatActivity
        implements AppBarLayout.OnOffsetChangedListener  {

    private static final float PERCENTAGE_TO_SHOW_TITLE_AT_TOOLBAR  = 0.9f;
    private static final float PERCENTAGE_TO_HIDE_TITLE_DETAILS     = 0.3f;
    private static final int ALPHA_ANIMATIONS_DURATION              = 200;

    private boolean mIsTheTitleVisible          = false;
    private boolean mIsTheTitleContainerVisible = true;

    private LinearLayout mTitleContainer;
    private TextView mTitle, mLblRestoName;
    private AppBarLayout mAppBarLayout;
    private ImageView imgRestoLogo;
    private RecyclerView recyMenus;
    private ProgressDialog pDialog;
    NetworkConnectivity mNetConn = new NetworkConnectivity(RestaurantMenuListActivity.this);
    private static final String TAG = RestaurantMenuListActivity.class.getSimpleName();
    private String mRestoID;

    private ArrayList<String> mArrMenuItemID = new ArrayList<>();
    private ArrayList<String> mArrMenuItemName = new ArrayList<>();
    private ArrayList<String> mArrMenuItemServing = new ArrayList<>();
    private ArrayList<String> mArrMenuItemCalories = new ArrayList<>();
    private ArrayList<String> mArrMenuItemFat = new ArrayList<>();
    private ArrayList<String> mArrMenuItemCholesterol = new ArrayList<>();
    private ArrayList<String> mArrMenuItemSodium = new ArrayList<>();
    private ArrayList<String> mArrMenuItemCarbs = new ArrayList<>();
    private ArrayList<String> mArrMenuItemProtein = new ArrayList<>();
    private MenuItemAdapter adapter = new MenuItemAdapter(RestaurantMenuListActivity.this, feedListContent(mArrMenuItemID), recyMenus);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_restaurant_menu_list);
        bindActivity();
        mAppBarLayout.addOnOffsetChangedListener(this);
        startAlphaAnimation(mTitle, 0, View.INVISIBLE);

        Bundle extras;
        extras = getIntent().getExtras();

        if (extras != null) {
            String mRestoName   = extras.getString("resto_name");
            String mRestoLogo   = extras.getString("resto_logo");
            mRestoID            = extras.getString("resto_id");

            Glide.with(this)
                    .load(mRestoLogo)
                    .transform(new CircleImageTransform(this))
                    .into(imgRestoLogo);
            mTitle.setText(mRestoName);
            mLblRestoName.setText(mRestoName);

        }
        recyMenus.setHasFixedSize(true);
        recyMenus.setAdapter(adapter);

        if(mNetConn.isConnectedToInternet()){
            loadRestaurantMenu();
        }else{
            Toast.makeText(getApplicationContext(), "No connection", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }

    private void loadRestaurantMenu() {
        pDialog = new ProgressDialog(this);
        pDialog.setMessage("Loading Menus...");
        pDialog.setCancelable(false);
        pDialog.show();

        RequestQueue requestQueue = Volley.newRequestQueue(this);
        CustomRequest obreq = new CustomRequest (Request.Method.GET,  Credentials.NUTRIVISION.RESTAURANTS_URL + mRestoID, null,
            new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                pDialog.dismiss();
                try {
                    JSONArray arrResto = response.getJSONArray("menu");
                    Log.d(TAG, "onResponse: " + arrResto);
                    clearArray();
                    for (int i = 0; i < arrResto.length(); i++) {

                        JSONObject objFields = arrResto.getJSONObject(i);
                        mArrMenuItemID.add(objFields.getString("menu_item_id"));
                        mArrMenuItemName.add(objFields.getString("menu_item_name"));
                        mArrMenuItemServing.add(objFields.getString("n_serving_size"));
                        mArrMenuItemCalories.add(objFields.getString("n_calories"));
                        mArrMenuItemFat.add(objFields.getString("n_fat"));
                        mArrMenuItemCholesterol.add(objFields.getString("n_cholesterol"));
                        mArrMenuItemSodium.add(objFields.getString("n_sodium"));
                        mArrMenuItemCarbs.add(objFields.getString("n_carbs"));
                        mArrMenuItemProtein.add(objFields.getString("n_protein"));

                    }
                    if (recyMenus.getAdapter() == null) {
                        adapter = new MenuItemAdapter(getApplicationContext(), feedListContent(mArrMenuItemID), recyMenus);
                        recyMenus.setAdapter(adapter);
                    } else {
                        adapter = ((MenuItemAdapter) recyMenus.getAdapter());
                        adapter.resetData(feedListContent(mArrMenuItemID));
                    }
                }
                catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        },
        new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        });
        requestQueue.add(obreq);
    }
    private List<MenuClass> feedListContent(ArrayList mArrayRestoID) {
        List<MenuClass> result = new ArrayList<>();
        for (int i = 0; i < mArrayRestoID.size(); i++) {
            MenuClass ci = new MenuClass();
            ci.menu_item_id = mArrMenuItemID.get(i);
            ci.menu_item_name = mArrMenuItemName.get(i);
            ci.n_serving_size = mArrMenuItemServing.get(i);
            ci.n_calories = mArrMenuItemCalories.get(i);
            ci.n_fat = mArrMenuItemFat.get(i);
            ci.n_cholesterol = mArrMenuItemCholesterol.get(i);
            ci.n_sodium = mArrMenuItemSodium.get(i);
            ci.n_carbs = mArrMenuItemCarbs.get(i);
            ci.n_protein = mArrMenuItemProtein.get(i);
            result.add(ci);
        }
        return result;
    }
    @Override
    public void onBackPressed(){
        super.onBackPressed();
        finish();
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_right);
    }
    private void clearArray() {
        mArrMenuItemName.clear();
        mArrMenuItemServing.clear();
        mArrMenuItemCalories.clear();
        mArrMenuItemFat.clear();
        mArrMenuItemCholesterol.clear();
        mArrMenuItemSodium.clear();
        mArrMenuItemCarbs.clear();
        mArrMenuItemProtein.clear();
    }
    private class MenuClass implements Serializable {
        String menu_item_id;
        String menu_item_name;
        String n_serving_size;
        String n_calories;
        String n_fat;
        String n_cholesterol;
        String n_sodium;
        String n_carbs;
        String n_protein;
    }

    private class MenuItemAdapter extends RecyclerView.Adapter {
        private final Context mContext;
        private final List<MenuClass> menuList;
        private final RecyclerView mRecyResto;

        MenuItemAdapter(Context applicationContext, List<MenuClass> menuList, RecyclerView mRecyMenu) {
            this.mContext = applicationContext;
            this.menuList = menuList;
            this.mRecyResto = mRecyMenu;
        }
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View rowView = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_1, parent, false);
            return new MenuListHolder(rowView);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder hold, int position) {
            final MenuClass ci = menuList.get(position);
            MenuListHolder hldr = ((MenuListHolder) hold);
            hldr.lblItemName.setText(ci.menu_item_name);
            hldr.lblItemName.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent i = new Intent(getApplicationContext(), ResultActivity.class);
                    i.putExtra("str_tag", ci.menu_item_name);
                    i.putExtra("type", 2);
                    i.putExtra("menu_item_id", Integer.parseInt(ci.menu_item_id));
                    i.putExtra("resto_id", mRestoID);
                    i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(i);
                    overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_left);
                }
            });

        }

        @Override
        public int getItemCount() {
            return menuList.size();
        }

        class MenuListHolder extends RecyclerView.ViewHolder {
            final TextView lblItemName;

            MenuListHolder(View itemView) {
                super(itemView);
                lblItemName        = (TextView) itemView.findViewById(android.R.id.text1);
            }
        }

        void resetData(List<MenuClass> listResto) {
            this.menuList.clear();
            this.menuList.addAll(listResto);
            notifyDataSetChanged();
        }
    }
    private void bindActivity() {
        mTitle          = (TextView) findViewById(R.id.main_textview_title);
        mLblRestoName   = (TextView) findViewById(R.id.lbl_resto_title);
        mTitleContainer = (LinearLayout) findViewById(R.id.main_linearlayout_title);
        mAppBarLayout   = (AppBarLayout) findViewById(R.id.main_appbar);
        imgRestoLogo    = (ImageView) findViewById(R.id.img_resto_logo);
        recyMenus       = (RecyclerView) findViewById(R.id.recyMenus);
    }

    @Override
    public void onOffsetChanged(AppBarLayout appBarLayout, int offset) {
        int maxScroll = appBarLayout.getTotalScrollRange();
        float percentage = (float) Math.abs(offset) / (float) maxScroll;

        handleAlphaOnTitle(percentage);
        handleToolbarTitleVisibility(percentage);
    }

    private void handleToolbarTitleVisibility(float percentage) {
        if (percentage >= PERCENTAGE_TO_SHOW_TITLE_AT_TOOLBAR) {

            if(!mIsTheTitleVisible) {
                startAlphaAnimation(mTitle, ALPHA_ANIMATIONS_DURATION, View.VISIBLE);
                mIsTheTitleVisible = true;
            }

        } else {

            if (mIsTheTitleVisible) {
                startAlphaAnimation(mTitle, ALPHA_ANIMATIONS_DURATION, View.INVISIBLE);
                mIsTheTitleVisible = false;
            }
        }
    }

    private void handleAlphaOnTitle(float percentage) {
        if (percentage >= PERCENTAGE_TO_HIDE_TITLE_DETAILS) {
            if(mIsTheTitleContainerVisible) {
                startAlphaAnimation(mTitleContainer, ALPHA_ANIMATIONS_DURATION, View.INVISIBLE);
                mIsTheTitleContainerVisible = false;
            }

        } else {

            if (!mIsTheTitleContainerVisible) {
                startAlphaAnimation(mTitleContainer, ALPHA_ANIMATIONS_DURATION, View.VISIBLE);
                mIsTheTitleContainerVisible = true;
            }
        }
    }

    public static void startAlphaAnimation (View v, long duration, int visibility) {
        AlphaAnimation alphaAnimation = (visibility == View.VISIBLE)
                ? new AlphaAnimation(0f, 1f)
                : new AlphaAnimation(1f, 0f);

        alphaAnimation.setDuration(duration);
        alphaAnimation.setFillAfter(true);
        v.startAnimation(alphaAnimation);
    }
}
