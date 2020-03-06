package com.telran.photogallery;


import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 * A simple {@link Fragment} subclass.
 */
public class PhotoGalleryFragment extends Fragment{
    public static final String TAG = "photogalleryfragment";
    private ThumbnailDownloader<PhotoHolder> thumbnailDownloader;
    private RecyclerView photoResView;
    private List<GalleryItem> photoItems = new ArrayList<>();
    private ProgressBar progressBar;
    private int pageNum = 1;

    public PhotoGalleryFragment() {
        // Required empty public constructor
    }

    public static PhotoGalleryFragment newInstance(){
        return new PhotoGalleryFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        new FetchItemTask().execute();

        Handler responseHandler = new Handler();
        thumbnailDownloader = new ThumbnailDownloader<>(responseHandler);
        thumbnailDownloader.setListener((target, thumbnail) -> {
            Drawable drawable = new BitmapDrawable(getResources(),thumbnail);
            target.bindDrawable(drawable);
        });
        thumbnailDownloader.start();
        thumbnailDownloader.getLooper();

        Log.i(TAG, "Background thread started");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_photo_gallery, container, false);
        photoResView = view.findViewById(R.id.photo_res_view);
        progressBar = view.findViewById(R.id.progress);

        GridLayoutManager manager = new GridLayoutManager(requireContext(),3);
        photoResView.setLayoutManager(manager);

        photoResView.addOnScrollListener(new RecyclerView.OnScrollListener() {

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                int total = manager.getItemCount();
                int firstVisCt = manager.findFirstVisibleItemPosition();
                int lastVisCt = manager.findLastVisibleItemPosition();
                if (total > 0){
                    if (lastVisCt == total-1){
                    new FetchItemTask().execute();
                    }
                }

            }
        });
        return view;
    }

    private void setupAdapter(){
        if (isAdded()){
            photoResView.setAdapter(new PhotoAdapter(photoItems));
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        thumbnailDownloader.clearQueue();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        thumbnailDownloader.quit();

        Log.i(TAG, "Background thread destroyed");
    }

    private class PhotoAdapter extends RecyclerView.Adapter<PhotoHolder>{
        private List<GalleryItem> items;

        public PhotoAdapter(List<GalleryItem> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public PhotoHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(getActivity());
            View v = inflater.inflate(R.layout.gallery_item,parent,false);
            return new PhotoHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull PhotoHolder holder, int position) {
            GalleryItem item = items.get(position);
            Drawable placeholder = getResources().getDrawable(R.drawable.bill_up_close);
            holder.bindDrawable(placeholder);
            thumbnailDownloader.queueThumbnail(holder,item.getUrl());
        }

        @Override
        public int getItemCount() {
            return items.size();
        }
    }

    private class PhotoHolder extends RecyclerView.ViewHolder {
        //   private TextView titleTxt;
        private ImageView imgView;

        public PhotoHolder(@NonNull View itemView) {
            super(itemView);
            imgView = itemView.findViewById(R.id.imgView);
        }

//        public void bindGalleryItem(GalleryItem item){
//            titleTxt.setText(item.toString());
//        }

        public void bindDrawable(Drawable drawable){
            imgView.setImageDrawable(drawable);
        }

    }
    private class FetchItemTask extends AsyncTask<Void, Void,List<GalleryItem>>{
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            if (progressBar != null){
                progressBar.setVisibility(View.VISIBLE);
            }
        }

        @Override
        protected List<GalleryItem> doInBackground(Void... voids) {
            return new FlickrFetchr().fetchItems(pageNum++);
        }

        @Override
        protected void onPostExecute(List<GalleryItem> galleryItems) {
            super.onPostExecute(galleryItems);
            photoItems.addAll(galleryItems);
            setupAdapter();
            if (progressBar.getId() != 0){
                progressBar.setVisibility(View.GONE);
            }
        }
    }


}
