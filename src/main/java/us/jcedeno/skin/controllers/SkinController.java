package us.jcedeno.skin.controllers;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.Getter;
import us.jcedeno.skin.SkinToolApplication;
import us.jcedeno.skin.entities.Skin;
import us.jcedeno.skin.entities.SkinCollection;

/**
 * The rest skin controller. Allows interaction with users skin collections and
 * creation.
 * 
 * @author jcedeno
 */
@RestController
public class SkinController {
    private static volatile @Getter ConcurrentHashMap<SkinCollection, UUID> skinCollectionMap = new ConcurrentHashMap<>();

    /**
     * Retrieves the skins that belong to the given id.
     * 
     * @param id A unique id that represents the owner of a collection.
     * @return A collection of skins if present.
     */
    @GetMapping("/skin/get/{id}")
    public Optional<List<SkinCollection>> getSkins(@PathVariable("id") String id,
            @RequestParam(value = "otherwiseCreate", defaultValue = "false") Boolean createIfNotPresent) {
        // Process the rest in a lambda

        var list = skinCollectionMap.entrySet().stream()
                .filter(entry -> isEquals(entry.getValue(), UUID.fromString(id))).map(mapper -> mapper.getKey())
                .sorted().toList();

        if (list.isEmpty()) {
            if (createIfNotPresent) {
                var collection = SkinCollection.random();
                skinCollectionMap.put(collection, UUID.fromString(id));
                // Return optional

                return Optional.of(List.of(collection));
            }
            return Optional.empty();
        }

        // Return if present.
        return Optional.of(list);
    }

    @GetMapping("/skin/get-all/{variant}")
    public Optional<List<Skin>> getAllVariants(@PathVariable("variant") String variant) {

        var list = new ArrayList<Skin>();

        for (var entry : skinCollectionMap.entrySet())
            for (var skin : entry.getKey().getSkins())
                if (skin.getName().equalsIgnoreCase(variant))
                    list.add(skin);

        return Optional.ofNullable(list);

    }

    @PutMapping("/skin/create/{id}")
    public SkinCollection generateSkins(@PathVariable("id") UUID id) {

        var opt = skinCollectionMap.entrySet().stream().filter(entry -> isEquals(entry.getValue(), id)).findFirst();
        if (opt.isPresent()) {
            return opt.get().getKey();
        }

        // Get the skins from python
        var skinsForPlayer = SkinToolApplication.generateSkins(id.toString());

        // Parse the skins into SkinCollection Format.
        if (skinsForPlayer.get("data") != null) {
            var skins = skinsForPlayer.getAsJsonObject("data").entrySet().stream().map(
                    m -> Skin.create(m.getValue().getAsString(), m.getKey(), skinsForPlayer.get("slim").getAsBoolean()))
                    .toList();

            var collection = SkinCollection.of(skins);

            // Add to the map
            skinCollectionMap.put(collection, id);

            return collection;
        } else {
            return null;
        }

    }

    @PostMapping("/skin/add")
    public boolean addSkin(@RequestBody List<UUID> requestJson) {

        if (requestJson.isEmpty()) {
            return false;
        }
        // Generate skins for all the provided ids
        requestJson.parallelStream().forEach(id -> {
            try {
                generateSkins(id);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        // Return to symbolize failure`
        return true;
    }

    /**
     * Helper function to quickly compare to uuids.
     * 
     * @param uuid1 First id.`
     * @param uuid2 Second id.
     * 
     * @return true if ids are equal.
     */
    private static Boolean isEquals(UUID uuid1, UUID uuid2) {
        return uuid1.compareTo(uuid2) == 0;
    }

}
