package com.pulka2014.fnaf.android;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.core.content.FileProvider;

import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
import com.pulka2014.fnaf.FnafGame;
import com.pulka2014.fnaf.VideoLauncher;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class AndroidLauncher extends AndroidApplication implements VideoLauncher {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AndroidApplicationConfiguration config = new AndroidApplicationConfiguration();
        config.useImmersiveMode = true;
        initialize(new FnafGame(this), config);
    }

    @Override
    public void playBomb() {
        runOnUiThread(() -> {
            try {
                // Копируем bomb.mp4 из assets во временный файл
                InputStream in = getAssets().open("bomb.mp4");
                File outFile = new File(getCacheDir(), "bomb.mp4");
                OutputStream out = new FileOutputStream(outFile);
                byte[] buf = new byte[8192];
                int len;
                while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
                out.close();
                in.close();

                // Открываем системный видеоплеер
                Uri uri = FileProvider.getUriForFile(this,
                        getPackageName() + ".fileprovider", outFile);
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(uri, "video/mp4");
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(intent);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
