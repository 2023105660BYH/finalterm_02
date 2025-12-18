package com.example.photoviewer;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    ImageView imgView;
    TextView textView;
    String site_url = "http://10.0.2.2:8000";
    Bitmap bmImg = null;
    CloadImage taskDownload;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textView = findViewById(R.id.textView);

        // 앱 시작 시 토큰 확인
        if (getAccessToken() == null) {
            new LoginTask(true).execute(); // 로그인 후 자동 다운로드
        } else {
            new CloadImage().execute(site_url + "/api_root/Post/");
        }
    }

    private void saveAccessToken(String token) {
        getSharedPreferences("auth", MODE_PRIVATE)
                .edit()
                .putString("accessToken", token)
                .apply();
    }

    private String getAccessToken() {
        return getSharedPreferences("auth", MODE_PRIVATE)
                .getString("accessToken", null);
    }

    public void onClickDownload(View v) {
        if (taskDownload != null && taskDownload.getStatus() == AsyncTask.Status.RUNNING) {
            taskDownload.cancel(true);
        }
        taskDownload = new CloadImage();
        taskDownload.execute(site_url + "/api_root/Post/");
        Toast.makeText(getApplicationContext(), "Download 시작", Toast.LENGTH_SHORT).show();
    }

    public void onClickUpload(View v) {
        String title = "테스트 제목";
        String text = "안드로이드에서 보낸 게시글 내용";
        String imageUrl = ""; // ImageField 비워둠

        new CloadImage(title, text, imageUrl).execute(site_url + "/api_root/Post/");
        Toast.makeText(getApplicationContext(), "Upload 시작", Toast.LENGTH_SHORT).show();
    }

    // --- 로그인 태스크 ---
    private class LoginTask extends AsyncTask<Void, Void, String> {
        boolean autoDownload;

        public LoginTask(boolean autoDownload) {
            this.autoDownload = autoDownload;
        }

        @Override
        protected String doInBackground(Void... voids) {
            try {
                URL url = new URL(site_url + "/api/token/");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                JSONObject body = new JSONObject();
                body.put("username", "byh");
                body.put("password", "1234");

                OutputStream os = conn.getOutputStream();
                os.write(body.toString().getBytes("UTF-8"));
                os.close();

                if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder result = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        result.append(line);
                    }

                    JSONObject json = new JSONObject(result.toString());
                    return json.getString("access"); // JWT access token
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String token) {
            if (token != null) {
                saveAccessToken(token);
                Toast.makeText(getApplicationContext(), "로그인 성공", Toast.LENGTH_SHORT).show();

                // 로그인 후 자동 다운로드
                if (autoDownload) {
                    new CloadImage().execute(site_url + "/api_root/Post/");
                }
            } else {
                Toast.makeText(getApplicationContext(), "로그인 실패", Toast.LENGTH_LONG).show();
            }
        }
    }

    // --- 다운로드 / 업로드 공용 AsyncTask ---
    private class CloadImage extends AsyncTask<String, Void, List<Bitmap>> {

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

        // 다운로드용 기본 생성자
        public CloadImage() {
            this.isUpload = false;
        }

        @Override
        protected List<Bitmap> doInBackground(String... urls) {
            if (isUpload) {
                return uploadPost(urls[0]);
            } else {
                return downloadPosts(urls[0]);
            }
        }

        private List<Bitmap> uploadPost(String urlStr) {
            try {
                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");

                String token = getAccessToken();
                conn.setRequestProperty("Authorization", "Bearer " + token);

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

            } catch (Exception e) {
                e.printStackTrace();
                uploadSuccess = false;
            }
            return new ArrayList<>();
        }

        private List<Bitmap> downloadPosts(String urlStr) {
            List<Bitmap> bitmapList = new ArrayList<>();
            try {
                URL urlAPI = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) urlAPI.openConnection();
                String token = getAccessToken();
                conn.setRequestProperty("Authorization", "Bearer " + token);
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                int responseCode = conn.getResponseCode();

                if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
                    // 토큰이 만료되거나 없으면 로그인 후 재시도
                    new LoginTask(true).execute();
                    return bitmapList;
                }

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder result = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        result.append(line);
                    }

                    JSONArray aryJson = new JSONArray(result.toString());
                    for (int i = 0; i < aryJson.length(); i++) {
                        JSONObject post_json = aryJson.getJSONObject(i);
                        String imageUrl = post_json.getString("image");
                        if (!imageUrl.equals("")) {
                            URL myImageUrl = new URL(imageUrl);
                            HttpURLConnection imgConn = (HttpURLConnection) myImageUrl.openConnection();
                            InputStream imgStream = imgConn.getInputStream();
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

        @Override
        protected void onPostExecute(List<Bitmap> images) {
            if (isUpload) {
                if (uploadSuccess) {
                    Toast.makeText(getApplicationContext(), "게시글 업로드 성공!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getApplicationContext(), "게시글 업로드 실패!", Toast.LENGTH_LONG).show();
                }
            } else {
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
    }
}
