package com.example.photoviewer;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final int MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 1;
    ImageView imgView;
    TextView textView;
    String site_url = "http://10.0.2.2:8000";
    static JSONObject post_json;
    static String imageUrl = null;
    Bitmap bmImg = null;
    CloadImage taskDownload;

    //PutPost taskUpload;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //imgView= (ImageView) findViewById(R.id.imgView);
        textView = (TextView) findViewById(R.id.textView);
    }

    public void onClickDownload(View v) {

        if (taskDownload != null && taskDownload.getStatus() == AsyncTask.Status.RUNNING) {
            taskDownload.cancel(true);
        }
        taskDownload = new CloadImage();
        taskDownload.execute(site_url + "/api_root/Post/");
        Toast.makeText(getApplicationContext(), "Download", Toast.LENGTH_LONG).show();
        new Thread(() -> {
            try {
                // 🔹 PC의 IP 주소 (에뮬레이터에서는 10.0.2.2 사용)
                Socket socket = new Socket("10.0.2.2", 9999);

                // 서버로 데이터 전송
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                out.println("/api_root/Post/");

                // 서버로부터 응답 받기
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = in.readLine()) != null) {
                    response.append(line);
                }

                socket.close();

                // 결과를 UI에 표시
                runOnUiThread(() -> {
                    textView.setText(response.toString());
                });

            } catch (IOException e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    textView.setText("소켓 서버 연결 실패");
                });
            }
        }).start();
    }

    public void onClickUpload(View v) {
        String title = "테스트 제목";
        String text = "이건 안드로이드에서 보낸 게시글 내용입니다.";
        String imageUrl = ""; // ⚠️ ImageField 때문에 일단 비워둠
        int authorId = 1;     // ⚠️ 실제 Django의 유저 ID로 변경 (예: admin 계정의 id)

        // 🔹 CloadImage 업로드 모드 실행
        CloadImage taskUpload = new CloadImage(title, text, imageUrl);
        taskUpload.execute(site_url + "/api_root/Post/");

        Toast.makeText(getApplicationContext(), "Upload", Toast.LENGTH_LONG).show();
    }

    private class CloadImage extends AsyncTask<String, Integer, List<Bitmap>> {
        private boolean isUpload = false;
        private String uploadTitle;
        private String uploadContent;
        private String uploadImageUrl;
        private boolean uploadSuccess = false;

        // 업로드용 생성자
        public CloadImage(String title, String content, String imageUrl) {
            this.isUpload = true;
            this.uploadTitle = title;
            this.uploadContent = content;
            this.uploadImageUrl = imageUrl;
        }

        // 기존 다운로드용 기본 생성자
        public CloadImage() {
            this.isUpload = false;
        }


        @Override
        protected List<Bitmap> doInBackground(String... urls) {
            if (isUpload) {
                // --- 업로드 기능 ---
                try {
                    URL url = new URL(urls[0]); // 서버 API
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setRequestProperty("Authorization", "Token " + "bf46b8f9337d1d27b4ef2511514c798be1a954b8");
                    conn.setDoOutput(true);

                    JSONObject postData = new JSONObject();
                    postData.put("author", site_url + "/api_root/User/1/");
                    postData.put("title", uploadTitle);
                    postData.put("text", uploadContent);
                    postData.put("image", JSONObject.NULL);

                    OutputStream os = conn.getOutputStream();
                    os.write(postData.toString().getBytes("UTF-8"));
                    os.flush();
                    os.close();


                    int responseCode = conn.getResponseCode();
                    uploadSuccess = (responseCode == HttpURLConnection.HTTP_CREATED || responseCode == HttpURLConnection.HTTP_OK);

                    // 업로드니까 이미지 리스트는 비워서 반환
                    return responseCode == HttpURLConnection.HTTP_CREATED || responseCode == HttpURLConnection.HTTP_OK ? new ArrayList<>() : null;

                } catch (Exception e) {
                    e.printStackTrace();
                    uploadSuccess = false;
                    return null;
                }
            } else {
                // --- 기존 다운로드 기능 ---
                List<Bitmap> bitmapList = new ArrayList<>();
                try {
                    String apiUrl = urls[0];
                    String token = "bf46b8f9337d1d27b4ef2511514c798be1a954b8";
                    URL urlAPI = new URL(apiUrl);
                    HttpURLConnection conn = (HttpURLConnection) urlAPI.openConnection();
                    conn.setRequestProperty("Authorization", "Token " + token);
                    conn.setRequestMethod("GET");
                    conn.setConnectTimeout(3000);
                    conn.setReadTimeout(3000);
                    int responseCode = conn.getResponseCode();
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        InputStream is = conn.getInputStream();
                        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                        StringBuilder result = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            result.append(line);
                        }
                        is.close();
                        JSONArray aryJson = new JSONArray(result.toString());
                        for (int i = 0; i < aryJson.length(); i++) {
                            JSONObject post_json = (JSONObject) aryJson.get(i);
                            String imageUrl = post_json.getString("image");
                            if (!imageUrl.equals("")) {
                                URL myImageUrl = new URL(imageUrl);
                                conn = (HttpURLConnection) myImageUrl.openConnection();
                                InputStream imgStream = conn.getInputStream();
                                Bitmap imageBitmap = BitmapFactory.decodeStream(imgStream);
                                bitmapList.add(imageBitmap);
                                imgStream.close();
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return bitmapList;
            }
        }


        @Override
        protected void onPostExecute(List<Bitmap> images) {
            if (isUpload) {
                if (uploadSuccess) {
                    Toast.makeText(getApplicationContext(), "게시글 업로드 성공!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getApplicationContext(), "게시글 업로드 실패!", Toast.LENGTH_LONG).show();
                }
            } else {
                // 다운로드 처리
                if (images == null || images.isEmpty()) {
                    textView.setText("불러올 이미지가 없습니다.");
                } else {
                    textView.setText("이미지 로드 성공!");
                    RecyclerView recyclerView = findViewById(R.id.recyclerView);
                    ImageAdapter adapter = new ImageAdapter(images);
                    recyclerView.setLayoutManager(new LinearLayoutManager(MainActivity.this));
                    recyclerView.setAdapter(adapter);
                }
            }
        }
        //...생략...
        /*private class PutPost extends AsyncTask<String, Void, Void> {//...여기에코드추가...}
         */
    }
}
