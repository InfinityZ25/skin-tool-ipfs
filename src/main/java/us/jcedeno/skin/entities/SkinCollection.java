package us.jcedeno.skin.entities;

import java.util.List;
import java.util.UUID;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(staticName = "of")
public class SkinCollection {
    private @NonNull List<Skin> skins;

    public List<Skin> getSkins() {
        return skins;
    }

    public void overrideSkins(List<Skin> skins) {
        this.skins = skins;
    }

    public static SkinCollection random() {
        return of(List.of(Skin.create("skinBase64", UUID.randomUUID().toString().split("-")[0])));
    }

}
