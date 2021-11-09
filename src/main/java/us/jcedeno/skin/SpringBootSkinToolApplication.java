package us.jcedeno.skin;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.UUID;

import org.mineskin.MineskinClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import io.ipfs.api.IPFS;
import io.ipfs.api.NamedStreamable;
import io.ipfs.multiaddr.MultiAddress;
import io.ipfs.multihash.Multihash;
import lombok.Getter;

@RestController
@SpringBootApplication
public class SpringBootSkinToolApplication {
	private static @Getter MineskinClient mineskinClient;
	private static @Getter IPFS ipfs;

	private static @Getter String skinToolPythonEndpoint;

	private static HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();

	public static void main(String[] args) {
		// Get variables from environment
		final var mineskinClientKey = getEnvOrEmpty("MINESKIN_KEY");
		final var mineskinAgent = getEnvOrEmpty("MINESKIN_USR_AGENT");
		final var ipfsAddress = getEnvOrEmpty("IPFS_HOST");
		final var skinToolPythonUri = getEnvOrEmpty("SKIN_TOOL_PYTHON_URI");

		// Set the SkinTool Python endpoint
		skinToolPythonEndpoint = skinToolPythonUri.isEmpty() ? "http://localhost:8069" : skinToolPythonUri;

		// Intialize IFPS client
		ipfs = new IPFS(new MultiAddress(ipfsAddress.isEmpty() ? "/ip4/127.0.0.1/tcp/5001" : ipfsAddress));

		// Intialize mineskinClient
		mineskinClient = mineskinClientKey.isEmpty() ? new MineskinClient(mineskinAgent, mineskinClientKey)
				: new MineskinClient(mineskinAgent.isEmpty() ? "SkinToolApi" : mineskinAgent);

		// Run Spring Boot
		SpringApplication.run(SpringBootSkinToolApplication.class, args);

	}

	@GetMapping("/helloworld")
	public String helloWorld() {
		return "Hello World (" + System.currentTimeMillis() + ")!";
	}

	@ResponseBody
	@RequestMapping(value = "/generate/{id}", method = RequestMethod.GET)
	public String generateSkins(@PathVariable("id") String id) {

		// Make a get request

		var request = HttpRequest.newBuilder(URI.create(skinToolPythonEndpoint + "/" + id))
				.header("accept", "application/json").build();

		try {
			// TODO Don't just return, use the data to upload skins to mojang.
			return client.send(request, BodyHandlers.ofString()).body();
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}

		return "";
	}

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

	/**
	 * 
	 * Function that returns the string of an environment variable or blank.
	 * 
	 * @param key The key of the environment variable.
	 * @return The value of the environment variable or blank.
	 */
	private static String getEnvOrEmpty(String key) {
		return System.getenv("MINESKIN_CLIENT_URL") != null ? System.getenv("MINESKIN_CLIENT_URL") : "";
	}

}
