package edu.ucuccs.nutrivision;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

public class ResultActivity extends AppCompatActivity implements AppBarLayout.OnOffsetChangedListener {
    private Toolbar mToolbar;
    private String mTagTitle;
    private ImageView imgResult;
    private TextView nutriText;
    private static final String TAG = ResultActivity.class.getSimpleName();
    private ProgressDialog pDialog;
    private RecyclerView recyFoods;

    private ArrayList<String> mArrayID= new ArrayList<>();
    private ArrayList<String> mArrayName = new ArrayList<>();
    private ArrayList<String> mArrayBrand = new ArrayList<>();
    private ArrayList<String> mArrayQty = new ArrayList<>();
    private ArrayList<String> mArrayUnit = new ArrayList<>();
    private ArrayList<String> mArrayCalories = new ArrayList<>();
    private ArrayList<String> mArrayCaloriesFromFat = new ArrayList<>();
    private ArrayList<String> mArrayFat = new ArrayList<>();
    private ArrayList<String> mArraySatFat = new ArrayList<>();
    private ArrayList<String> mArrayTransFat = new ArrayList<>();
    private ArrayList<String> mArrayCholesterol = new ArrayList<>();
    private ArrayList<String> mArraySodium = new ArrayList<>();
    private ArrayList<String> mArrayCarbs = new ArrayList<>();
    private ArrayList<String> mArrayFiber = new ArrayList<>();
    private ArrayList<String> mArraySugars = new ArrayList<>();
    private ArrayList<String> mArrayProtein = new ArrayList<>();
    private ArrayList<String> mArrayVitA = new ArrayList<>();
    private ArrayList<String> mArrayVitC = new ArrayList<>();
    private ArrayList<String> mArrayCalcium = new ArrayList<>();
    private ArrayList<String> mArrayIron = new ArrayList<>();

