package com.nisaelek.servisimnerede;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.UUID;

public class UploadActivity extends AppCompatActivity {

    Bitmap selectedImage;
    ImageView imageView;
    EditText commentText;
    private FirebaseStorage firebaseStorage;
    private StorageReference storageReference;
    Uri imageData;
    private FirebaseFirestore firebaseFirestore;
    private FirebaseAuth firebaseAuth;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload);

        imageView = findViewById(R.id.imageView);
        commentText = findViewById(R.id.commentText);

        firebaseStorage = FirebaseStorage.getInstance();
        storageReference = firebaseStorage.getReference();
        firebaseFirestore = FirebaseFirestore.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();


    }

    public void upload(View view) {

        if (imageData != null) {


            final UUID uuid = UUID.randomUUID();
            //??mage namini tan??mlad??k
            final String imageName = "images/" + uuid + ".jpg";

            storageReference.child(imageName).putFile(imageData).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                    //Download URL
//imageName adl?? bir tablo olu??turduk
                    StorageReference newReference = FirebaseStorage.getInstance().getReference(imageName);

                    newReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri uri) {

                            String downloadUrl = uri.toString();

                            FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
//assert test etmek i??in kullan??l??r ve onay
                            assert firebaseUser != null;
                            String userEmail = firebaseUser.getEmail();
//A????klama sat??r??n?? tan??mlad??k
                            String comment = commentText.getText().toString();
                            Date date = new Date();
                            System.out.println(date.toString());

                            if (!userEmail.isEmpty() && !downloadUrl.isEmpty() && !comment.isEmpty()){
                                HashMap<String, Object> postData = new HashMap<>();
                                //Hashmap olu??turduk ve boylelikle birden ??ok alt sutun olu??turduk
                                postData.put("useremail", userEmail);
                                postData.put("downloadurl", downloadUrl);
                                postData.put("comment", comment);
                                postData.put("date", date);
                                FirebaseFirestore db = FirebaseFirestore.getInstance();
                                db.collection("Posts").document(""+uuid).set(postData).addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        Toast.makeText(UploadActivity.this, "Succes", Toast.LENGTH_SHORT).show();
                                        startActivity(new Intent(getApplicationContext(),AnaActivity.class));
                                    }
                                }).addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Toast.makeText(UploadActivity.this, e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }



                        }
                    });


                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(UploadActivity.this, e.getLocalizedMessage().toString(), Toast.LENGTH_LONG).show();
                }
            });

        }


    }

    public void selectImage(View view) {
//??zinlerle alakal??
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
        } else {
            Intent intentToGallery = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intentToGallery, 2);
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {


        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Intent intentToGallery = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(intentToGallery, 2);
            }
        }


        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
//Burda Bitmap e donu??tururken sorun olabilece??inden apk kontrolu yaparak se??ilen image i bitmape donu??turduk.
        if (requestCode == 2 && resultCode == RESULT_OK && data != null) {

            imageData = data.getData();

            try {

                if (Build.VERSION.SDK_INT >= 28) {
                    ImageDecoder.Source source = ImageDecoder.createSource(this.getContentResolver(), imageData);
                    selectedImage = ImageDecoder.decodeBitmap(source);
                    imageView.setImageBitmap(selectedImage);
                } else {
                    selectedImage = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageData);
                    imageView.setImageBitmap(selectedImage);
                }


            } catch (IOException e) {
                e.printStackTrace();
            }


        }


        super.onActivityResult(requestCode, resultCode, data);
    }
}
