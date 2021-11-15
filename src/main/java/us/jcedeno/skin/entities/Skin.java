package us.jcedeno.skin.entities;

import lombok.Getter;
import lombok.Setter;

/**
 * An object containing a player's skin variant and the name of the variant.
 * 
 * @jcedeno
 */
public class Skin {
    /** The name to refer to this skin-variant as. */
    private final @Getter String name;
    /**
     * The Mojang Skin Signature, If null at any point, assume the skin hasn't been
     * uploaded yet.
     */
    private volatile @Getter @Setter String signature;
    /** The base64-encoded skin file. This is the actual png skin image */
    private volatile @Getter @Setter String value;
    /** Weather the skin is slim or not */
    private volatile @Getter @Setter boolean slim;

    public Skin(String value, String name, Boolean slim) {
        this.value = value;
        this.name = name;
        this.slim = slim;
    }

    /** Static constructor */
    public static Skin create(String skinBase64, String skinName, Boolean slim) {
        return new Skin(skinBase64, skinName, slim);
    }

}
