package net.alexblass.capstoneproject;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;

import android.support.v4.app.LoaderManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.constraint.ConstraintLayout;
import android.support.v4.content.Loader;
import android.support.v7.app.AlertDialog;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import net.alexblass.capstoneproject.models.User;
import net.alexblass.capstoneproject.utils.UserDataUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import butterknife.BindColor;
import butterknife.BindString;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static net.alexblass.capstoneproject.data.Keys.USER_BIRTHDAY_KEY;
import static net.alexblass.capstoneproject.data.Keys.USER_DESCRIPTION_KEY;
import static net.alexblass.capstoneproject.data.Keys.USER_DEVICE_TOKEN;
import static net.alexblass.capstoneproject.data.Keys.USER_GENDER_KEY;
import static net.alexblass.capstoneproject.data.Keys.USER_KEY;
import static net.alexblass.capstoneproject.data.Keys.USER_NAME_KEY;
import static net.alexblass.capstoneproject.data.Keys.USER_PROFILE_IMG_KEY;
import static net.alexblass.capstoneproject.data.Keys.USER_RELATIONSHIP_KEY;
import static net.alexblass.capstoneproject.data.Keys.USER_SEXUALITY_KEY;
import static net.alexblass.capstoneproject.data.Keys.USER_ZIPCODE_KEY;

public class EditActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<String> {

    private static final int ACTION_SIGN_OUT = 0;
    private static final int ACTION_RETURN_TO_DASH = 1;
    private static final int ACTION_TO_MESSAGES = 2;
    private static final int SELECT_PICTURE = 100;

    private static final String GENDERS_LIST_KEY = "genders_list";
    private static final String SEXUALITIESS_LIST_KEY = "sexualities_list";
    private static final String RELATIONSHIPS_LIST_KEY = "relationships_list";

    @BindView(R.id.edit_name_et) EditText mNameEt;
    @BindView(R.id.edit_zipcode_et) EditText mZipcodeEt;
    @BindView(R.id.edit_description_et) EditText mDescriptionEt;
    @BindView(R.id.edit_gender_spinner) Spinner mGenderSpinnner;
    @BindView(R.id.edit_sexuality_spinner) Spinner mSexualitySpinner;
    @BindView(R.id.edit_relationship_spinner) Spinner mRelationshipStatusSpinner;
    @BindView(R.id.edit_parent) ConstraintLayout mParent;
    @BindView(R.id.edit_add_img_btn) ImageButton mProfileImage;
    @BindView(R.id.edit_remove_txt) TextView mRemoveImageTv;

    @BindView(R.id.edit_name_helper) TextView mNameHelperTv;
    @BindView(R.id.edit_zipcode_helper) TextView mZipcodeHelperTv;
    @BindView(R.id.edit_gender_error) TextView mGenderErrorTv;
    @BindView(R.id.edit_sexuality_error) TextView mSexualityErrorTv;
    @BindView(R.id.edit_relationship_error) TextView mRelationshipErrorTv;

    @BindString(R.string.required_field) String mRequired;
    @BindString(R.string.invalid_entry) String mEntryErrorTitle;
    @BindColor(R.color.validation_error) int mErrorColor;
    @BindColor(R.color.colorPrimary) int mHelperColor;

    private FirebaseAuth mAuth;
    private StorageReference mStorageRef;
    private StorageReference mUserProfilePic;

    private User mUser;
    private long mBirthday;
    private String mName;
    private String mZipcode;
    private boolean mValidZipcode;
    private String mImageUriString;
    private boolean mFirstEdit;

    private ArrayList<String> mGendersList;
    private ArrayList<String> mSexualitiesList;
    private ArrayList<String> mRelationshipsList;

    private AlertDialog mOfflineDialog;

