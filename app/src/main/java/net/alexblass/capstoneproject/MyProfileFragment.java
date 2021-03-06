package net.alexblass.capstoneproject;


import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.LoaderManager;
import android.content.Intent;
import android.support.v4.content.Loader;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.CardView;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
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
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import net.alexblass.capstoneproject.utils.UserDataUtils;
import net.alexblass.capstoneproject.models.User;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static android.app.Activity.RESULT_OK;
import static net.alexblass.capstoneproject.data.Constants.MY_PROFILE_FRAG_INDEX;
import static net.alexblass.capstoneproject.data.Keys.DASH_PG_NUM_KEY;
import static net.alexblass.capstoneproject.data.Keys.USER_BANNER_IMG_KEY;
import static net.alexblass.capstoneproject.data.Keys.USER_BIRTHDAY_KEY;
import static net.alexblass.capstoneproject.data.Keys.USER_DESCRIPTION_KEY;
import static net.alexblass.capstoneproject.data.Keys.USER_GENDER_KEY;
import static net.alexblass.capstoneproject.data.Keys.USER_KEY;
import static net.alexblass.capstoneproject.data.Keys.USER_NAME_KEY;
import static net.alexblass.capstoneproject.data.Keys.USER_PROFILE_IMG_KEY;
import static net.alexblass.capstoneproject.data.Keys.USER_RELATIONSHIP_KEY;
import static net.alexblass.capstoneproject.data.Keys.USER_SEXUALITY_KEY;
import static net.alexblass.capstoneproject.data.Keys.USER_ZIPCODE_KEY;

/**
 * A Fragment to display the current user's profile.
 */
public class MyProfileFragment extends Fragment implements LoaderManager.LoaderCallbacks<String> {

    private static final int SELECT_PICTURE = 100;

    @BindView(R.id.user_profile_image) ImageView mProfilePic;
    @BindView(R.id.user_profile_banner) ImageView mBannerIv;
    @BindView(R.id.user_profile_name) TextView mNameTv;
    @BindView(R.id.user_profile_stats) TextView mStats;
    @BindView(R.id.user_profile_description_tv) TextView mDescriptionTv;
    @BindView(R.id.user_profile_description_et) EditText mEditDescription;
    @BindView(R.id.user_profile_edit_description_btn) ImageButton mEditDescriptionBtn;
    @BindView(R.id.user_profile_sexuality) TextView mSexuality;
    @BindView(R.id.user_profile_sexuality_spinner) Spinner mSexualitySpinner;
    @BindView(R.id.user_profile_edit_sexuality_btn) ImageButton mEditSexualityBtn;
    @BindView(R.id.user_profile_relationship_status) TextView mRelationshipStatus;
    @BindView(R.id.user_profile_relationship_spinner) Spinner mRelationshipSpinner;
    @BindView(R.id.user_profile_edit_relationship_btn) ImageButton mEditRelationshipBtn;
    @BindView(R.id.user_profile_no_connection_tv) TextView mConnectivityTv;
    @BindView(R.id.user_profile_progressbar) ProgressBar mProgress;

    private User mUser;
    private String mZipcode;
    private String mLocation;
    private int mAge;
    private String mGender;
    private View mParent;

    private FirebaseAuth mAuth;
    private StorageReference mStorageRef;
    private StorageReference mUserBannerPic;
    private String mImageUriString;

