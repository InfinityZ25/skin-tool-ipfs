package us.jcedeno.skin;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.UUID;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import org.mineskin.MineskinClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.ipfs.api.IPFS;
import io.ipfs.api.NamedStreamable;
import io.ipfs.multihash.Multihash;
import lombok.Getter;
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
	private static @Getter IPFS ipfs;

	private static @Getter String skinToolPythonEndpoint;
	private static @Getter Gson gson = new GsonBuilder().setPrettyPrinting().create();
	private static HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();
	private static @Getter UploaderTask uploaderThread;
	private static @Getter RedisController cacheController;

	public static void main(String[] args) {
		// Get variables from environment
		final var mineskinClientKey = getEnvOrEmpty("MINESKIN_KEY");
		final var mineskinAgent = getEnvOrEmpty("MINESKIN_USR_AGENT");
		final var ipfsAddress = getEnvOrEmpty("IPFS_HOST");
		final var skinToolPythonUri = getEnvOrEmpty("SKIN_TOOL_PYTHON_URI");
		final var redisURI = getEnvOrEmpty("REDIS_URI");

		// Print out all variables
		System.out.println("MINESKIN_KEY: " + mineskinClientKey);
		System.out.println("MINESKIN_USR_AGENT: " + mineskinAgent);
		System.out.println("IPFS_HOST: " + ipfsAddress);
		System.out.println("SKIN_TOOL_PYTHON_URI: " + skinToolPythonUri);
		System.out.println("REDIS_URI: " + redisURI);

		// Set the SkinTool Python endpoint
		skinToolPythonEndpoint = skinToolPythonUri.isEmpty() ? "http://localhost:8069" : skinToolPythonUri;

		// Intialize IFPS client
		// ipfs = new IPFS(new MultiAddress(ipfsAddress.isEmpty() ?
		// "/ip4/127.0.0.1/tcp/5001" : ipfsAddress));

		// Intialize mineskinClient
		mineskinClient = mineskinClientKey.isEmpty() ? new MineskinClient(mineskinAgent, mineskinClientKey)
				: new MineskinClient(mineskinAgent.isEmpty() ? "SkinToolApi" : mineskinAgent);

		// Run Spring Boot
		SpringApplication.run(SkinToolApplication.class, args);

		// Initialize cache controller
		if (redisURI != null && !redisURI.isEmpty())
			cacheController = new RedisController(redisURI);

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
	 * Just returns hello world if api is online.
	 * 
	 * @return Hello world.
	 */
	@GetMapping("/hello")
	public String helloWorld() {
		return "Hello World (" + System.currentTimeMillis() + ")!";
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

	/**
	 * 
	 * From here on, you'll see functions protoypes of how to use ipfs.
	 * 
	 */

	@GetMapping("/get")
	public String getMultihash(@RequestParam(value = "hash") String multihash) {

		System.out.println("Getting: " + multihash);

		var hash = Multihash.fromHex(multihash);

		try {
			var retrieved = new String(ipfs.cat(hash));
			System.out.println("Retrieved file: " + retrieved);
			return retrieved;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return "null";
	}

	@PostMapping("/upload")
	public String setMultiHash(@RequestBody String inputJson) {
		var file = new File("ipfs-data/input-" + UUID.randomUUID().toString().split("-")[0]);
		if (!file.exists()) {
			file.getParentFile().mkdirs();
			try {
				file.createNewFile();

				var writer = new java.io.FileWriter(file);
				writer.write(inputJson.toString());
				writer.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		try {
			var merkelNodes = ipfs.add(new NamedStreamable.FileWrapper(file));
			// Get index 0 of the merkelNodes since its of a single file.
			var hash = merkelNodes.get(0).hash;
			file.delete();
			// Return as hex.
			return "{\n hash: \"" + hash.toHex() + "\"\n}";
		} catch (IOException e) {
			e.printStackTrace();
		}
		return "null";
	}

}
