package edu.ucuccs.nutrivision;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.speech.RecognizerIntent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.clarifai.api.ClarifaiClient;
import com.clarifai.api.RecognitionRequest;
import com.clarifai.api.RecognitionResult;
import com.clarifai.api.Tag;
import com.clarifai.api.exception.ClarifaiException;
import com.flipboard.bottomsheet.BottomSheetLayout;
import com.flipboard.bottomsheet.commons.ImagePickerSheetView;
import com.github.clans.fab.FloatingActionButton;
import com.github.clans.fab.FloatingActionMenu;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

import edu.ucuccs.nutrivision.custom.AdjustableLayout;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

import static android.provider.MediaStore.Images.Media;

public class MainActivity extends AppCompatActivity {

    private final ClarifaiClient client = new ClarifaiClient(Credentials.CLARIFAI.CLIENT_ID, Credentials.CLARIFAI.CLIENT_SECRET);
    private static final String TAG = MainActivity.class.getSimpleName();

    private static final int CODE_PICK = 1;
    private static final int CODE_SHOT = 2;
    private static final int REQUEST_SHOT = 3;
    private static final int CODE_SPEAK = 4;

    private static final int REQUEST_STORAGE = 0;
    private static final int REQUEST_IMAGE_CAPTURE = REQUEST_STORAGE + 1;
    private static final int REQUEST_LOAD_IMAGE = REQUEST_IMAGE_CAPTURE + 1;

    private BottomSheetLayout bottomSheetGallery;
    private MenuItem item;

    private AlertDialog.Builder confirmTextDialog;

    private final List<String> tagsListInitial = new ArrayList<>();
    private FloatingActionMenu fabMenu;

    private CoordinatorLayout layoutRoot;
    private TextView mLblResultTags, mLblSelectTag;
    private TextView mLblEmptyState;
    private Uri cameraImageUri = null;

    private ImageView imgResult;
    private ImageView imgEmptyState;

    private Toolbar mToolbar;

    private LinearLayout mLinearEmpty;
    NetworkConnectivity mNetConn = new NetworkConnectivity(MainActivity.this);

    private Uri imageUri;
    private Bitmap thumbnail;

    public byte[] jpeg;

