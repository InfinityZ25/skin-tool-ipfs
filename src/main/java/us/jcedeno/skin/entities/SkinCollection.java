package us.jcedeno.skin.entities;

import java.util.List;
import java.util.UUID;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * An object that represent a list of skins. This is inteneded to be used to
 * later reference this skin collections and manage them with ease. For now,
 * they're kinda pointless.
 * 
 * @author jcedeno
 */
@RequiredArgsConstructor(staticName = "of")
public class SkinCollection {
    private @Getter @NonNull List<Skin> skins;

    /**
     * Overrides the current contents of the skin collection. It's just a setter.
     * 
     * @param skins The new contents of the skin collection.
     */
    public void overrideSkins(List<Skin> skins) {
        this.skins = skins;
    }

    /**
     * Static constructor to generate a random skin collection for testing purposes.
     * 
     * @return The newly generated collection.
     */
    public static SkinCollection random() {
        return of(List.of(Skin.create("skinBase64", UUID.randomUUID().toString().split("-")[0], false)));
    }

}
