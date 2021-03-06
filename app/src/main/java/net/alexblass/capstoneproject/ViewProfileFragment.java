package net.alexblass.capstoneproject;


import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;

import net.alexblass.capstoneproject.models.User;
import net.alexblass.capstoneproject.utils.UserDataUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;

import butterknife.BindView;
import butterknife.ButterKnife;

import static net.alexblass.capstoneproject.data.Constants.CONNECT_FRAG_INDEX;
import static net.alexblass.capstoneproject.data.Keys.DASH_PG_NUM_KEY;
import static net.alexblass.capstoneproject.data.Keys.USER_EMAIL_KEY;
import static net.alexblass.capstoneproject.data.Keys.USER_FAVORITES_KEY;
import static net.alexblass.capstoneproject.data.Keys.USER_KEY;

/**
 * A Fragment to display a user's profile.
 */
public class ViewProfileFragment extends Fragment implements LoaderManager.LoaderCallbacks<String> {

    @BindView(R.id.view_profile_image) ImageView mProfilePic;
    @BindView(R.id.view_profile_banner) ImageView mBannerIv;
    @BindView(R.id.view_profile_name) TextView mNameTv;
    @BindView(R.id.view_profile_stats) TextView mStats;
    @BindView(R.id.view_profile_description_tv) TextView mDescriptionTv;
    @BindView(R.id.view_profile_sexuality) TextView mSexuality;
    @BindView(R.id.view_profile_relationship_status) TextView mRelationshipStatus;
    @BindView(R.id.view_profile_message_btn) ImageButton mSendMessageBtn;
    @BindView(R.id.view_profile_favorite_btn) ImageButton mFavoriteUserBtn;
    @BindView(R.id.view_profile_no_connection_tv) TextView mConnectivityTv;
    @BindView(R.id.view_profile_progressbar) ProgressBar mProgress;
    @BindView(R.id.view_profile_scrollview) ScrollView mScroll;

    private User mUser;
    private String mZipcode;
    private String mLocation;
    private int mAge;
    private String mGender;
    private ArrayList<String> mFavorites;

    public ViewProfileFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_view_profile, container, false);
        ButterKnife.bind(this, root);

        loadFragment();
        return root;
    }

    private void loadFragment(){
        if (!UserDataUtils.checkNetworkConnectivity(getContext())) {
            mProgress.setVisibility(View.GONE);
            mConnectivityTv.setVisibility(View.VISIBLE);
            return;
        } else {
            mConnectivityTv.setVisibility(View.GONE);
        }
        getFavorites();

        Intent intentThatStartedThisActivity = getActivity().getIntent();
        if (intentThatStartedThisActivity.hasExtra(USER_KEY)) {
            mUser = intentThatStartedThisActivity.getParcelableExtra(USER_KEY);

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

            if (!mUser.getBannerPicUri().isEmpty()){
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
            mScroll.setVisibility(View.VISIBLE);

            mSendMessageBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent newMessageIntent = new Intent(getContext(), NewMessageActivity.class);
                    newMessageIntent.putExtra(USER_EMAIL_KEY, mUser.getEmail());
                    getContext().startActivity(newMessageIntent);
                }
            });

            mFavoriteUserBtn.setTag(R.drawable.ic_favorite_border_white_24dp);
            mFavoriteUserBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if ((Integer) mFavoriteUserBtn.getTag() == R.drawable.ic_favorite_border_white_24dp){
                        mFavoriteUserBtn.setImageResource(R.drawable.ic_favorite_white_24dp);
                        mFavoriteUserBtn.setTag(R.drawable.ic_favorite_white_24dp);
                        UserDataUtils.addFavorite(FirebaseAuth.getInstance().getCurrentUser().getEmail(),
                                mUser.getEmail());
                        Toast.makeText(getContext(), getString(R.string.add_favorite), Toast.LENGTH_SHORT).show();
                    } else {
                        mFavoriteUserBtn.setImageResource(R.drawable.ic_favorite_border_white_24dp);
                        mFavoriteUserBtn.setTag(R.drawable.ic_favorite_border_white_24dp);
                        UserDataUtils.removeFavorite(FirebaseAuth.getInstance().getCurrentUser().getEmail(),
                                mUser.getEmail());
                        Toast.makeText(getContext(), getString(R.string.remove_favorite), Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
        LoaderManager loaderManager = getLoaderManager();
        loaderManager.initLoader(0, null, this);
    }

    private void getFavorites(){
        final ArrayList<String> favorites = new ArrayList<>();

        final Query query = FirebaseDatabase.getInstance()
                .getReference(FirebaseAuth.getInstance().getCurrentUser().getEmail()
                        .replace(".", "(dot)"));
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    dataSnapshot = dataSnapshot.child(USER_FAVORITES_KEY);

                    for (DataSnapshot child : dataSnapshot.getChildren()){
                        favorites.add(child.getValue().toString());
                    }
                    query.removeEventListener(this);
                    mFavorites = favorites;
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        loadFragment();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putStringArrayList(USER_FAVORITES_KEY, mFavorites);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);

        if (savedInstanceState != null) {
            mFavorites = savedInstanceState.getStringArrayList(USER_FAVORITES_KEY);
        }
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
        if (!mFavorites.contains(mUser.getEmail())) {
            mFavoriteUserBtn.setImageResource(R.drawable.ic_favorite_border_white_24dp);
        } else {
            mFavoriteUserBtn.setImageResource(R.drawable.ic_favorite_white_24dp);
        }
    }

    @Override
    public void onLoaderReset(Loader<String> loader) {
    }
}