    String[] PERMISSIONS = {
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_NETWORK_STATE
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FloatingActionButton mFabResto = (FloatingActionButton) findViewById(R.id.menu_resto);
        FloatingActionButton mFabCam = (FloatingActionButton) findViewById(R.id.menu_camera);
        FloatingActionButton mFabBrowse = (FloatingActionButton) findViewById(R.id.menu_browse);
        FloatingActionButton mFabSpeak = (FloatingActionButton) findViewById(R.id.menu_speak);
        FloatingActionButton mFabSearch = (FloatingActionButton) findViewById(R.id.menu_search);

        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        bottomSheetGallery = (BottomSheetLayout) findViewById(R.id.btmsheet_gallery);
        layoutRoot = (CoordinatorLayout) findViewById(R.id.layoutRoot);
        fabMenu = (FloatingActionMenu) findViewById(R.id.fab_menu);
        imgResult = (ImageView) findViewById(R.id.img_result);
        mLblResultTags = (TextView) findViewById(R.id.lbl_result_tag);
        mLblEmptyState = (TextView) findViewById(R.id.lbl_empty_state);
        imgEmptyState = (ImageView) findViewById(R.id.img_empty_state);
        mLblSelectTag = (TextView) findViewById(R.id.lbl_select_tag);

        mLinearEmpty = (LinearLayout) findViewById(R.id.layout_empty_state);
        confirmTextDialog = new AlertDialog.Builder(this);

        fabMenu.setClosedOnTouchOutside(true);

        setUpToolbar();
        handleIntent(getIntent());

        grantPermissions();

        getSend();

        mFabResto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getApplicationContext(), RestaurantActivity.class));
                fabMenu.close(true);
            }
        });
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
                if (checkNeedsPermission()) {
                    requestStoragePermission();
                } else {
                    showSheetView();
                }
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
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }

    void setUpToolbar() {
        setSupportActionBar(mToolbar);
        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayShowHomeEnabled(true);
    }

    void grantPermissions() {
        if (!hasPermissions(getApplicationContext(), PERMISSIONS)) {
            ActivityCompat.requestPermissions(MainActivity.this, PERMISSIONS, 1);
        }
    }

    private static boolean hasPermissions(Context context, String... permissions) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    public void getSend() {
        // Get intent, action and MIME type
        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        if (Intent.ACTION_SEND.equals(action) && type != null) {

            if (type.startsWith("image/")) {
                handleSendImage(intent); // Handle single image being sent
            }
        }
    }

    void handleSendImage(Intent intent) {
        Uri imageUriSend = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
        if (imageUriSend != null) {
            // Update UI to reflect image being shared

            try {
                mLinearEmpty.setVisibility(View.GONE);
                InputStream inputStream = null;
                inputStream = getContentResolver().openInputStream(imageUriSend);
                thumbnail = BitmapFactory.decodeStream(inputStream);
                imgResult.setImageBitmap(thumbnail);
                callClarifai(thumbnail);
            } catch (FileNotFoundException e) {
                mLblResultTags.setText(R.string.err_unable_load_image);
                e.printStackTrace();
            }
        } else {
            mLblResultTags.setText(R.string.err_unable_load_image);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        handleIntent(intent);
    }

    public void cameraShot() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            ContentValues values = new ContentValues();
            values.put(Media.TITLE, "Food Picture");
            values.put(Media.DESCRIPTION, "From Camera");
            imageUri = getContentResolver().insert(
                    Media.EXTERNAL_CONTENT_URI, values);
            final Intent intent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
            startActivityForResult(intent, CODE_SHOT);
        } else {
            requestPermission();
        }
    }

    void requestPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_SHOT);
    }


    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_SHOT: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    cameraShot();
                } else {
                    Utils.showToast(getString(R.string.info_allow_permission_capture), Toast.LENGTH_LONG);
                }
            }
            break;
            case REQUEST_STORAGE:
                if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    hideSoftKeyboard();
                    showSheetView();
                } else {
                    Toast.makeText(this, "Allow permission to access external storage", Toast.LENGTH_SHORT).show();
                }
                break;
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    public void speechText() {
        Intent mIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        mIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        mIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        mIntent.putExtra(RecognizerIntent.EXTRA_PROMPT, getString(R.string.speech_prompt));

        try {
            startActivityForResult(mIntent, CODE_SPEAK);
        } catch (ActivityNotFoundException a) {
            Toast.makeText(getApplicationContext(), getString(R.string.speech_not_supported), Toast.LENGTH_SHORT).show();
        }
    }

    public void search() {
        item.expandActionView();
    }

    private void handleIntent(Intent intent) {
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);

            Intent i = new Intent(getApplicationContext(), ResultActivity.class);
            i.putExtra("str_tag", query);
            i.putExtra("type", 1);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (mNetConn.isConnectedToInternet()) {
            if (requestCode == CODE_PICK && resultCode == RESULT_OK) {
                final Bitmap bitmap = loadBitmapFromUri(data.getData());
                if (bitmap != null) {
                    mLinearEmpty.setVisibility(View.GONE);
                    imgResult.setImageBitmap(bitmap);
                    callClarifai(bitmap);
                } else {
                    mLblResultTags.setText(R.string.err_msg_unable_to_load_image);
                }
            } else if (requestCode == CODE_SHOT && resultCode == RESULT_OK) {
                try {
                    mLinearEmpty.setVisibility(View.GONE);
                    thumbnail = MediaStore.Images.Media.getBitmap(
                            getContentResolver(), imageUri);
                    imgResult.setImageBitmap(thumbnail);
                    callClarifai(thumbnail);
                } catch (Exception e) {
                    mLblResultTags.setText(R.string.err_unable_load_image);
                    e.printStackTrace();
                }

            } else if (requestCode == CODE_SPEAK && resultCode == RESULT_OK && null != data) {
                final ArrayList<String> result = data
                        .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                confirmTextDialog.setTitle(R.string.confirm_msg_correct)
                        .setMessage(result.get(0))
                        .setPositiveButton(R.string.action_submit, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Intent i = new Intent(getApplicationContext(), ResultActivity.class);
                                i.putExtra("str_tag", result.get(0));
                                i.putExtra("type", 1);
                                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(i);
                            }
                        })
                        .setNegativeButton(R.string.action_cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        })
                        .setNeutralButton(R.string.action_retry, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                speechText();
                            }
                        })
                        .show();

            }
        } else if (resultCode == RESULT_CANCELED) {
            mLinearEmpty.setVisibility(View.VISIBLE);
        } else {
            showNoConnectionState();
        }

        if (resultCode == Activity.RESULT_OK) {
            Uri selectedImage = null;
            if (requestCode == REQUEST_LOAD_IMAGE && data != null) {
                selectedImage = data.getData();
                if (selectedImage == null) {
                    genericError();
                }
            } else if (requestCode == REQUEST_IMAGE_CAPTURE) {
                selectedImage = cameraImageUri;
            }

            if (selectedImage != null) {
                try {
                    sendSelectedImage(selectedImage);
                } catch (ExecutionException | InterruptedException e) {
                    e.printStackTrace();
                }
            } else {
                genericError();
            }
        }
    }

    void showNoConnectionState() {
        imgEmptyState.setImageResource(R.drawable.empty_state_onion_connection);
        mLblEmptyState.setText(R.string.msg_no_connection);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    void callClarifai(Bitmap bitmap) {
        mLblResultTags.setText(R.string.info_msg_loading);
        new AsyncTask<Bitmap, Void, RecognitionResult>() {
            @Override
            protected RecognitionResult doInBackground(Bitmap... bitmaps) {
                Log.d(TAG, "doInBackground: " + bitmaps[0]);
                return recognizeBitmap(bitmaps[0]);
            }

            @Override
            protected void onPostExecute(RecognitionResult result) {
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
            while (opts.outWidth / (2 * sampleSize) >= imgResult.getWidth() && opts.outHeight / (2 * sampleSize) >= imgResult.getHeight()) {
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
            jpeg = out.toByteArray();

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
                mLblSelectTag.setVisibility(View.VISIBLE);
                addChipsViewFinal(tagsListInitial);
            } else {
                mLblResultTags.setText(R.string.err_msg_unrecognized_image);
            }
        } else {
            mLblResultTags.setText(R.string.err_msg_unrecognized_image);
        }
    }

    void submitTag(String tag) {
        Intent i = new Intent(getApplicationContext(), ResultActivity.class);
        i.putExtra("str_tag", tag);
        i.putExtra("type", 1);
        i.putExtra("byteArray", jpeg);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(i);
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_left);
    }

    private void addChipsViewFinal(List<String> tagList) {
        AdjustableLayout adjustableLayout = (AdjustableLayout) findViewById(R.id.container);
        adjustableLayout.removeAllViews();
        for (int i = 0; i < tagList.size(); i++) {
            @SuppressLint("InflateParams") final View newView = LayoutInflater.from(this).inflate(R.layout.layout_view_chip_text, null);
            LinearLayout linearChipTag = (LinearLayout) newView.findViewById(R.id.linear_chip_tag);
            final TextView txtChipTag = (TextView) newView.findViewById(R.id.txt_chip_content);

            linearChipTag.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    final String tempTags = txtChipTag.getText().toString();
                    if (mNetConn.isConnectedToInternet()) {
                        submitTag(tempTags);
                    } else {
                        Utils.showSnackBar(getString(R.string.err_msg_cannot_connect), layoutRoot, Toast.LENGTH_LONG);
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
        this.item = menu.findItem(R.id.search);

        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView = (SearchView) menu.findItem(R.id.search).getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        return true;
    }

    /*
    *
    * Display bottomsheet
     */
    private boolean checkNeedsPermission() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED;
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void requestStoragePermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_STORAGE);
        } else {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_STORAGE);
        }
    }

    /**
     * Show an {@link ImagePickerSheetView}
     */
    private void showSheetView() {
        ImagePickerSheetView sheetView = new ImagePickerSheetView.Builder(this)
                .setMaxItems(30)
                .setShowCameraOption(createCameraIntent() != null)
                .setShowPickerOption(createPickIntent() != null)
                .setImageProvider(new ImagePickerSheetView.ImageProvider() {
                    @Override
                    public void onProvideImage(ImageView imageView, Uri imageUri, int size) {
                        Glide.with(MainActivity.this)
                                .load(imageUri)
                                .crossFade()
                                .into(imageView);
                    }
                })
                .setOnTileSelectedListener(new ImagePickerSheetView.OnTileSelectedListener() {
                    @Override
                    public void onTileSelected(ImagePickerSheetView.ImagePickerTile selectedTile) {
                        bottomSheetGallery.dismissSheet();
                        if (selectedTile.isCameraTile()) {
                            dispatchTakePictureIntent();
                        } else if (selectedTile.isPickerTile()) {
                            startActivityForResult(createPickIntent(), REQUEST_LOAD_IMAGE);
                        } else if (selectedTile.isImageTile()) {
                            try {
                                sendSelectedImage(selectedTile.getImageUri());
                            } catch (ExecutionException | InterruptedException e) {
                                e.printStackTrace();
                            }
                        } else {
                            genericError();
                        }
                    }
                })
                .setTitle("Select an image...")
                .create();

        bottomSheetGallery.showWithSheetView(sheetView);
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        File imageFile = File.createTempFile(
                imageFileName,
                ".jpg",
                storageDir
        );

        cameraImageUri = Uri.fromFile(imageFile);
        return imageFile;
    }

    @Nullable
    private Intent createPickIntent() {
        Intent picImageIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        if (picImageIntent.resolveActivity(getPackageManager()) != null) {
            return picImageIntent;
        } else {
            return null;
        }
    }

    @Nullable
    private Intent createCameraIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            return takePictureIntent;
        } else {
            return null;
        }
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = createCameraIntent();
        if (takePictureIntent != null) {
            try {
                File imageFile = createImageFile();
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(imageFile));
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            } catch (IOException e) {
                genericError("Could not create imageFile for camera");
            }
        }
    }

    private void sendSelectedImage(Uri selectedImageUri) throws ExecutionException, InterruptedException {
        Log.d("uri", selectedImageUri + "");
        imgResult.setImageDrawable(null);
        Bitmap bitmap = null;
        String photo = null;
        try {
            bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImageUri);
            photo = Utils.convertBitmapToBase64(bitmap);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Drawable d = new BitmapDrawable(getResources(), bitmap);
        imgResult.setImageDrawable(d);
        callClarifai(bitmap);
        mLinearEmpty.setVisibility(View.GONE);
    }

    private void genericError() {
        genericError(null);
    }

    private void genericError(String message) {
        Toast.makeText(this, message == null ? "Something went wrong." : message, Toast.LENGTH_SHORT).show();
    }

    private void hideSoftKeyboard() {
        if (getCurrentFocus() != null) {
            InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        }
    }


}