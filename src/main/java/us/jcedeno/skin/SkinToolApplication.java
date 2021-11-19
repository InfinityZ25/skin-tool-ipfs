package us.jcedeno.skin;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import org.mineskin.MineskinClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.RestController;

import lombok.Getter;
import us.jcedeno.skin.controllers.SkinController;
import us.jcedeno.skin.entities.Skin;
import us.jcedeno.skin.redis.RedisController;
import us.jcedeno.skin.uploader.UploaderTask;

/**
 * SkinToolApplication is a spring tool rest api that interacts with
 * skin-tool-python to generate custom skins on demand. This app also guarantees
 * that the skins get uploaded to the mojang servers.
 * 
 * @author jcedeno
 */
@RestController
@SpringBootApplication
public class SkinToolApplication {
	private static @Getter MineskinClient mineskinClient;

	private static @Getter String skinToolPythonEndpoint;
	private static @Getter Gson gson = new GsonBuilder().setPrettyPrinting().create();
	private static HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();
	private static @Getter UploaderTask uploaderThread;
	private static @Getter RedisController cacheController;

	public static void main(String[] args) {
		// Get variables from environment
		final var mineskinClientKey = getEnvOrEmpty("MINESKIN_KEY");
		final var mineskinAgent = getEnvOrEmpty("MINESKIN_USR_AGENT");
		final var skinToolPythonUri = getEnvOrEmpty("SKIN_TOOL_PYTHON_URI");
		final var redisURI = getEnvOrEmpty("REDIS_URI");

		// Print out all variables
		System.out.println("MINESKIN_KEY: " + mineskinClientKey);
		System.out.println("MINESKIN_USR_AGENT: " + mineskinAgent);
		System.out.println("SKIN_TOOL_PYTHON_URI: " + skinToolPythonUri);
		System.out.println("REDIS_URI: " + redisURI);

		// Set the SkinTool Python endpoint
		skinToolPythonEndpoint = skinToolPythonUri.isEmpty() ? "http://localhost:8069" : skinToolPythonUri;

		// Intialize mineskinClient
		mineskinClient = mineskinClientKey.isEmpty() ? new MineskinClient(mineskinAgent, mineskinClientKey)
				: new MineskinClient(mineskinAgent.isEmpty() ? "SkinToolApi" : mineskinAgent);

		// Run Spring Boot
		SpringApplication.run(SkinToolApplication.class, args);

		// Initialize cache controller
		if (redisURI != null && !redisURI.isEmpty()) {

			cacheController = new RedisController(redisURI);
			cacheController.getRedisConnection().sync().hgetall("skins").entrySet().forEach(all -> {
				var id = UUID.fromString(all.getKey());
				var skins = List.of(gson.fromJson(all.getValue(), Skin[].class));
				SkinController.getSkinCollectionMap().put(id, skins);
			});
		}
		cacheController = new RedisController(redisURI);

		// Create and start Uploader Task Thread
		uploaderThread = new UploaderTask();
		uploaderThread.start();

	}

	/**
	 * Util function to generate the player skin variants by contacting
	 * skin-tool-python.
	 * 
	 * @param id The UUID of the player you wish to generate the skins for.
	 * @return A JSON object containing the player skin variants.
	 */
	public static JsonObject generateSkins(String id) {

		// Make a get request

		var request = HttpRequest.newBuilder(URI.create(skinToolPythonEndpoint + "/" + id))
				.header("accept", "application/json").build();

		try {
			var response = client.send(request, BodyHandlers.ofString()).body();
			return gson.fromJson(response, JsonObject.class);
		} catch (IOException | InterruptedException e) {
			// e.printStackTrace();
		}

		return new JsonObject();
	}

	/**
	 * 
	 * Function that returns the string of an environment variable or blank.
	 * 
	 * @param key The key of the environment variable.
	 * @return The value of the environment variable or blank.
	 */
	private static String getEnvOrEmpty(String key) {
		return System.getenv(key) != null ? System.getenv(key) : "";
	}

}
