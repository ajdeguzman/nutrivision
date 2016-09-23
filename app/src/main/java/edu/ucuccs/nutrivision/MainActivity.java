package edu.ucuccs.nutrivision;

import android.Manifest;
import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.clarifai.api.ClarifaiClient;
import com.clarifai.api.RecognitionRequest;
import com.clarifai.api.RecognitionResult;
import com.clarifai.api.Tag;
import com.clarifai.api.exception.ClarifaiException;
import com.getkeepsafe.taptargetview.TapTarget;
import com.getkeepsafe.taptargetview.TapTargetView;
import com.github.clans.fab.FloatingActionButton;
import com.github.clans.fab.FloatingActionMenu;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import edu.ucuccs.nutrivision.custom.AdjustableLayout;

import static android.provider.MediaStore.Images.Media;

public class MainActivity extends AppCompatActivity{

    private final ClarifaiClient client = new ClarifaiClient(Credentials.CLARIFAI.CLIENT_ID, Credentials.CLARIFAI.CLIENT_SECRET);
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int CODE_PICK = 1;
    private static final int CODE_SHOT = 2;
    private static final int REQUEST_SHOT = 3;
    private static final int CODE_SPEAK = 4;
    private Intent data;

    private MenuItem item;

    private AlertDialog.Builder confirmTextDialog;

    private final List<String> tagsListInitial = new ArrayList<>();
    private FloatingActionButton mFabCam, mFabBrowse, mFabSpeak, mFabSearch;
    private FloatingActionMenu fabMenu;

    private AdjustableLayout adjustableLayout;
    private CoordinatorLayout layoutRoot;
    private TextView mLblResultTags;
    private TextView mLblEmptyState;

    private ImageView imgResult;
    private ImageView imgEmptyState;

    private Toolbar mToolbar;

