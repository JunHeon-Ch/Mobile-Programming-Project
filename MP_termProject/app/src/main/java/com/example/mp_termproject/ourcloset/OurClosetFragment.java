package com.example.mp_termproject.ourcloset;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.mp_termproject.R;
import com.example.mp_termproject.mycloset.ImageDTO;
import com.example.mp_termproject.ourcloset.filter.OurClosetFilterActivity;
import com.example.mp_termproject.signup.UserInfo;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;


public class OurClosetFragment extends Fragment {

    private static final String TAG = "OurClosetFragment";

    static final int REQUEST_FILTER = 1;
    static final int NORMAL = 1;
    static final int SEARCH = 2;
    static final int FILTER = 3;
    static final int SORTING = 4;

    static int check = NORMAL;
    static int totalNum = 0;
    static int checkNum = 0;

    EditText searchText;
    ImageView searchImage;
    LinearLayout imageContainer;

    FirebaseUser user;
    FirebaseFirestore db;
    DocumentReference docRefUserInfo;
    FirebaseStorage storage;
    StorageReference storageRef;

    ArrayList<ImageDTO> dtoList;
    ArrayList<StorageReference> imageList;
    HashSet<ImageDTO> filterList;
    ArrayList<String> imgUrl;
    ArrayList<UserInfo> userInfoList;

    Double[] imgnum;