    private Bundle mSavedInstanceState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit);
        ButterKnife.bind(this);

        mSavedInstanceState = savedInstanceState;
        loadActivity(savedInstanceState);
    }

    @OnClick({R.id.edit_add_img_txt, R.id.edit_add_img_btn})
    public void addImage(){
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), SELECT_PICTURE);
    }

    @OnClick(R.id.edit_remove_txt)
    public void removeImage(){
        deleteImage(mImageUriString);
        mImageUriString = "";
        Picasso.with(EditActivity.this)
                .load(R.drawable.ic_person_white_48dp)
                .placeholder(R.drawable.ic_person_white_48dp)
                .centerCrop()
                .fit()
                .into(mProfileImage);
        mRemoveImageTv.setVisibility(View.GONE);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == SELECT_PICTURE) {
                mRemoveImageTv.setVisibility(View.VISIBLE);
                Uri selectedImageUri = data.getData();
                if (selectedImageUri != null) {
                    mImageUriString = selectedImageUri.toString();
                    Picasso.with(EditActivity.this)
                            .load(selectedImageUri)
                            .placeholder(R.drawable.ic_person_white_48dp)
                            .centerCrop()
                            .fit()
                            .into(mProfileImage);
                }
            }
        }
    }

    @OnClick(R.id.edit_save_btn)
    public void saveData(){
        mName = mNameEt.getText().toString().trim();
        if (mName.isEmpty()){
            showDialog(mEntryErrorTitle, getString(R.string.empty_name));
            mNameEt.requestFocus();
            mNameHelperTv.setTextColor(mErrorColor);
            mNameHelperTv.setText(mRequired);
            return;
        }

        mZipcode = mZipcodeEt.getText().toString().trim();

        if (mZipcode.isEmpty() || mZipcode.length() != 5){
            showDialog(mEntryErrorTitle, getString(R.string.invalid_zipcode));
            mZipcodeEt.requestFocus();
            mZipcodeHelperTv.setTextColor(mErrorColor);
            mZipcodeHelperTv.setText(mRequired);
            return;
        } else {
            LoaderManager loaderManager = getSupportLoaderManager();
            loaderManager.initLoader(0, null, this);
        }

        String description = mDescriptionEt.getText().toString().trim();

        long gender = mGenderSpinnner.getSelectedItemId();
        if (mGenderSpinnner.getSelectedItemId() == 0){
            showDialog(mEntryErrorTitle, getString(R.string.invalid_gender));
            mGenderErrorTv.setVisibility(View.VISIBLE);
            return;
        }

        String sexuality = mSexualitySpinner.getSelectedItem().toString();
        if (mSexualitySpinner.getSelectedItemId() == 0){
            showDialog(mEntryErrorTitle, getString(R.string.invalid_sexuality));
            mSexualityErrorTv.setVisibility(View.VISIBLE);
            return;
        }

        String relationshipStatus = mRelationshipStatusSpinner.getSelectedItem().toString();
        if (mRelationshipStatusSpinner.getSelectedItemId() == 0){
            showDialog(mEntryErrorTitle, getString(R.string.invalid_relationship));
            mRelationshipErrorTv.setVisibility(View.VISIBLE);
            return;
        }

        String email = mAuth.getCurrentUser().getEmail();

        String oldProfilePicUri = "";
        if (mUser != null &&
                !mUser.getProfilePicUri().isEmpty() &&
                !mUser.getProfilePicUri().equals(mImageUriString)){
            oldProfilePicUri = mUser.getProfilePicUri();
        }

        String bannerPicUri = "";
        if (mUser != null &&
                !mUser.getBannerPicUri().isEmpty()){
            bannerPicUri = mUser.getBannerPicUri();
        }

        mUser = new User(email, mName, mBirthday, mZipcode, gender, sexuality, relationshipStatus,
                description, mImageUriString, bannerPicUri);

        DatabaseReference database = FirebaseDatabase.getInstance().getReference(email.replace(".", "(dot)"));
        database.setValue(mUser);

        database = FirebaseDatabase.getInstance().getReference(
                mAuth.getCurrentUser().getEmail().replace(".", "(dot)"))
                .child(USER_DEVICE_TOKEN);
        database.setValue(FirebaseInstanceId.getInstance().getToken());

        if (!mImageUriString.isEmpty()){
            uploadImage();

            if (!oldProfilePicUri.isEmpty()) {
                deleteImage(oldProfilePicUri);
            }
        }

        Toast.makeText(this, getString(R.string.change_saved), Toast.LENGTH_SHORT).show();

        Intent dashboardActivity = new Intent(getApplicationContext(), DashboardActivity.class);
        dashboardActivity.putExtra(USER_KEY, mUser);
        startActivity(dashboardActivity);
    }

    private void uploadImage(){
        mUserProfilePic = mStorageRef.child(Uri.parse(mImageUriString).getPath());
        mUserProfilePic.putFile(Uri.parse(mImageUriString))
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        Uri downloadUrl = taskSnapshot.getDownloadUrl();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        exception.printStackTrace();
                        Toast.makeText(EditActivity.this, getString(R.string.upload_error), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void deleteImage(String oldProfilePicUri){
        StorageReference photoRef = mStorageRef.child(Uri.parse(oldProfilePicUri).getPath());
        photoRef.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                // Picture deleted from storage, take no action
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // Picture is still in storage, take no action
            }
        });
    }

    private void showDialog(final String title, String body){
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle(title)
                .setMessage(body)
                .setPositiveButton(getString(R.string.okay), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        if (!title.equals(mEntryErrorTitle)){
                            getFragmentManager().popBackStack();
                        }
                    }
                });
        dialog.create().show();
    }

    private void clearFocus(){
        View view = getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            mNameEt.clearFocus();
            mZipcodeEt.clearFocus();
            mDescriptionEt.clearFocus();

            mNameHelperTv.setVisibility(View.GONE);
            mZipcodeHelperTv.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        AlertDialog.Builder offlineDialog = new AlertDialog.Builder(this);
        offlineDialog.setTitle(getString(R.string.offline_edits_dialog_title))
                .setMessage(getString(R.string.offline_edits_prompt))
                .setPositiveButton(getString(R.string.okay), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        finish();
                    }
                });
        mOfflineDialog = offlineDialog.create();

        if (!UserDataUtils.checkNetworkConnectivity(this)) {
            mOfflineDialog.show();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mOfflineDialog != null){
            mOfflineDialog.dismiss();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putParcelable(USER_KEY, mUser);

        outState.putString(USER_NAME_KEY, mNameEt.getText().toString().trim());
        outState.putString(USER_ZIPCODE_KEY, mZipcodeEt.getText().toString().trim());
        outState.putString(USER_GENDER_KEY, mGenderSpinnner.getSelectedItem().toString());
        outState.putString(USER_SEXUALITY_KEY, mSexualitySpinner.getSelectedItem().toString());
        outState.putString(USER_RELATIONSHIP_KEY, mRelationshipStatusSpinner.getSelectedItem().toString());
        outState.putString(USER_DESCRIPTION_KEY, mDescriptionEt.getText().toString().trim());
        outState.putString(USER_PROFILE_IMG_KEY, mImageUriString);

        outState.putStringArrayList(GENDERS_LIST_KEY, mGendersList);
        outState.putStringArrayList(SEXUALITIESS_LIST_KEY, mSexualitiesList);
        outState.putStringArrayList(RELATIONSHIPS_LIST_KEY, mRelationshipsList);

        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        mSavedInstanceState = savedInstanceState;

        if (savedInstanceState != null) {
            mUser = savedInstanceState.getParcelable(USER_KEY);

            mGendersList = savedInstanceState.getStringArrayList(GENDERS_LIST_KEY);
            mSexualitiesList = savedInstanceState.getStringArrayList(SEXUALITIESS_LIST_KEY);
            mRelationshipsList = savedInstanceState.getStringArrayList(RELATIONSHIPS_LIST_KEY);

            mName = savedInstanceState.getString(USER_NAME_KEY);
            mZipcode = savedInstanceState.getString(USER_ZIPCODE_KEY);

            mNameEt.setText(mName);
            mZipcodeEt.setText(mZipcode);
            mGenderSpinnner.setSelection(mGendersList.indexOf(savedInstanceState.getString(USER_GENDER_KEY)));
            mSexualitySpinner.setSelection(mSexualitiesList.indexOf(savedInstanceState.getString(USER_SEXUALITY_KEY)));
            mRelationshipStatusSpinner.setSelection(mRelationshipsList.indexOf(savedInstanceState.getString(USER_RELATIONSHIP_KEY)));
            mDescriptionEt.setText(savedInstanceState.getString(USER_DESCRIPTION_KEY));

            mImageUriString = savedInstanceState.getString(USER_PROFILE_IMG_KEY);
            loadImage();
        }
    }

    private void loadActivity(Bundle savedInstanceState) {
        AlertDialog.Builder offlineDialog = new AlertDialog.Builder(this);
        offlineDialog.setTitle(getString(R.string.offline_edits_dialog_title))
                .setMessage(getString(R.string.offline_edits_prompt))
                .setPositiveButton(getString(R.string.okay), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        finish();
                    }
                });
        mOfflineDialog = offlineDialog.create();

        if (!UserDataUtils.checkNetworkConnectivity(this)) {
            mOfflineDialog.show();
        }

        mAuth = FirebaseAuth.getInstance();
        mStorageRef = FirebaseStorage.getInstance().getReference();
        mValidZipcode = false;

        if (savedInstanceState == null) {
            mImageUriString = "";

            mGendersList = new ArrayList<>(Arrays.asList(getResources().getStringArray(R.array.gender_choices)));
            mGenderSpinnner.setAdapter(getArrayAdapter(mGendersList));

            mSexualitiesList = new ArrayList<>(Arrays.asList(getResources().getStringArray(R.array.sexuality_choices)));
            mSexualitySpinner.setAdapter(getArrayAdapter(mSexualitiesList));

            mRelationshipsList = new ArrayList<>(Arrays.asList(getResources().getStringArray(R.array.relationship_choices)));
            mRelationshipStatusSpinner.setAdapter(getArrayAdapter(mRelationshipsList));

            Intent intentThatStartedThisActivity = getIntent();
            if (intentThatStartedThisActivity.hasExtra(USER_KEY)) {
                mFirstEdit = false;
                getSupportActionBar().setDisplayHomeAsUpEnabled(!mFirstEdit);

                mUser = intentThatStartedThisActivity.getParcelableExtra(USER_KEY);
                mName = mUser.getName();
                mBirthday = mUser.getBirthday();
                mZipcode = mUser.getZipcode();
                mImageUriString = mUser.getProfilePicUri();

                mNameEt.setText(mName);
                mZipcodeEt.setText(mZipcode);
                mDescriptionEt.setText(mUser.getDescription());
                mGenderSpinnner.setSelection((int) mUser.getGenderCode());
                mSexualitySpinner.setSelection(mSexualitiesList.indexOf(mUser.getSexuality()));
                mRelationshipStatusSpinner.setSelection(mRelationshipsList.indexOf(mUser.getRelationshipStatus()));

                loadImage();
            } else {
                mFirstEdit = true;
                getSupportActionBar().setDisplayHomeAsUpEnabled(!mFirstEdit);

                mName = null;
                mBirthday = 0;

                Query query = FirebaseDatabase.getInstance().getReference().child(
                        mAuth.getCurrentUser().getEmail().replace(".", "(dot)"));
                query.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {

                            mName = (String) dataSnapshot.child(USER_NAME_KEY).getValue();
                            mNameEt.setText(mName);

                            mBirthday = (long) dataSnapshot.child(USER_BIRTHDAY_KEY).getValue();
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Toast.makeText(getApplicationContext(), getResources().getString(R.string.verification_error), Toast.LENGTH_SHORT).show();
                    }
                });

                mAuth = FirebaseAuth.getInstance();
            }
        }

        setFocusListeners();
    }

    private void loadImage(){
        if (!mImageUriString.isEmpty()) {
            mUserProfilePic = mStorageRef.child(Uri.parse(mImageUriString).getPath());
            try {
                final File localFile = File.createTempFile("images", "jpg");
                mUserProfilePic.getFile(localFile)
                        .addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                            @Override
                            public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                                mRemoveImageTv.setVisibility(View.VISIBLE);
                                Picasso.with(EditActivity.this)
                                        .load(localFile)
                                        .placeholder(R.drawable.ic_person_white_48dp)
                                        .centerCrop()
                                        .fit()
                                        .into(mProfileImage);
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        exception.printStackTrace();
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private ArrayAdapter<String> getArrayAdapter(List<String> list){

        final ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<String>(
                this, R.layout.item_edit_profile_hint, list){
            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {

                View v = null;

                if (position == 0) {
                    TextView tv = new TextView(getContext());
                    tv.setHeight(0);
                    tv.setVisibility(View.GONE);
                    v = tv;
                }
                else {

                    v = super.getDropDownView(position, null, parent);
                }

                parent.setVerticalScrollBarEnabled(false);
                return v;
            }
        };
        spinnerArrayAdapter.setDropDownViewResource(R.layout.item_edit_profile_spinner);
        return spinnerArrayAdapter;
    }

    private void setFocusListeners(){
        mParent.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                clearFocus();
                return false;
            }
        });

        mNameEt.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                if (hasFocus){
                    mNameHelperTv.setVisibility(View.VISIBLE);
                    mNameHelperTv.setTextColor(mHelperColor);
                    mNameHelperTv.setText(getString(R.string.name_helper));
                } else {
                    mNameHelperTv.setVisibility(View.GONE);
                }
            }
        });

        mZipcodeEt.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                if (hasFocus){
                    mZipcodeHelperTv.setVisibility(View.VISIBLE);
                    mZipcodeHelperTv.setTextColor(mHelperColor);
                    mZipcodeHelperTv.setText(getString(R.string.zipcode_helper));
                } else {
                    mZipcodeHelperTv.setVisibility(View.GONE);
                }
            }
        });

        mGenderSpinnner.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                mGenderErrorTv.setVisibility(View.GONE);
                clearFocus();
                return false;
            }
        });

        mSexualitySpinner.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                mSexualityErrorTv.setVisibility(View.GONE);
                clearFocus();
                return false;
            }
        });

        mRelationshipStatusSpinner.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                mRelationshipErrorTv.setVisibility(View.GONE);
                clearFocus();
                return false;
            }
        });
    }

    @Override
    public Loader<String> onCreateLoader(int i, Bundle bundle) {
        return new UserDataUtils.CityLoader(this, mZipcode);
    }

    @Override
    public void onLoadFinished(Loader<String> loader, String s) {
        mValidZipcode = (s!= null);
        if (!mValidZipcode){
            showDialog(mEntryErrorTitle, getString(R.string.invalid_zipcode));
            mZipcodeEt.requestFocus();
            mZipcodeHelperTv.setTextColor(mErrorColor);
            mZipcodeHelperTv.setText(mRequired);
        }
    }

    @Override
    public void onLoaderReset(Loader<String> loader) {
    }

    private void handleAction(int actionId){
        switch (actionId) {
            case ACTION_RETURN_TO_DASH:
                Intent dashboardActivity = new Intent(EditActivity.this, DashboardActivity.class);
                dashboardActivity.putExtra(USER_KEY, mUser);
                startActivity(dashboardActivity);
                break;
            case ACTION_SIGN_OUT:
                FirebaseAuth.getInstance().signOut();
                Intent loginActivity = new Intent(EditActivity.this, LoginActivity.class);
                startActivity(loginActivity);
                break;
            case ACTION_TO_MESSAGES:
                Intent messagingActivityIntent = new Intent(this, MessagingActivity.class);
                startActivity(messagingActivityIntent);
                break;
            default:
                break;
        }
    }

    private void unsavedEditsPrompt(final int actionId){
        if (!mName.equals(mNameEt.getText().toString()) ||
                !mZipcode.equals(mZipcodeEt.getText().toString()) ||
                mGenderSpinnner.getSelectedItemId() != mUser.getGenderCode() ||
                !mSexualitySpinner.getSelectedItem().equals(mUser.getSexuality()) ||
                !mRelationshipStatusSpinner.getSelectedItem().equals(mUser.getRelationshipStatus()) ||
                !mImageUriString.equals(mUser.getProfilePicUri())) {
            AlertDialog.Builder dialog = new AlertDialog.Builder(this);
            dialog.setTitle(getString(R.string.unsaved_edits_title))
                    .setMessage(getString(R.string.unsaved_edits_prompt))
                    .setPositiveButton(getString(R.string.positive_btn), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            handleAction(actionId);
                        }
                    })
                    .setNegativeButton(getString(R.string.negative_btn), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
            dialog.create().show();
        } else {
            handleAction(actionId);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.editor, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu (Menu menu) {
        if (mFirstEdit) {
            menu.removeGroup(0);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home){
            unsavedEditsPrompt(ACTION_RETURN_TO_DASH);
            return true;
        }
        if (id == R.id.action_sign_out){
            unsavedEditsPrompt(ACTION_SIGN_OUT);
            return true;
        }
        if (id == R.id.action_messages){
            unsavedEditsPrompt(ACTION_TO_MESSAGES);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}