    private static final int PERCENTAGE_TO_SHOW_IMAGE = 20;
    private View mFab;
    private int mMaxScrollSize;
    private boolean mIsImageHidden;
    private CollapsingToolbarLayout collapseToolbar;
    private int menuItemID;
    private String restoID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);
        mToolbar        = (Toolbar) findViewById(R.id.result_toolbar);
        imgResult       = (ImageView) findViewById(R.id.img_result);
        recyFoods       = (RecyclerView) findViewById(R.id.recyFoods);
        nutriText       = (TextView) findViewById(R.id.no_nutrifacts_text);
        collapseToolbar = (CollapsingToolbarLayout) findViewById(R.id.flexible_example_collapsing);

        recyFoods.setHasFixedSize(true);


        mTagTitle   = (String) getIntent().getExtras().get("str_tag");
        int type    = getIntent().getExtras().getInt("type");

        if(getIntent().hasExtra("byteArray")) {
            Bitmap bitmap = BitmapFactory.decodeByteArray(
                    getIntent().getByteArrayExtra("byteArray"),0,getIntent().getByteArrayExtra("byteArray").length);
            imgResult.setImageBitmap(bitmap);
        }

        setUpToolbar();
        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_right);
            }
        });

        AppBarLayout appbar = (AppBarLayout) findViewById(R.id.flexible_example_appbar);
        appbar.addOnOffsetChangedListener(this);
        switch (type){
            case 1:
                loadNutriFacts();
                break;
            case 2:
                menuItemID =   getIntent().getExtras().getInt("menu_item_id");
                restoID =   (String) getIntent().getExtras().get("resto_id");
                loadRestoNutriFacts();
                break;

        }
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }

    void setUpToolbar(){
        setSupportActionBar(mToolbar);
        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        collapseToolbar.setTitle(mTagTitle.substring(0,1).toUpperCase() + mTagTitle.substring(1).toLowerCase());
    }
    private void loadNutriFacts() {
        pDialog = new ProgressDialog(this);
        pDialog.setMessage("Loading...");
        pDialog.show();

        RequestQueue requestQueue = Volley.newRequestQueue(this);
        JsonObjectRequest obreq = new JsonObjectRequest(Request.Method.GET,  new Credentials().returnURL(mTagTitle), null, new Response.Listener<JSONObject>() {

            @Override
            public void onResponse(JSONObject response) {
                pDialog.dismiss();
                try {
                    JSONArray arrHits = response.getJSONArray("hits");
                    Log.d(TAG, "onResponse: " + arrHits.length());
                    Log.d(TAG, "response: " + response.toString());
                    clearArray();
                    for (int i = 0; i < arrHits.length(); i++) {

                        JSONObject objFields = arrHits.getJSONObject(i);
                        JSONObject test = objFields.getJSONObject("fields");
                        Log.d(TAG, "objFields: " + objFields.toString());

                        mArrayID.add(test.getString("item_id"));
                        mArrayName.add(test.getString("item_name"));
                        mArrayBrand.add(test.getString("brand_name"));
                        mArrayQty.add(test.getString("nf_serving_size_qty"));
                        mArrayUnit.add(test.getString("nf_serving_size_unit"));
                        mArrayCalories.add(test.getString("nf_calories"));
                        mArrayCaloriesFromFat.add(test.getString("nf_calories_from_fat"));
                        mArrayFat.add(test.getString("nf_total_fat"));
                        mArraySatFat.add(test.getString("nf_saturated_fat"));
                        mArrayTransFat.add(test.getString("nf_trans_fatty_acid"));
                        mArrayCholesterol.add(test.getString("nf_cholesterol"));
                        mArraySodium.add(test.getString("nf_sodium"));
                        mArrayCarbs.add(test.getString("nf_total_carbohydrate"));
                        mArrayFiber.add(test.getString("nf_dietary_fiber"));
                        mArraySugars.add(test.getString("nf_sugars"));
                        mArrayProtein.add(test.getString("nf_protein"));
                        mArrayVitA.add(test.getString("nf_vitamin_a_dv"));
                        mArrayVitC.add(test.getString("nf_vitamin_c_dv"));
                        mArrayCalcium.add(test.getString("nf_calcium_dv"));
                        mArrayIron.add(test.getString("nf_iron_dv"));

                    }
                    FoodsAdapter adapter;
                    if (recyFoods.getAdapter() == null) {
                        adapter = new FoodsAdapter(getApplicationContext(), feedListContent(mArrayID), recyFoods);
                        recyFoods.setAdapter(adapter);
                    } else {
                        adapter = ((FoodsAdapter) recyFoods.getAdapter());
                        adapter.resetData(feedListContent(mArrayID));
                    }
                }
                catch (JSONException e) {
                    e.printStackTrace();
                    nutriText.setVisibility(View.VISIBLE);
                }
            }
        },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        pDialog.hide();
                        nutriText.setVisibility(View.VISIBLE);
                        Log.e("Volley", "Error");
                    }
                }
        );
        requestQueue.add(obreq);
    }

    private void loadRestoNutriFacts() {
        pDialog = new ProgressDialog(this);
        pDialog.setMessage("Loading...");
        pDialog.show();

        RequestQueue requestQueue = Volley.newRequestQueue(this);
        JsonObjectRequest obreq = new JsonObjectRequest(Request.Method.GET,  Credentials.NUTRIVISION.RESTAURANTS_URL + restoID, null, new Response.Listener<JSONObject>() {

            @Override
            public void onResponse(JSONObject response) {
                pDialog.dismiss();
                try {
                    JSONArray arrResto = response.getJSONArray("menu");
                    clearArray();
                    for (int i = 0; i < arrResto.length(); i++) {

                        JSONObject test = arrResto.getJSONObject(i);
                        int tempItemID = Integer.parseInt(test.getString("menu_item_id"));
                        if(menuItemID == tempItemID){
                            mArrayID.add(String.valueOf(tempItemID));
                            mArrayName.add(test.getString("menu_item_name"));
                            mArrayUnit.add(test.getString("n_serving_size"));
                            mArrayCalories.add(test.getString("n_calories"));
                            mArrayFat.add(test.getString("n_fat"));
                            mArrayCholesterol.add(test.getString("n_cholesterol"));
                            mArraySodium.add(test.getString("n_sodium"));
                            mArrayCarbs.add(test.getString("n_carbs"));
                            mArrayProtein.add(test.getString("n_protein"));

                        }
                    }
                    FoodsAdapter adapter;
                    if (recyFoods.getAdapter() == null) {
                        adapter = new FoodsAdapter(getApplicationContext(), feedListContent(mArrayID), recyFoods);
                        recyFoods.setAdapter(adapter);
                    } else {
                        adapter = ((FoodsAdapter) recyFoods.getAdapter());
                        adapter.resetData(feedListContent(mArrayID));
                    }
                }
                catch (JSONException e) {
                    e.printStackTrace();
                    nutriText.setVisibility(View.VISIBLE);
                }
            }
        },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        pDialog.hide();
                        nutriText.setVisibility(View.VISIBLE);
                        Log.e("Volley", "Error");
                    }
                }
        );
        requestQueue.add(obreq);
    }


    private void clearArray() {
        mArrayID.clear();
        mArrayName.clear();
        mArrayBrand.clear();
        mArrayQty.clear();
        mArrayUnit.clear();
        mArrayCalories.clear();
        mArrayCaloriesFromFat.clear();
        mArrayFat.clear();
        mArraySatFat.clear();
        mArrayTransFat.clear();
        mArrayCholesterol.clear();
        mArraySodium.clear();
        mArrayCarbs.clear();
        mArrayFiber.clear();
        mArraySugars.clear();
        mArrayProtein.clear();
        mArrayVitA.clear();
        mArrayVitC.clear();
        mArrayCalcium.clear();
        mArrayIron.clear();
    }
    private class FoodClass implements Serializable {
        String food_id;
        String food_name;
        String food_brand;
        String food_qty;
        String food_unit;
        String food_calories;
        String food_caloriesfromfat;
        String food_fat;
        String food_satfat;
        String food_transfat;
        String food_cholesterol;
        String food_sodium;
        String food_carbs;
        String food_fiber;
        String food_sugars;
        String food_protein;
        String food_vita;
        String food_vitc;
        String food_calcium;
        String food_iron;
    }

    private List<FoodClass> feedListContent(ArrayList mArrayID) {
        List<FoodClass> result = new ArrayList<>();
        for (int i = 0; i < mArrayID.size(); i++) {
            FoodClass ci = new FoodClass();
            ci.food_id = mArrayID.get(i).toString();
            ci.food_name = mArrayName.get(i);
            ci.food_brand = mArrayBrand.size() > 0 ? mArrayBrand.get(i) : "null";
            ci.food_qty = mArrayQty.size() > 0 ? mArrayQty.get(i) : "null";
            ci.food_unit = mArrayUnit.size() > 0 ? mArrayUnit.get(i) : "null";
            ci.food_calories = mArrayCalories.get(i);
            ci.food_caloriesfromfat =  mArrayCaloriesFromFat.size() > 0 ? mArrayCaloriesFromFat.get(i) : "null";
            ci.food_fat = mArrayFat.get(i);
            ci.food_satfat = mArraySatFat.size() > 0 ? mArraySatFat.get(i) : "null";
            ci.food_transfat = mArrayTransFat.size() > 0 ? mArrayTransFat.get(i) : "null";
            ci.food_cholesterol = mArrayCholesterol.get(i);
            ci.food_sodium = mArraySodium.get(i);
            ci.food_carbs = mArrayCarbs.get(i);
            ci.food_fiber = mArrayFiber.size() > 0 ? mArrayFiber.get(i) : "null";
            ci.food_sugars = mArraySugars.size() > 0 ? mArraySugars.get(i) : "null";
            ci.food_protein = mArrayProtein.size() > 0 ? mArrayProtein.get(i) : "null";
            ci.food_vita =  mArrayVitA.size() > 0 ? mArrayVitA.get(i) : "null";
            ci.food_vitc = mArrayVitC.size() > 0 ? mArrayVitC.get(i) : "null";
            ci.food_calcium = mArrayCalcium.size() > 0 ? mArrayCalcium.get(i) : "null";
            ci.food_iron = mArrayIron.size() > 0 ? mArrayIron.get(i) : "null";
            result.add(ci);
        }
        return result;
    }
    private class FoodsAdapter extends RecyclerView.Adapter {
        private final Context applicationContext;
        private final List<FoodClass> foodList;
        private final RecyclerView recyFoods;

        FoodsAdapter(Context applicationContext, List<FoodClass> foodList, RecyclerView recyFoods) {
            this.applicationContext = applicationContext;
            this.foodList = foodList;
            this.recyFoods = recyFoods;
        }
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View rowView = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_nutri_facts_new, parent, false);
            return new FoodListHolder(rowView);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder hold, int position) {
            final FoodClass ci = foodList.get(position);
            FoodListHolder hldr = ((FoodListHolder) hold);
            hldr.lblFoodTitle.setText(ci.food_name);
            hldr.lblQuantity.setText(ci.food_qty);
            hldr.lblServings.setText(ci.food_unit);
            hldr.lblCalories.setText(ci.food_calories);
            hldr.lblCaloriesFromFat.setText(ci.food_caloriesfromfat);
            hldr.lblFat.setText(ci.food_fat);
            hldr.lblSatFat.setText(ci.food_satfat);
            hldr.lblTransFat.setText(ci.food_transfat);
            hldr.lblCholesterol.setText(ci.food_cholesterol);
            hldr.lblSodium.setText(ci.food_sodium);
            hldr.lblCarbs.setText(ci.food_carbs);
            hldr.lblFiber.setText(ci.food_fiber);
            hldr.lblSugars.setText(ci.food_sugars);
            hldr.lblProtein.setText(ci.food_protein);
            hldr.lblVitA.setText(ci.food_vita);
            hldr.lblVitC.setText(ci.food_vitc);
            hldr.lblCalcium.setText(ci.food_calcium);
            hldr.lblIron.setText(ci.food_iron);

            setToZeroIfNull(hldr.lblQuantity, hldr.lblServings, hldr.lblCalories, hldr.lblCaloriesFromFat, hldr.lblFat,
                    hldr.lblSatFat, hldr.lblTransFat, hldr.lblCholesterol, hldr.lblSodium, hldr.lblCarbs, hldr.lblFiber,
                    hldr.lblSugars, hldr.lblProtein, hldr.lblVitA, hldr.lblVitC,  hldr.lblCalcium, hldr.lblIron );
        }

        void setToZeroIfNull(TextView... mTxtViews){
            for (TextView txt: mTxtViews) {
                if (txt.getText().toString().equals("null"))
                    txt.setText("0");
            }
        }

        @Override
        public int getItemCount() {
            return foodList.size();
        }

        class FoodListHolder extends RecyclerView.ViewHolder {
            final TextView lblFoodTitle, lblQuantity, lblServings, lblCalories, lblCaloriesFromFat,
                    lblFat, lblSatFat, lblTransFat, lblCholesterol, lblSodium, lblCarbs, lblFiber,
                    lblSugars, lblProtein, lblVitA, lblVitC, lblCalcium, lblIron;

            FoodListHolder(View itemView) {
                super(itemView);
                lblFoodTitle        = (TextView) itemView.findViewById(R.id.lblFoodTitle);
                lblQuantity         = (TextView) itemView.findViewById(R.id.lblServingQty);
                lblServings         = (TextView) itemView.findViewById(R.id.lblServingUnit);
                lblCalories         = (TextView) itemView.findViewById(R.id.lblCalories);
                lblCaloriesFromFat  = (TextView) itemView.findViewById(R.id.lblCaloriesFromFat);
                lblFat              = (TextView) itemView.findViewById(R.id.lblTotalFat);
                lblSatFat           = (TextView) itemView.findViewById(R.id.lblSatFat);
                lblTransFat         = (TextView) itemView.findViewById(R.id.lblTransFat);
                lblCholesterol      = (TextView) itemView.findViewById(R.id.lblCholesterol);
                lblSodium           = (TextView) itemView.findViewById(R.id.lblSodium);
                lblCarbs            = (TextView) itemView.findViewById(R.id.lblCarbs);
                lblFiber            = (TextView) itemView.findViewById(R.id.lblFiber);
                lblSugars           = (TextView) itemView.findViewById(R.id.lblSugar);
                lblProtein          = (TextView) itemView.findViewById(R.id.lblProtein);
                lblVitA             = (TextView) itemView.findViewById(R.id.lblVitA);
                lblVitC             = (TextView) itemView.findViewById(R.id.lblVitC);
                lblCalcium          = (TextView) itemView.findViewById(R.id.lblCalcium);
                lblIron             = (TextView) itemView.findViewById(R.id.lblIron);
            }
        }

        void resetData(List<FoodClass> listSong) {
            this.foodList.clear();
            this.foodList.addAll(listSong);
            notifyDataSetChanged();
        }
    }

    @Override
    public void onOffsetChanged(AppBarLayout appBarLayout, int i) {
        if (mMaxScrollSize == 0)
            mMaxScrollSize = appBarLayout.getTotalScrollRange();

        int currentScrollPercentage = (Math.abs(i)) * 100 / mMaxScrollSize;

        if (currentScrollPercentage >= PERCENTAGE_TO_SHOW_IMAGE) {
            if (!mIsImageHidden) {
                mIsImageHidden = true;

                ViewCompat.animate(mFab).scaleY(0).scaleX(0).start();
            }
        }

        if (currentScrollPercentage < PERCENTAGE_TO_SHOW_IMAGE) {
            if (mIsImageHidden) {
                mIsImageHidden = false;
                ViewCompat.animate(mFab).scaleY(1).scaleX(1).start();
            }
        }
    }
}
