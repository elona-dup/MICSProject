package uni.lu.mics.mics_project.nmbd.app.service;

import android.content.Context;
import android.util.Log;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.firebase.storage.StorageReference;

import uni.lu.mics.mics_project.nmbd.R;


//Check https://bumptech.github.io/glide/ to implement other functions

public class ImageViewUtils {

    static final private String TAG ="ImageViewUtils";

    public static void displayCirclePic(Context context, StorageReference strgRef, ImageView imgView){
        Glide.with(context)
                .load(strgRef)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .circleCrop()
                .into(imgView);
    }

    public static void displayCircleAvatarPic(final Context context, Storage storageService, final ImageView imgView){
        final String defaulPictUrl = context.getString(R.string.avatarPicUrl);
        final String defaultPicId = context.getString(R.string.avatarPicId);
        storageService.getStorageReference(defaulPictUrl,defaultPicId , new StorageCallback() {
            @Override
            public void onSuccess(StorageReference storageReference) {
                Log.d(TAG, "Using default avatar to display profile picture");
                ImageViewUtils.displayCirclePic(context, storageReference, imgView);
            }
            @Override
            public void onFailure() {
                Log.d(TAG, "No profile picture to display");

            }
        });
    }

    public static void displayPic(Context context, StorageReference strgRef, ImageView imgView){
        Glide.with(context)
                .load(strgRef)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .into(imgView);
    }

    public static void displayAvatarPic(final Context context, Storage storageService, final ImageView imgView){
        final String defaulPictUrl = context.getString(R.string.gsEventsPicsUrl);
        final String defaultPicId = context.getString(R.string.avatarPicId);
        storageService.getStorageReference(defaulPictUrl,defaultPicId , new StorageCallback() {
            @Override
            public void onSuccess(StorageReference storageReference) {
                Log.d(TAG, "Using default avatar to display profile picture");
                ImageViewUtils.displayPic(context, storageReference, imgView);
            }
            @Override
            public void onFailure() {
                Log.d(TAG, "No profile picture to display");

            }
        });
    }
}
