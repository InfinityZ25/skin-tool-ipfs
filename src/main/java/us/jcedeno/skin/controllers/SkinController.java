package us.jcedeno.skin.controllers;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import lombok.Getter;
import us.jcedeno.skin.SkinToolApplication;
import us.jcedeno.skin.entities.Skin;

/**
 * The rest skin controller. Allows interaction with users skin collections and
 * creation.
 * 
 * @author jcedeno
 */
@RestController
public class SkinController {
    private static volatile @Getter ConcurrentHashMap<UUID, List<Skin>> skinCollectionMap = new ConcurrentHashMap<>();

    @GetMapping("/skin/get/{id}")
    public Optional<List<Skin>> getSkins(@PathVariable("id") UUID id) {

        var skinList = skinCollectionMap.get(id);
        if (skinList == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(skinList);
    }

    @DeleteMapping("/skin/delete/{id}")
    public Optional<List<Skin>> deleteSkins(@PathVariable("id") UUID id) {

        var skinList = skinCollectionMap.get(id);
        if (skinList == null) {
            return Optional.empty();
        }
        skinCollectionMap.remove(id);
        // Delete on backend
        SkinToolApplication.getCacheController().getRedisConnection().async().hdel("skins", id.toString());
        return Optional.ofNullable(skinList);
    }

    @GetMapping("/skin/get-all/{variant}")
    public Optional<List<Skin>> getAllVariants(@PathVariable("variant") String variant) {

        var list = new ArrayList<Skin>();

        for (var entry : skinCollectionMap.entrySet())
            for (var skin : entry.getValue())
                if (skin.getName().equalsIgnoreCase(variant))
                    list.add(skin);

        return list.isEmpty() ? Optional.empty() : Optional.ofNullable(list);

    }

    @PutMapping("/skin/create/{id}")
    public List<Skin> generateSkins(@PathVariable("id") UUID id) {
        var storedSkins = skinCollectionMap.get(id);

        if (storedSkins != null) {
            return storedSkins;
        }

        // Get the skins from python
        var skinsForPlayer = SkinToolApplication.generateSkins(id.toString());

        // Parse the skins into SkinCollection Format.
        if (skinsForPlayer.get("data") != null) {
            var skins = skinsForPlayer.getAsJsonObject("data").entrySet().stream().map(
                    m -> Skin.create(m.getValue().getAsString(), m.getKey(), skinsForPlayer.get("slim").getAsBoolean()))
                    .toList();

            // Add to the map
            skinCollectionMap.put(id, skins);

            return skins;
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
        // Return to symbolize success.
        return true;
    }

}