    Double[] myLoc;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle("OUR CLOSET");
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.fragment_our_closet,
                container,
                false);

        setHasOptionsMenu(true);
        user = FirebaseAuth.getInstance().getCurrentUser();
        db = FirebaseFirestore.getInstance();
        docRefUserInfo = db.collection("users").document(user.getUid());
        storage = FirebaseStorage.getInstance();
        storageRef = storage.getReference();

        dtoList = new ArrayList<>();
        imageList = new ArrayList<>();
        filterList = new HashSet<>();
        imgUrl = new ArrayList<>();
        userInfoList = new ArrayList<>();

        imgnum = new Double[1];
        myLoc = new Double[2];

        imageContainer = rootView.findViewById(R.id.closet_image_container);

        searchText = rootView.findViewById(R.id.search);
        searchImage = rootView.findViewById(R.id.search_image);
        searchImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!searchText.getText().toString().equals("")) {
//                  edit text에 있는 string값과 같은 상품명을 확인해서 보여줌
                    check = SEARCH;
                }
                onStart();
            }
        });

        return rootView;
    }

    // Action Bar에 메뉴옵션 띄우기
    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.actionbar_ourcloset, menu);
    }

    // Action Bar 메뉴옵션 선택 시
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int curId = item.getItemId();
        Intent intent;

        switch (curId) {
            case R.id.actionbar_filter:
//                필터 옵션 메뉴 선택
//                필터 선택 후 My Closet 화면에 조건에 맞는 아이템을 보여줌

                intent = new Intent(getContext(), OurClosetFilterActivity.class);
                startActivityForResult(intent, REQUEST_FILTER);
                break;

            case R.id.actionbar_sorting:

                sortByDistance();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onStart() {
        super.onStart();
        imgUrl.clear();
        dtoList.clear();

        totalUser();
        accessDBInfo();
    }

    private void totalUser() {

        db.collection("users").get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                totalNum++;
                            }
                        } else {
                            Log.d(TAG, "Error getting documents: ", task.getException());
                        }
                    }
                });

    }

    private void accessDBInfo() {
        if (check == NORMAL) {
            // 유저 정보접근
            db.collection("users").get()
                    .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            if (task.isSuccessful()) {
                                userInfoList.clear();

                                for (QueryDocumentSnapshot document : task.getResult()) {
                                    checkNum++;
                                    accessUserInfoDB(document.getId());
                                    if (!user.getUid().equals(document.getId())) {
                                        accessImageInfoDB(document.getId());
                                    }
                                }
                            } else {
                                Log.d(TAG, "Error getting documents: ", task.getException());
                            }
                        }
                    });
        } else {
            int count = addPathReference(check);
            floatTotalImages(count);
        }
    }

    private void accessUserInfoDB(final String document_id) {
        db
                .collection("users")
                .document(document_id)
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            DocumentSnapshot result = task.getResult();
                            Map<String, Object> data = result.getData();

                            if (user.getUid().equals(document_id)) {
                                myLoc[0] = (Double) data.get("latitude");
                                myLoc[1] = (Double) data.get("longitude");
                            } else {
                                String address = (String) data.get("address");
                                Double latitude = (Double) data.get("latitude");
                                Double longitude = (Double) data.get("longitude");
                                String name = (String) data.get("name");
                                String phoneNumber = (String) data.get("phoneNumber");

                                UserInfo userInfo = new UserInfo(user.getUid(), name, phoneNumber,
                                        address, latitude, longitude);

                                userInfoList.add(userInfo);
                            }
                        }
                    }
                });
    }

    private void accessImageInfoDB(String document_id) {
        db
                .collection("images")
                .document(document_id)
                .collection("image")
                .whereEqualTo("shared", "공유")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {

                            for (QueryDocumentSnapshot document : task.getResult()) {
                                String tempUrl = document.getString("imgURL");

                                Map<String, Object> temp = document.getData();

                                String id = (String) temp.get("userID");
                                String url = (String) temp.get("imgURL");
                                String category = (String) temp.get("category");
                                String name = (String) temp.get("itemName");
                                String color = (String) temp.get("color");
                                String brand = (String) temp.get("brand");
                                String season = (String) temp.get("season");
                                String size = (String) temp.get("size");
                                String shared = (String) temp.get("shared");
                                ImageDTO dto = new ImageDTO(id, url, category, name,
                                        color, brand, season, size, shared);

                                dtoList.add(dto);
                                imgUrl.add(tempUrl);

                            }
                            if (totalNum == checkNum) {
                                int count = addPathReference(check);
                                floatTotalImages(count);
                            }
                        } else {
                            Log.d(TAG, "Error getting documents: ", task.getException());
                        }
                    }
                });
    }

    private void sortByDistance() {
        ArrayList<Double> dist = new ArrayList<>();
        Log.d("test", "");
        for (int i = 0; i < userInfoList.size(); i++) {
            if (!user.getUid().equals(userInfoList.get(i).getUserId())) {
                dist.add(distance(myLoc[0], myLoc[1],
                        userInfoList.get(i).getLatitude(), userInfoList.get(i).getLongitude()));
                Log.d("test", dist.get(i) + "");
            }
        }
        for (int i = 0; i < dist.size(); i++) {
            Log.d("before test", dist.get(i) + "");
        }
        heap(dist);

        for (int i = 0; i < dist.size(); i++) {
            Toast.makeText(getContext(), dist.get(i) +" ", Toast.LENGTH_SHORT).show();
        }
    }

    public void heap(ArrayList<Double> data) {
        for (int i = 1; i < data.size(); i++) {
            int child = i;
            while (child > 0) {
                int parent = (child - 1) / 2;
                if (data.get(child) < data.get(parent)) {
                    Collections.swap(data, child, parent);
                }
                child = parent;
            }
        }
    }

    private double distance(double lat1, double lon1, double lat2, double lon2) {

        double theta = lon1 - lon2;
        double dist = Math.sin(deg2rad(lat1)) * Math.sin(deg2rad(lat2))
                + Math.cos(deg2rad(lat1)) * Math.cos(deg2rad(lat2)) * Math.cos(deg2rad(theta));

        dist = Math.acos(dist);
        dist = rad2deg(dist);
        dist = dist * 60 * 1.1515;
        dist = dist * 1.609344;

        return (dist);
    }


    // This function converts decimal degrees to radians
    private double deg2rad(double deg) {
        return (deg * Math.PI / 180.0);
    }

    // This function converts radians to decimal degrees
    private double rad2deg(double rad) {
        return (rad * 180 / Math.PI);
    }

    private int addPathReference(int flag) {
        imageList.clear();
        int count = 0;

        switch (flag) {
            case NORMAL:

                for (int i = 0; i < imgUrl.size(); i++) {
                    count++;
                    imageList.add(storageRef.child(imgUrl.get(i)));
                }

                break;

            case SEARCH:
                String sText = searchText.getText().toString();
                for (int i = 0; i < dtoList.size(); i++) {
                    if (sText.equals(dtoList.get(i).getBrand()) ||
                            sText.equals(dtoList.get(i).getItemName())) {
                        count++;
                        imageList.add(storageRef.child(dtoList.get(i).getImgURL()));
                    }
                }

                break;

            case FILTER:
                for (ImageDTO dto : filterList) {
                    count++;
                    imageList.add(storageRef.child(dto.getImgURL()));
                }

                break;
        }

        return count;
    }

    private void floatTotalImages(int count) {
        LinearLayout linearLayout = null;
        imageContainer.removeAllViews();

        final int height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                180, getResources().getDisplayMetrics());

        int i = 0;
        while (i < count) {
            StorageReference pathReference = imageList.get(i);

            if (i % 3 == 0) {
                LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, height);

                linearLayout = new LinearLayout(imageContainer.getContext());
                linearLayout.setOrientation(LinearLayout.HORIZONTAL);
                linearLayout.setLayoutParams(layoutParams);

                imageContainer.addView(linearLayout);
            }

            LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            imageParams.setMargins(5, 5, 5, 5);
            imageParams.weight = 1;
            imageParams.gravity = Gravity.LEFT;

            ImageView imageView = new ImageView(linearLayout.getContext());
            imageView.setLayoutParams(imageParams);

            Glide.with(linearLayout)
                    .load(pathReference)
                    .into(imageView);
            linearLayout.addView(imageView);

            i++;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_FILTER) {
            if (resultCode == -1) {
                Bundle bundle = data.getExtras();

                ArrayList<String> categoryItemList = bundle.getStringArrayList("category");
                ArrayList<String> colorItemList = bundle.getStringArrayList("color");
                ArrayList<String> seasonItemList = bundle.getStringArrayList("season");

                filterList.clear();
                filterList.addAll(filterCategory(dtoList, categoryItemList));
                filterList.addAll(filterColor(filterList, colorItemList));
                filterList.addAll(filterSeason(filterList, seasonItemList));

                if (filterList.size() == 0) {
                    check = NORMAL;
                    Toast.makeText(getContext(), "해당하는 값이 없습니다.", Toast.LENGTH_SHORT).show();
                } else {
                    check = FILTER;
                }

                Toast.makeText(getContext(),
                        categoryItemList.toString() + "\n"
                                + colorItemList.toString() + "\n"
                                + seasonItemList.toString() + "\n",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    private ArrayList<ImageDTO> filterCategory(ArrayList<ImageDTO> list, ArrayList<String> arrayList) {
        ArrayList<ImageDTO> temp = new ArrayList<>();

        if (arrayList.size() == 0) {
            return list;
        } else {
            for (int i = 0; i < list.size(); i++) {
                for (int j = 0; j < arrayList.size(); j++) {
                    if (list.get(i).getCategory().equals(arrayList.get(j))) {
                        temp.add(list.get((i)));
                        break;
                    }
                }
            }
        }

        return temp;
    }

    private HashSet<ImageDTO> filterColor(HashSet<ImageDTO> list, ArrayList<String> arrayList) {
        HashSet<ImageDTO> temp = new HashSet<>();

        if (arrayList.size() == 0) {
            return list;
        } else {
            for (ImageDTO dto : list) {
                String[] tempColor = dto.getColor().split(" ");
                for (int k = 0; k < tempColor.length; k++) {
                    int flag = 0;
                    for (int j = 0; j < arrayList.size(); j++) {
                        if (tempColor[k].equals(arrayList.get(j))) {
                            temp.add(dto);
                            flag = 1;
                            break;
                        }
                    }

                    if (flag == 1) {
                        break;
                    }
                }
            }
        }

        return temp;
    }

    private HashSet<ImageDTO> filterSeason(HashSet<ImageDTO> list, ArrayList<String> arrayList) {
        HashSet<ImageDTO> temp = new HashSet<>();

        if (arrayList.size() == 0) {
            return list;
        } else {
            for (ImageDTO dto : list) {
                String[] temSeason = dto.getColor().split(" ");
                for (int k = 0; k < temSeason.length; k++) {
                    int flag = 0;
                    for (int j = 0; j < arrayList.size(); j++) {
                        if (temSeason[k].equals(arrayList.get(j))) {
                            temp.add(dto);
                            flag = 1;
                            break;
                        }
                    }

                    if (flag == 1) {
                        break;
                    }
                }
            }
        }

        return temp;
    }
}
