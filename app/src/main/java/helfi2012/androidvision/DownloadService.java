package helfi2012.androidvision;

import android.app.IntentService;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;

public class DownloadService extends IntentService {

    public static final String KEY_LINK = "LINK";
    public static final String KEY_FILE_NAME = "FILE_NAME";

    public DownloadService() {
        super("DownloadService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        String link = intent.getStringExtra(KEY_LINK);
        String fileName = intent.getStringExtra(KEY_FILE_NAME);
        //download(link, fileName);

        final int id = 1;
        final NotificationManager mNotifyManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        final NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this);
        mBuilder.setContentTitle(getString(R.string.on_network_load_title))
                .setContentText(getString(R.string.on_network_load_summary))
                .setSmallIcon(android.R.drawable.progress_horizontal);
        new Thread(
                new Runnable() {
                    @Override
                    public void run() {
                        for (int i = 0; i <= 100; i+=5) {
                            mBuilder.setProgress(100, i, false);
                            mNotifyManager.notify(id, mBuilder.build());
                            try {
                                // Sleep for 5 seconds
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {
                               e.printStackTrace();
                            }
                        }
                        mBuilder.setContentText(getString(R.string.on_network_load_complete))
                                .setProgress(0,0,false);
                        mNotifyManager.notify(id, mBuilder.build());
                    }
                }
        ).start();
    }

    private boolean isRedirected( Map<String, List<String>> header ) {
        for( String hv : header.get( null )) {
            if(hv.contains(" 301 ") || hv.contains(" 302 ")) return true;
        }
        return false;
    }

    private void download(String link, String fileName) {
        try {
            URL url = new URL(link);
            HttpURLConnection http = (HttpURLConnection) url.openConnection();
            Map<String, List<String>> header = http.getHeaderFields();
            while (isRedirected(header)) {
                link = header.get("Location").get(0);
                url = new URL(link);
                http = (HttpURLConnection) url.openConnection();
                header = http.getHeaderFields();
            }
            InputStream input = http.getInputStream();
            byte[] buffer = new byte[4096];
            int n;
            OutputStream output = new FileOutputStream(new File(fileName));
            while ((n = input.read(buffer)) != -1) {
                output.write(buffer, 0, n);
            }
            output.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