    public MyProfileFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_my_profile, container, false);
        ButterKnife.bind(this, root);

        if (getContext().getResources().getBoolean(R.bool.isTablet)){
            mParent = (CardView) root.findViewById(R.id.user_profile_parent);
            mParent.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View view, MotionEvent motionEvent) {
                    clearFocus();
                    return false;
                }
            });
        } else {
            mParent = (ConstraintLayout) root.findViewById(R.id.user_profile_parent);
            mParent.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View view, MotionEvent motionEvent) {
                    clearFocus();
                    return false;
                }
            });
        }

        loadFragment();

        return root;
    }

    private void loadFragment() {
        if (!UserDataUtils.checkNetworkConnectivity(getContext())) {
            mProgress.setVisibility(View.GONE);
            mConnectivityTv.setVisibility(View.VISIBLE);
            return;
        } else {
            mConnectivityTv.setVisibility(View.GONE);
        }

        mAuth = FirebaseAuth.getInstance();
        mStorageRef = FirebaseStorage.getInstance().getReference();
        mEditDescriptionBtn.setTag(R.drawable.ic_edit_white_24dp);
        mEditSexualityBtn.setTag(R.drawable.ic_edit_white_24dp);
        mEditRelationshipBtn.setTag(R.drawable.ic_edit_white_24dp);
        mImageUriString = "";

        mBannerIv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent, "Select Picture"), SELECT_PICTURE);
            }
        });

        Intent intentThatStartedThis = getActivity().getIntent();
        if (intentThatStartedThis != null && intentThatStartedThis.hasExtra(USER_KEY)){

            mUser = intentThatStartedThis.getParcelableExtra(USER_KEY);
            populateData();

        } else {
            Query query = FirebaseDatabase.getInstance().getReference().child(
                    mAuth.getCurrentUser().getEmail().replace(".", "(dot)"));
            query.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        String name, zipcode, description, sexuality, relationshipStatus, email, profilePicUri, bannerPicUri;
                        long gender;

                        name = (String) dataSnapshot.child(USER_NAME_KEY).getValue();

                        long birthdayInMillis = (long) dataSnapshot.child(USER_BIRTHDAY_KEY).getValue();

                        zipcode = (String) dataSnapshot.child(USER_ZIPCODE_KEY).getValue();
                        description = (String) dataSnapshot.child(USER_DESCRIPTION_KEY).getValue();

                        gender = (long) dataSnapshot.child(USER_GENDER_KEY).getValue();
                        sexuality = (String) dataSnapshot.child(USER_SEXUALITY_KEY).getValue();
                        relationshipStatus = (String) dataSnapshot.child(USER_RELATIONSHIP_KEY).getValue();
                        profilePicUri = (String) dataSnapshot.child(USER_PROFILE_IMG_KEY).getValue();
                        bannerPicUri = (String) dataSnapshot.child(USER_BANNER_IMG_KEY).getValue();

                        email = mAuth.getCurrentUser().getEmail();

                        mUser = new User(email, name, birthdayInMillis, zipcode, gender,
                                sexuality, relationshipStatus, description, profilePicUri, bannerPicUri);
                        populateData();


                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                }
            });
        }
    }

    private void populateData(){
        mNameTv.setText(mUser.getName());
        mDescriptionTv.setText(mUser.getDescription());
        mSexuality.setText(mUser.getSexuality());
        mRelationshipStatus.setText(mUser.getRelationshipStatus());

        Calendar birthday = new GregorianCalendar();
        birthday.setTimeInMillis(mUser.getBirthday());
        mAge = UserDataUtils.calculateAge(birthday);

        int genderStringId = UserDataUtils.getGenderAbbreviationStringId(mUser.getGenderCode());
        mGender = getActivity().getString(genderStringId);

        mZipcode = mUser.getZipcode();

        if (!mUser.getProfilePicUri().isEmpty()){
            StorageReference profilePicFile = FirebaseStorage.getInstance().getReference()
                    .child(Uri.parse(mUser.getProfilePicUri()).getPath());
            try {
                final File localFile = File.createTempFile("images", "jpg");
                profilePicFile.getFile(localFile)
                        .addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                            @Override
                            public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                                Picasso.with(getContext())
                                        .load(localFile)
                                        .placeholder(R.drawable.ic_person_white_48dp)
                                        .centerCrop()
                                        .fit()
                                        .into(mProfilePic);
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        exception.printStackTrace();
                    }
                });
            } catch (IOException e){
                e.printStackTrace();
            }
        }

        if (!mUser.getBannerPicUri().isEmpty()) {
            StorageReference bannerPicFile = FirebaseStorage.getInstance().getReference()
                    .child(Uri.parse(mUser.getBannerPicUri()).getPath());
            try {
                final File localFile = File.createTempFile("images", "jpg");
                bannerPicFile.getFile(localFile)
                        .addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                            @Override
                            public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                                Picasso.with(getContext())
                                        .load(localFile)
                                        .centerCrop()
                                        .fit()
                                        .into(mBannerIv);
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        exception.printStackTrace();
                    }
                });
            } catch (IOException e){
                e.printStackTrace();
            }
        }

        mProgress.setVisibility(View.GONE);
        mParent.setVisibility(View.VISIBLE);
        mBannerIv.setVisibility(View.VISIBLE);
        mProfilePic.setVisibility(View.VISIBLE);

        LoaderManager loaderManager = getLoaderManager();
        loaderManager.initLoader(0, null, this);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == SELECT_PICTURE) {
                Uri selectedImageUri = data.getData();
                if (null != selectedImageUri) {
                    mImageUriString = selectedImageUri.toString();
                    Picasso.with(getContext())
                            .load(selectedImageUri)
                            .centerCrop()
                            .fit()
                            .into(mBannerIv);
                }
                uploadImage();
            }
        }
    }

    private void uploadImage(){
        mUserBannerPic = mStorageRef.child(Uri.parse(mImageUriString).getPath());
        mUserBannerPic.putFile(Uri.parse(mImageUriString))
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
                        Toast.makeText(getContext(), getString(R.string.upload_error), Toast.LENGTH_SHORT).show();
                    }
                });

        DatabaseReference database = FirebaseDatabase.getInstance().getReference(mUser.getEmail()
                .replace(".", "(dot)")).child(USER_BANNER_IMG_KEY);
        database.setValue(mImageUriString);
    }

    @OnClick(R.id.user_profile_edit_description_btn)
    public void editMyDescription(){
        if (mEditDescriptionBtn.getTag().equals(R.drawable.ic_edit_white_24dp)) {
            clearFocus();
            mEditDescriptionBtn.setColorFilter(getResources().getColor(R.color.confirm_green));
            Picasso.with(getContext())
                    .load(R.drawable.ic_check_white_24dp)
                    .placeholder(R.drawable.ic_check_white_24dp)
                    .fit()
                    .into(mEditDescriptionBtn);
            mEditDescription.setText(mDescriptionTv.getText().toString());
            mDescriptionTv.setVisibility(View.GONE);
            mEditDescription.setVisibility(View.VISIBLE);
            mEditDescriptionBtn.setTag(R.drawable.ic_check_white_24dp);
        } else {
            String description = mEditDescription.getText().toString();
            mDescriptionTv.setText(description);

            clearFocus();

            DatabaseReference database = FirebaseDatabase.getInstance().getReference(
                    mUser.getEmail().replace(".", "(dot)"))
                    .child(USER_DESCRIPTION_KEY);
            database.setValue(description);

            Toast.makeText(getContext(), getString(R.string.change_saved), Toast.LENGTH_SHORT).show();
        }
    }

    @OnClick(R.id.user_profile_edit_sexuality_btn)
    public void editMySexuality(){
        if (mEditSexualityBtn.getTag().equals(R.drawable.ic_edit_white_24dp)) {
            clearFocus();
            mEditSexualityBtn.setColorFilter(getResources().getColor(R.color.confirm_green));
            Picasso.with(getContext())
                    .load(R.drawable.ic_check_white_24dp)
                    .placeholder(R.drawable.ic_check_white_24dp)
                    .fit()
                    .into(mEditSexualityBtn);

            List<String> sexualitiesList = new ArrayList<>(Arrays.asList(getResources().getStringArray(R.array.sexuality_choices)));
            mSexualitySpinner.setAdapter(getArrayAdapter(sexualitiesList));
            mSexualitySpinner.setSelection(sexualitiesList.indexOf(mUser.getSexuality()));

            mSexuality.setVisibility(View.GONE);
            mSexualitySpinner.setVisibility(View.VISIBLE);

            mEditSexualityBtn.setTag(R.drawable.ic_check_white_24dp);
        } else {
            String sexuality = mSexualitySpinner.getSelectedItem().toString();
            mSexuality.setText(sexuality);

            clearFocus();

            DatabaseReference database = FirebaseDatabase.getInstance().getReference(
                    mUser.getEmail().replace(".", "(dot)"))
                    .child(USER_SEXUALITY_KEY);
            database.setValue(sexuality);

            Toast.makeText(getContext(), getString(R.string.change_saved), Toast.LENGTH_SHORT).show();
        }
    }

    @OnClick(R.id.user_profile_edit_relationship_btn)
    public void editMyRelationship(){
        if (mEditRelationshipBtn.getTag().equals(R.drawable.ic_edit_white_24dp)) {
            clearFocus();
            mEditRelationshipBtn.setColorFilter(getResources().getColor(R.color.confirm_green));
            Picasso.with(getContext())
                    .load(R.drawable.ic_check_white_24dp)
                    .placeholder(R.drawable.ic_check_white_24dp)
                    .fit()
                    .into(mEditRelationshipBtn);

            List<String> relationshipsList = new ArrayList<>(Arrays.asList(getResources().getStringArray(R.array.relationship_choices)));
            mRelationshipSpinner.setAdapter(getArrayAdapter(relationshipsList));
            mRelationshipSpinner.setSelection(relationshipsList.indexOf(mUser.getRelationshipStatus()));

            mRelationshipStatus.setVisibility(View.GONE);
            mRelationshipSpinner.setVisibility(View.VISIBLE);

            mEditRelationshipBtn.setTag(R.drawable.ic_check_white_24dp);
        } else {
            String relationship = mRelationshipSpinner.getSelectedItem().toString();
            mRelationshipStatus.setText(relationship);

            clearFocus();

            DatabaseReference database = FirebaseDatabase.getInstance().getReference(
                    mUser.getEmail().replace(".", "(dot)"))
                    .child(USER_RELATIONSHIP_KEY);
            database.setValue(relationship);

            Toast.makeText(getContext(), getString(R.string.change_saved), Toast.LENGTH_SHORT).show();
        }
    }

    private void clearFocus(){
        mEditDescriptionBtn.setColorFilter(getResources().getColor(R.color.primary_light));
        Picasso.with(getContext())
                .load(R.drawable.ic_edit_white_24dp)
                .placeholder(R.drawable.ic_edit_white_24dp)
                .fit()
                .into(mEditDescriptionBtn);
        mEditDescription.setVisibility(View.GONE);
        mDescriptionTv.setVisibility(View.VISIBLE);
        mEditDescriptionBtn.setTag(R.drawable.ic_edit_white_24dp);

        mEditSexualityBtn.setColorFilter(getResources().getColor(R.color.primary_light));
        Picasso.with(getContext())
                .load(R.drawable.ic_edit_white_24dp)
                .placeholder(R.drawable.ic_edit_white_24dp)
                .fit()
                .into(mEditSexualityBtn);
        mSexualitySpinner.setVisibility(View.GONE);
        mSexuality.setVisibility(View.VISIBLE);
        mEditSexualityBtn.setTag(R.drawable.ic_edit_white_24dp);

        mEditRelationshipBtn.setColorFilter(getResources().getColor(R.color.primary_light));
        Picasso.with(getContext())
                .load(R.drawable.ic_edit_white_24dp)
                .placeholder(R.drawable.ic_edit_white_24dp)
                .fit()
                .into(mEditRelationshipBtn);
        mRelationshipSpinner.setVisibility(View.GONE);
        mRelationshipStatus.setVisibility(View.VISIBLE);
        mEditRelationshipBtn.setTag(R.drawable.ic_edit_white_24dp);
    }

    private ArrayAdapter<String> getArrayAdapter(List<String> list){

        final ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<String>(
                getContext(), R.layout.item_edit_profile_spinner, list){
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

    @Override
    public Loader<String> onCreateLoader(int i, Bundle bundle) {
        return new UserDataUtils.CityLoader(getActivity(), mZipcode);
    }

    @Override
    public void onLoadFinished(Loader<String> loader, String s) {
        if (s != null) {
            mLocation = s;
        } else {
            mLocation = mUser.getZipcode();
        }

        mStats.setText(getActivity().getResources().getString(R.string.stats_format,
                mAge, mGender, mLocation));
    }

    @Override
    public void onLoaderReset(Loader<String> loader) {
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(DASH_PG_NUM_KEY, MY_PROFILE_FRAG_INDEX);
    }

    @Override
    public void onResume() {
        super.onResume();
        loadFragment();
    }
}
