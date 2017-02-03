package edu.ucuccs.nutrivision;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.NetworkResponse;
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

public class RestaurantActivity extends AppCompatActivity {
    private Toolbar mToolbar;
    private ArrayList<String> mRestoID = new ArrayList<>();
    private ArrayList<String> mRestoName = new ArrayList<>();
    private ArrayList<String> mRestoLogo = new ArrayList<>();
    private RecyclerView mRecyResto;
    private static final String TAG = RestaurantActivity.class.getSimpleName();
    private ProgressDialog pDialog;
    NetworkConnectivity mNetConn = new NetworkConnectivity(RestaurantActivity.this);
    private ImageView imgEmptyState;
    private TextView mLblEmptyState;
    private RestoAdapter adapter = new RestoAdapter(RestaurantActivity.this, feedListContent(mRestoID), mRecyResto);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_restaurant);
        LinearLayout mLinearEmpty   = (LinearLayout) findViewById(R.id.layout_empty_state);
        mToolbar                    = (Toolbar) findViewById(R.id.toolbar);
        mRecyResto                  = (RecyclerView) findViewById(R.id.recyResto);
        mLblEmptyState              = (TextView) findViewById(R.id.lbl_empty_state);
        imgEmptyState               = (ImageView) findViewById(R.id.img_empty_state);
        setUpToolbar();
        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_right);
            }
        });
        mRecyResto.setHasFixedSize(true);
        mRecyResto.setLayoutManager(new GridLayoutManager(this, 2));
        mRecyResto.setAdapter(adapter);

        if(mNetConn.isConnectedToInternet()){
            mLinearEmpty.setVisibility(View.GONE);
            loadRestaurants();
        }else{
            showNoConnectionState();
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
    }
    void showNoConnectionState() {
        imgEmptyState.setImageResource(R.drawable.empty_state_onion_connection);
        mLblEmptyState.setText(R.string.msg_no_connection);
    }
    private void loadRestaurants() {
        pDialog = new ProgressDialog(this);
        pDialog.setMessage("Loading Restaurants...");
        pDialog.setCancelable(false);
        pDialog.show();

        RequestQueue requestQueue = Volley.newRequestQueue(this);
        CustomRequest obreq = new CustomRequest (Request.Method.GET,  Credentials.NUTRIVISION.RESTAURANTS_URL, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                pDialog.dismiss();
                try {
                    JSONArray arrResto = response.getJSONArray("resto");
                    Log.d(TAG, "onResponse: " + arrResto);
                    clearArray();
                    for (int i = 0; i < arrResto.length(); i++) {

                        JSONObject objFields = arrResto.getJSONObject(i);
                        mRestoID.add(objFields.getString("resto_id"));
                        mRestoName.add(objFields.getString("resto_name"));
                        mRestoLogo.add(objFields.getString("resto_logo"));

                    }
                    if (mRecyResto.getAdapter() == null) {
                        adapter = new RestoAdapter(getApplicationContext(), feedListContent(mRestoID), mRecyResto);
                        mRecyResto.setAdapter(adapter);
                    } else {
                        adapter = ((RestoAdapter) mRecyResto.getAdapter());
                        adapter.resetData(feedListContent(mRestoID));
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
                String json;
                NetworkResponse response = error.networkResponse;
                if(response != null && response.data != null){
                    switch(response.statusCode){
                        case 400:
                            json = new String(response.data);
                            json = trimMessage(json, "message");
                            if(json != null) displayMessage(json);
                            break;
                        case 502:
                            json = new String(response.data);
                            json = trimMessage(json, "message");
                            if(json != null) displayMessage(json);
                            break;

                    }
                }
            }
        }
        );
        requestQueue.add(obreq);
    }
    public String trimMessage(String json, String key){
        String trimmedString;
        try{
            JSONObject obj = new JSONObject(json);
            trimmedString = obj.getString(key);
        } catch(JSONException e){
            e.printStackTrace();
            return null;
        }

        return trimmedString;
    }

    public void displayMessage(String toastString){
        Toast.makeText(getApplicationContext(), toastString, Toast.LENGTH_LONG).show();
    }

    private void clearArray() {
        mRestoID.clear();
        mRestoName.clear();
        mRestoLogo.clear();
    }
    private class RestoClass implements Serializable {
        String resto_id;
        String resto_name;
        String resto_logo;
    }

    private List<RestoClass> feedListContent(ArrayList mArrayRestoID) {
        List<RestoClass> result = new ArrayList<>();
        for (int i = 0; i < mArrayRestoID.size(); i++) {
            RestoClass ci = new RestoClass();
            ci.resto_id = mArrayRestoID.get(i).toString();
            ci.resto_name = mRestoName.get(i);
            ci.resto_logo = mRestoLogo.get(i);
            result.add(ci);
        }
        return result;
    }
    private class RestoAdapter extends RecyclerView.Adapter {
        private final Context mContext;
        private final List<RestoClass> restoList;
        private final RecyclerView mRecyResto;

        RestoAdapter(Context applicationContext, List<RestoClass> restoList, RecyclerView mRecyResto) {
            this.mContext = applicationContext;
            this.restoList = restoList;
            this.mRecyResto = mRecyResto;
        }
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View rowView = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_resto_list, parent, false);
            return new RestoListHolder(rowView);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder hold, int position) {
            final RestoClass ci = restoList.get(position);
            RestoListHolder hldr = ((RestoListHolder) hold);
            hldr.lblRestoName.setText(ci.resto_name);
            Glide.with(mContext)
                    .load(ci.resto_logo)
                    .placeholder(R.drawable.circle_accent)
                    .transform(new CircleImageTransform(mContext))
                    .into(hldr.imgRestoLogo);
            hldr.imgRestoLogo.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent i = new Intent(mContext, RestaurantMenuListActivity.class);
                    Bundle bundle = new Bundle();
                    bundle.putString("resto_name", ci.resto_name);
                    bundle.putString("resto_logo", ci.resto_logo);
                    bundle.putString("resto_id", ci.resto_id);
                    i.putExtras(bundle);
                    i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(i);
                    overridePendingTransition(R.anim.slide_up, R.anim.zoom_out);
                }
            });
        }

        @Override
        public int getItemCount() {
            return restoList.size();
        }

        class RestoListHolder extends RecyclerView.ViewHolder {
            final ImageView imgRestoLogo;
            final TextView lblRestoName;

            RestoListHolder(View itemView) {
                super(itemView);
                imgRestoLogo        = (ImageView) itemView.findViewById(R.id.imgRestoLogo);
                lblRestoName        = (TextView) itemView.findViewById(R.id.lblRestoName);
            }
        }

        void resetData(List<RestoClass> listResto) {
            this.restoList.clear();
            this.restoList.addAll(listResto);
            notifyDataSetChanged();
        }
    }
}