    private LinearLayout mLinearEmpty;
    NetworkConnectivity mNetConn = new NetworkConnectivity(MainActivity.this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        mFabCam = (FloatingActionButton) findViewById(R.id.menu_camera);
        mFabBrowse = (FloatingActionButton) findViewById(R.id.menu_browse);
        mFabSpeak = (FloatingActionButton) findViewById(R.id.menu_speak);
        mFabSearch = (FloatingActionButton) findViewById(R.id.menu_search);
        fabMenu = (FloatingActionMenu) findViewById(R.id.fab_menu);
        imgResult = (ImageView) findViewById(R.id.img_result);
        mLblResultTags = (TextView) findViewById(R.id.lbl_result_tag);
        mLblEmptyState = (TextView) findViewById(R.id.lbl_empty_state);
        imgEmptyState = (ImageView) findViewById(R.id.img_empty_state);

        mLinearEmpty = (LinearLayout) findViewById(R.id.layout_empty_state);

        confirmTextDialog = new AlertDialog.Builder(this);

        setUpToolbar();

        handleIntent(getIntent());

        mFabCam.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                cameraShot();
                fabMenu.close(true);
            }
        });

        mFabBrowse.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                browseGallery();
                fabMenu.close(true);
            }
        });

        mFabSpeak.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                speechText();
                fabMenu.close(true);
            }
        });

        mFabSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                search();
                fabMenu.close(true);
            }
        });
        featureDiscovery();
    }

    void featureDiscovery(){
        TapTargetView.showFor(this, TapTarget.forView(findViewById(R.id.fabTemp), "This is the Action Button", "Browse, search, take pictures and instantly receive results")
                        .textColor(android.R.color.white)
                        .dimColor(android.R.color.black)
                        .drawShadow(true)
                        .cancelable(true)
                        .tintTarget(false),
                new TapTargetView.Listener() {
                    @Override
                    public void onTargetClick(TapTargetView view) {
                        super.onTargetClick(view);

                    }
                });
    }

    void setUpToolbar() {
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        handleIntent(intent);
    }

    public void cameraShot() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            final Intent intent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
            startActivityForResult(intent, CODE_SHOT);
        }else{
            requestPermission();
        }
    }
    void requestPermission(){
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_SHOT);
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_SHOT: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    cameraShot();
                } else {
                    Utils.showToast("You need to allow permission in order to capture images.", Toast.LENGTH_LONG);
                }
                return;
            }
        }
    }
    public void browseGallery() {
        final Intent intent = new Intent(Intent.ACTION_PICK, Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, CODE_PICK);
    }

    public void speechText(){
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT,
                getString(R.string.speech_prompt));

        try {
            startActivityForResult(intent, CODE_SPEAK);
        } catch (ActivityNotFoundException a) {
            Toast.makeText(getApplicationContext(),
                    getString(R.string.speech_not_supported),
                    Toast.LENGTH_SHORT).show();
        }
    }

    public void search(){
        item.expandActionView();
    }

    private void handleIntent(Intent intent){
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);

            Intent i = new Intent(getApplicationContext(), ResultActivity.class);
            i.putExtra("str_tag", query);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        this.data = data;

        if(mNetConn.isConnectedToInternet()){
            if (requestCode == CODE_PICK && resultCode == RESULT_OK) {
                final Bitmap bitmap = loadBitmapFromUri(data.getData());
                if (bitmap != null) {
                    mLinearEmpty.setVisibility(View.GONE);
                    imgResult.setImageBitmap(bitmap);
                    callClarifai(bitmap);
                } else {
                    mLblResultTags.setText("Unable to load selected image.");
                }
            }else if(requestCode == CODE_SHOT && resultCode == RESULT_OK){
                final Bitmap bitmap = (Bitmap) data.getExtras().get("data");
                if (bitmap != null) {
                    mLinearEmpty.setVisibility(View.GONE);
                    imgResult.setImageBitmap(bitmap);
                    callClarifai(bitmap);
                } else {
                    mLblResultTags.setText("Unable to load selected image.");
                }
            }else if (requestCode == CODE_SPEAK && resultCode == RESULT_OK && null != data){
                final ArrayList<String> result = data
                        .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                confirmTextDialog.setTitle("Is this correct?")
                        .setMessage(result.get(0))
                        .setPositiveButton("Submit", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Intent i = new Intent(getApplicationContext(), ResultActivity.class);
                                i.putExtra("str_tag", result.get(0));
                                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(i);
                            }
                        })
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        })
                        .setNeutralButton("Retry", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                speechText();
                            }
                        })
                        .show();

            }
        }else if(resultCode == RESULT_CANCELED){
            mLinearEmpty.setVisibility(View.VISIBLE);
        }else{
            showNoConnectionState();
        }
    }
    void showNoConnectionState(){
        imgEmptyState.setImageResource(R.drawable.ic_cloud_off_black_24dp);
        mLblEmptyState.setText(R.string.msg_no_connection);
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
    void callClarifai(Bitmap bitmap){
        mLblResultTags.setText("Recognizing...");
        new AsyncTask<Bitmap, Void, RecognitionResult>() {
            @Override protected RecognitionResult doInBackground(Bitmap... bitmaps) {
                Log.d(TAG, "doInBackground: "  + bitmaps[0]);
                return recognizeBitmap(bitmaps[0]);
            }
            @Override protected void onPostExecute(RecognitionResult result) {
                Log.d(TAG, "onPostExecute: " + result);
                updateUIForResult(result);
            }
        }.execute(bitmap);
    }
    private Bitmap loadBitmapFromUri(Uri uri) {
        try {
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(getContentResolver().openInputStream(uri), null, opts);
            int sampleSize = 1;
            while (opts.outWidth / (2 * sampleSize) >= imgResult.getWidth() &&
                    opts.outHeight / (2 * sampleSize) >= imgResult.getHeight()) {
                sampleSize *= 2;
            }

            opts = new BitmapFactory.Options();
            opts.inSampleSize = sampleSize;
            return BitmapFactory.decodeStream(getContentResolver().openInputStream(uri), null, opts);
        } catch (IOException e) {
            Log.e(TAG, "Error loading image: " + uri, e);
        }
        return null;
    }

    private RecognitionResult recognizeBitmap(Bitmap bitmap) {
        try {
            Bitmap scaled = Bitmap.createScaledBitmap(bitmap, 320,
                    320 * bitmap.getHeight() / bitmap.getWidth(), true);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            scaled.compress(Bitmap.CompressFormat.JPEG, 90, out);
            byte[] jpeg = out.toByteArray();

            return client.recognize(new RecognitionRequest(jpeg)).get(0);
        } catch (ClarifaiException e) {
            return null;
        }
    }

    private void updateUIForResult(RecognitionResult result) {
        tagsListInitial.clear();

        if (result != null) {
            if (result.getStatusCode() == RecognitionResult.StatusCode.OK) {
                StringBuilder b = new StringBuilder();
                for (Tag tag : result.getTags()) {
                    tagsListInitial.add(tag.getName());
                    b.append(b.length() > 0 ? ", " : "").append(tag.getName());
                }
                mLblResultTags.setVisibility(View.GONE);
                addChipsViewFinal(tagsListInitial);
            } else {
                mLblResultTags.setText("Sorry, there was an error recognizing your image.");
            }
        } else {
            mLblResultTags.setText("Sorry, there was an error recognizing your image.");
        }
    }
    void submitTag(String tag){
        Intent i = new Intent(getApplicationContext(), ResultActivity.class);
        i.putExtra("str_tag", tag);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(i);
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_left);
    }
    private void addChipsViewFinal(List<String> tagList) {
        adjustableLayout = (AdjustableLayout) findViewById(R.id.container);
        adjustableLayout.removeAllViews();
        for (int i = 0; i < tagList.size(); i++) {
            final View newView = LayoutInflater.from(this).inflate(R.layout.layout_view_chip_text, null);
            LinearLayout linearChipTag = (LinearLayout) newView.findViewById(R.id.linear_chip_tag);
            final TextView txtChipTag = (TextView) newView.findViewById(R.id.txt_chip_content);

            linearChipTag.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    final String tempTags = txtChipTag.getText().toString();
                    if(mNetConn.isConnectedToInternet()){
                        submitTag(tempTags);
                    }else{
                        Utils.showSnackBar("Can't connect right now", layoutRoot, Toast.LENGTH_LONG);
                        Snackbar snackbar = Snackbar.make(layoutRoot, R.string.msg_no_connection_short, Snackbar.LENGTH_LONG)
                                .setAction(R.string.action_retry, new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        submitTag(tempTags);
                                    }
                                });
                        snackbar.setDuration(Snackbar.LENGTH_INDEFINITE);
                        snackbar.show();
                    }
                }
            });
            txtChipTag.setText(tagList.get(i));
            adjustableLayout.addingMultipleView(newView);
        }
        adjustableLayout.invalidateView();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.options_menu, menu);
        MenuItem mMenuSearch = menu.findItem(R.id.search);
        this.item = mMenuSearch;

        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView = (SearchView) menu.findItem(R.id.search).getActionView();
        searchView.setSearchableInfo( searchManager.getSearchableInfo(getComponentName()) );
        return true;
    }
}
