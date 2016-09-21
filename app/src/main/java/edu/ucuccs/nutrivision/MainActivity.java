package edu.ucuccs.nutrivision;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.clarifai.api.ClarifaiClient;
import com.clarifai.api.RecognitionRequest;
import com.clarifai.api.RecognitionResult;
import com.clarifai.api.Tag;
import com.clarifai.api.exception.ClarifaiException;
import com.github.clans.fab.FloatingActionButton;
import com.github.clans.fab.FloatingActionMenu;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import edu.ucuccs.nutrivision.custom.AdjustableLayout;

import static android.provider.MediaStore.Images.Media;

public class MainActivity extends AppCompatActivity {

    private final ClarifaiClient client = new ClarifaiClient(Credentials.CLARIFAI.CLIENT_ID, Credentials.CLARIFAI.CLIENT_SECRET);
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int CODE_PICK = 1;
    private static final int CODE_SHOT = 2;
    private static final int REQUEST_SHOT = 3;
    private static final int CODE_SPEAK = 4;
    private Intent data;

    private MaterialDialog.Builder materialDialog;
    private MaterialDialog mDialog;

    private final List<String> tagsListInitial = new ArrayList<>();
    private FloatingActionButton mFabCam, mFabBrowse, mFabSpeak;
    private FloatingActionMenu fabMenu;
    private AdjustableLayout adjustableLayout;
    private TextView mLblResultTags;
    private ImageView imgResult;
    private Toolbar mToolbar;

    private LinearLayout mLinearEmpty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        mFabCam = (FloatingActionButton) findViewById(R.id.menu_camera);
        mFabBrowse = (FloatingActionButton) findViewById(R.id.menu_browse);
        mFabSpeak = (FloatingActionButton) findViewById(R.id.menu_speak);
        fabMenu = (FloatingActionMenu) findViewById(R.id.fab_menu);
        imgResult = (ImageView) findViewById(R.id.img_result);
        mLblResultTags = (TextView) findViewById(R.id.lbl_result_tag);

        mLinearEmpty = (LinearLayout) findViewById(R.id.layout_empty_state);

        materialDialog = new MaterialDialog.Builder(this);
        mDialog = materialDialog.build();

        setUpToolbar();

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
    }

    void setUpToolbar() {
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        this.data = data;
        mLinearEmpty.setVisibility(View.GONE);

        if (requestCode == CODE_PICK && resultCode == RESULT_OK) {
            final Bitmap bitmap = loadBitmapFromUri(data.getData());
            if (bitmap != null) {
                imgResult.setImageBitmap(bitmap);
                callClarifai(bitmap);
            } else {
                mLblResultTags.setText("Unable to load selected image.");
            }
        }else if(requestCode == CODE_SHOT && resultCode == RESULT_OK){
            final Bitmap bitmap = (Bitmap) data.getExtras().get("data");
            if (bitmap != null) {
                imgResult.setImageBitmap(bitmap);
                callClarifai(bitmap);
            } else {
                mLblResultTags.setText("Unable to load selected image.");
            }
        }else if (requestCode == CODE_SPEAK && resultCode == RESULT_OK && null != data){
            final ArrayList<String> result = data
                    .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);


                    materialDialog.title(result.get(0))
                    .content("Is this the word you spoken?")
                    .positiveText("Yes")
                    .negativeText("No")
                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            Intent i = new Intent(getApplicationContext(), ResultActivity.class);
                            i.putExtra("str_tag", result.get(0));
                            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(i);
                        }
                    })
                    .onNegative(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            mDialog.dismiss();
                        }
                    }).show();

        }
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
                    String tempTags = txtChipTag.getText().toString();

                    File file = new File(data.getData().getPath());
                    Intent i = new Intent(getApplicationContext(), ResultActivity.class);
                    i.putExtra("str_tag", tempTags);
                    i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(i);
                    overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_left);
                }
            });
            txtChipTag.setText(tagList.get(i));
            adjustableLayout.addingMultipleView(newView);
        }
        adjustableLayout.invalidateView();
    }

}
