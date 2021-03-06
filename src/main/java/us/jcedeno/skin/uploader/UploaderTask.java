package us.jcedeno.skin.uploader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.gson.Gson;

import org.mineskin.SkinOptions;
import org.mineskin.Variant;
import org.mineskin.Visibility;
import org.mineskin.data.Skin;

import us.jcedeno.skin.SkinToolApplication;
import us.jcedeno.skin.controllers.SkinController;

/**
 * A thread that uploads skins to the mojang servers using the mineskin api.
 * Since the mineskin api is extremely unstable, this task will guarantee that
 * all skins get, at some point, uploaded.
 * 
 * @author jcedeno
 */
public class UploaderTask extends Thread {
    private static AtomicBoolean anyChanges = new AtomicBoolean(false);
    private static Gson gson = new Gson();


    @Override
    public void run() {
        while (!Thread.interrupted()) {
            processSkins();

            // Send the thread to sleep every 500ms.
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }
    }

    /**
     * Helper function that processes and uploads skins to the mojang servers.
     * 
     */
    private static void processSkins() {
        SkinController.getSkinCollectionMap().entrySet().parallelStream().forEach(e -> {
            e.getValue().stream().filter(ep -> ep.getSignature() == null).forEach(skins -> {
                try {
                    var attempt = attemptUpload(skins.getValue(), skins.isSlim());
                    // Loop until the skin is uploaded
                    while (attempt == null) {
                        // Try Again
                        attempt = attemptUpload(skins.getValue(), skins.isSlim());
                    }
                    if (attempt != null) {
                        // Update the skin with the new signature
                        skins.setSignature(attempt.data.texture.signature);
                        skins.setValue(attempt.data.texture.value);

                        // Notify of changes later.
                        if (!anyChanges.get())
                            anyChanges.set(true);

                        // Log success
                        System.out.println("Successfully uploaded skin: " + skins.getName() + " for " + e.getKey());
                    }
                } catch (IOException ex) {
                    ex.printStackTrace();
                }

            });
        });
        if (anyChanges.get()) {
            System.out.println("There are changes on the skins. Writing to database.");

            var map = new HashMap<String, String>();
            var entries = SkinController.getSkinCollectionMap().entrySet();
            entries.forEach(a -> {
                map.put(a.getKey().toString(), gson.toJson(a.getValue()));
            });

            SkinToolApplication.getCacheController().getRedisConnection().async().hmset("skins", map);

            anyChanges.set(false);

        }
    }

    /**
     * A method that attempts uploading a skin to the mineskin api. This function is
     * epxected to fail.
     * 
     * @param skinBase64 the base64 encoded skin
     * @return the uploaded skin, if successful.
     * @throws IOException When the skin can't be written as a file in the local
     *                     disk for permissions reasons.
     */
    static Skin attemptUpload(String skinBase64, boolean bool) throws IOException {
        // Translate the skinBase64 to a file
        var skin = Base64.getDecoder().decode(skinBase64);
        var skinFile = new File("skin_" + UUID.randomUUID().toString().split("-")[0] + ".png");
        var skinFileOutputStream = new FileOutputStream(skinFile);
        skinFileOutputStream.write(skin);

        skinFileOutputStream.close();
        Skin skinObject = null;

        try {
            skinObject = SkinToolApplication.getMineskinClient().generateUpload(skinFile,
                    SkinOptions.create("", bool ? Variant.SLIM : Variant.CLASSIC, Visibility.PUBLIC)).get();
        } catch (Exception e) {
            e.printStackTrace();
        }

        skinFile.delete();

        return skinObject;

    }
    

}
